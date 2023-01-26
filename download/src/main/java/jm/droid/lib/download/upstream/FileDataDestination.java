package jm.droid.lib.download.upstream;

import jm.droid.lib.download.C;
import jm.droid.lib.download.util.Log;

import java.io.File;
import java.io.IOException;

public class FileDataDestination implements DataDestination {
    private final static String SUFFIX = ".tmp";
    private FileOutputStream outputStream;
    private final static String TAG = "FileDataDestination";

    private String originalFilePath = null;

    public static class Factory implements DataDestination.Factory {
        @Override
        public DataDestination createDataDestination() {
            return new FileDataDestination();
        }
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        originalFilePath = dataSpec.path;
        String temp = dataSpec.path + SUFFIX;
        File tempFile = new File(temp);
        Log.i(TAG, "byteCached:"+dataSpec.position+" , data spec pos:"+dataSpec.position);
        outputStream = new FileRandomAccessFile(tempFile);
        if (dataSpec.position > 0) {
            outputStream.seek(dataSpec.position);
        }
        return dataSpec.position;
    }

    @Override
    public int write(byte[] buffer, int offset, int length) throws IOException {
        outputStream.write(buffer, offset, length);
        return 0;
    }
    @Override
    public void close() throws IOException {
        outputStream.flushAndSync();
        outputStream.close();
    }

    @Override
    public void done() {
        boolean success = new File(originalFilePath+SUFFIX).renameTo(new File(originalFilePath));
        Log.i(TAG,"rename :"+success);
    }
}
