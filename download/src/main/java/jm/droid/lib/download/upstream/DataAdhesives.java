package jm.droid.lib.download.upstream;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.InterruptedIOException;

import jm.droid.lib.download.C;
import jm.droid.lib.download.offline.Download;
import jm.droid.lib.download.util.Log;

/**
 * 负责dataSource与dataWrite的粘合剂，负责数据源的open read及close的流程管理，
 * 同时也负责数据目的的open write及close的流程管理
 */
public class DataAdhesives {

    public static final String TAG = "DataAdhesives";

    /**
     * Receives progress updates during cache operations.
     */
    public interface ProgressListener {

        /**
         * Called when progress is made during a cache operation.
         *
         * @param requestLength  The length of the content being cached in bytes, or {@link
         *                       C#LENGTH_UNSET} if unknown.
         * @param bytesCached    The number of bytes that are cached.
         * @param newBytesCached The number of bytes that have been newly cached since the last progress
         *                       update.
         */
        void onProgress(long requestLength, long bytesCached, long newBytesCached);
    }

    /**
     * Default buffer size to be used while caching.
     */
    public static final int DEFAULT_BUFFER_SIZE_BYTES = 128 * 1024;

    private final DataSource dataSource;
    private final DataDestination dataDestination;
    private final DataSpec dataSpec;
    private final byte[] temporaryBuffer;
    @Nullable
    private final ProgressListener progressListener;

    private long nextPosition;
    private long endPosition;
    private long bytesCached;
    private long contentLength;

    private volatile boolean isCanceled;

    public DataAdhesives(ProgressListener listener, Download download, DataSource dataSource, DataDestination dataDestination) {
        temporaryBuffer = new byte[DEFAULT_BUFFER_SIZE_BYTES];
        this.progressListener = listener;
        this.dataSource = dataSource;
        this.dataDestination = dataDestination;
        dataSpec = new DataSpec.Builder()
            .setUri(download.request.uri)
            .setPath(download.request.path)
            .setPosition(download.getBytesDownloaded())
            .setLength(download.contentLength)
            .build();
        bytesCached = download.getBytesDownloaded();
        contentLength = download.contentLength;
    }

    /**
     *
     */
    @WorkerThread
    public void process() throws IOException {
        throwIfCanceled();
        Log.i(TAG, "start to save file to local storage pos: "+dataSpec.position+" len:"+dataSpec.length);
        //这里不相等说明是重试后进入的，缓存的数据增加了导致bytesCache增加了，才会与新建任务的时候传入的pos不一样
        if (bytesCached != dataSpec.position) {
            DataSpec aa = dataSpec.buildUpon().setPosition(bytesCached).build();
            dataDestination.open(aa);
        } else {
            dataDestination.open(dataSpec);
        }
        if (progressListener != null) {
            progressListener.onProgress(getLength(), bytesCached, /* newBytesCached= */ 0);
        }

        //endPosition构造方法中已经赋值了，这里不用动，目前只支持一个任务单线程下载，不支持并发
        Log.i(TAG, "cache bytes len:" + bytesCached + ", content len:" + contentLength);
        long nextRequestLength = contentLength == C.LENGTH_UNSET ? C.LENGTH_UNSET : contentLength - bytesCached;
        readBlockToCache(bytesCached, nextRequestLength);
        dataDestination.close();
        Log.i(TAG, "save to local storage end");
    }

    public void cancel() {
        isCanceled = true;
    }

    public void onRemove() {
        Log.i(TAG,"remove data destination");
    }

    public void onSuccess() {
        dataDestination.done();
    }
    /**
     * Reads the specified block of data, writing it into the cache.
     *
     * @param position The starting position of the block.
     * @param length   The length of the block, or {@link C#LENGTH_UNSET} if unbounded.
     * @return The number of bytes read.
     * @throws IOException If an error occurs reading the data or writing it to the cache.
     */
    private long readBlockToCache(long position, long length) throws IOException {
        boolean isLastBlock = position + length == endPosition || length == C.LENGTH_UNSET;

        Log.i(TAG, "readBlockToCache:" + position + ", len:" + length);
        long resolvedLength = C.LENGTH_UNSET;
        boolean isDataSourceOpen = false;
        if (length != C.LENGTH_UNSET) {
            // If the length is specified, try to open the data source with a bounded request to avoid
            // the underlying network stack requesting more data than required.
            DataSpec boundedDataSpec =
                dataSpec.buildUpon().setPosition(position).setLength(length).build();
            try {
                resolvedLength = dataSource.open(boundedDataSpec);
                contentLength = position + resolvedLength;
                isDataSourceOpen = true;
            } catch (IOException e) {
                DataSourceUtil.closeQuietly(dataSource);
                DataSourceUtil.closeQuietly(dataDestination);
            }
        }

        Log.i(TAG, "isDataSourceOpen:" + isDataSourceOpen);
        if (!isDataSourceOpen) {
            // Either the length was unspecified, or we allow short content and our attempt to open the
            // DataSource with the specified length failed.
            throwIfCanceled();
            DataSpec unboundedDataSpec =
                dataSpec.buildUpon().setPosition(position).setLength(C.LENGTH_UNSET).build();
            try {
                resolvedLength = dataSource.open(unboundedDataSpec);
                contentLength = position + resolvedLength;
            } catch (IOException e) {
                DataSourceUtil.closeQuietly(dataSource);
                DataSourceUtil.closeQuietly(dataDestination);
                throw e;
            }
        }
        int totalBytesRead = 0;
        try {
            if (isLastBlock && resolvedLength != C.LENGTH_UNSET) {
                onRequestEndPosition(position + resolvedLength);
            }
            int bytesRead = 0;
            while (bytesRead != C.RESULT_END_OF_INPUT) {
                throwIfCanceled();
                bytesRead = dataSource.read(temporaryBuffer, /* offset= */ 0, temporaryBuffer.length);
                if (bytesRead != C.RESULT_END_OF_INPUT) {
                    onNewBytesCached(bytesRead);
                    totalBytesRead += bytesRead;
                    dataDestination.write(temporaryBuffer, 0, bytesRead);
                }
            }
            if (isLastBlock) {
                onRequestEndPosition(position + totalBytesRead);
            }
        } catch (IOException e) {
            DataSourceUtil.closeQuietly(dataSource);
            DataSourceUtil.closeQuietly(dataDestination);
            throw e;
        }

        // Util.closeQuietly(dataSource) is not used here because it's important that an exception is
        // thrown if DataSource.close fails. This is because there's no way of knowing whether the block
        // was successfully cached in this case.
        dataSource.close();
        return totalBytesRead;
    }

    private void onRequestEndPosition(long endPosition) {
        if (this.endPosition == endPosition) {
            return;
        }
        this.endPosition = endPosition;
        if (progressListener != null) {
            progressListener.onProgress(getLength(), bytesCached, /* newBytesCached= */ 0);
        }
    }

    private void onNewBytesCached(long newBytesCached) {
        bytesCached += newBytesCached;
        if (progressListener != null) {
            progressListener.onProgress(getLength(), bytesCached, newBytesCached);
        }
    }

    private long getLength() {
        return contentLength == C.LENGTH_UNSET ? C.LENGTH_UNSET : contentLength;
    }

    private void throwIfCanceled() throws InterruptedIOException {
        if (isCanceled) {
            throw new InterruptedIOException();
        }
    }
}
