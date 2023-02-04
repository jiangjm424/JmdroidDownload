package jm.droid.lib.download.client;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.browse.MediaBrowser;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import jm.droid.lib.download.DefaultDownloadConfigFactory;
import jm.droid.lib.download.offline.Download;
import jm.droid.lib.download.offline.DownloadRequest;
import jm.droid.lib.download.util.Log;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jm.droid.lib.download.IDownloadListener;
import jm.droid.lib.download.IDownloadManager;
import jm.droid.lib.download.DefaultDownloadService;

/**
 * 作为服务端提供的一个业务方使用的对象，文件业务接入
 */
@MainThread
public final class DownloadClient implements ServiceConnection {
    private static final String TAG = "DownloadClient";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({
        STATE_DISCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED
    })
    private @interface State {
    }

    private @State int mState = STATE_DISCONNECTED;

    private Context mContext;
    private IDownloadManager proxy;

    private DownloadListenerDefaultBinder downloadBinder = null;
    private CopyOnWriteArrayList<DownloadListenerImpl> downloadListeners = new CopyOnWriteArrayList<>();

    private final @Nullable ConnectionCallback mCallback;

    public DownloadClient(@NotNull Context context) {
        this(context, null);
    }
    public DownloadClient(@NotNull Context context, @Nullable ConnectionCallback callback) {
        mCallback = callback;
        if (context.getApplicationContext() != null)
            mContext = context.getApplicationContext();
        else
            mContext = context;
    }

    //当未与服务端绑定时去绑定服务会有回调，如果已经绑定了，那么再次调用此方法时并不会有回调
    public void connect() {
        if (mState != STATE_DISCONNECTED) {
            Log.w(TAG, "the service is connecting or connected");
            return;
        }
        mState = STATE_CONNECTING;
        Log.i(TAG, "connect");
        Intent ii = new Intent(mContext, DefaultDownloadService.class);
        mContext.bindService(ii, this, Context.BIND_AUTO_CREATE);
    }

    //当调用此方法时，如果是未绑定状态，则系统会抛出异常，
    //如果已经绑定了调用这个方法，并不会回调disconnect，所以此时直接释放资源并重置状态
    public void disConnect() throws RemoteException {
        Log.i(TAG, "dis connect");
        if (mState != STATE_DISCONNECTED) {
            proxy = null;
            mState = STATE_DISCONNECTED;
            mContext.unbindService(this);
            if(mCallback != null) mCallback.onConnectionSuspended();
        }
        if (!downloadListeners.isEmpty()) {
            downloadListeners.clear();
            if (!checkNoProxy()) {
                proxy.removeDownloadListener(downloadBinder);
            }
        }
    }

    public void registerDownloadListener(@NotNull DownloadListenerImpl ll) throws RemoteException {
        downloadListeners.add(ll);
        if (checkNoProxy()) return;
        if (downloadBinder == null) {
            DownloadListenerDefaultBinder binder = new DownloadListenerDefaultBinder();
            downloadBinder = binder;
            proxy.addDownloadListener(binder);
        }
    }

    public void unRegisterDownloadListener(@NotNull DownloadListenerImpl ll) throws RemoteException {
        boolean remove = downloadListeners.remove(ll);
        if (remove && downloadListeners.isEmpty() && !checkNoProxy() && downloadBinder != null) {
            final DownloadListenerDefaultBinder binder = downloadBinder;
            downloadBinder = null;
            proxy.removeDownloadListener(binder);
        }
    }

    public List<Download> getDownloads() throws RemoteException {
        if (checkNoProxy()) return new ArrayList<>();
        List<Download> list = proxy.getDownloads();
        Log.i(TAG, "getDownload size : " + list.size());
        return list;
    }

    private boolean checkNoProxy() {
        return proxy == null;
    }

    private void onConnected() {
        if (mCallback != null) mCallback.onConnected();
        if (!downloadListeners.isEmpty() && downloadBinder == null) {
            DownloadListenerDefaultBinder binder = new DownloadListenerDefaultBinder();
            downloadBinder = binder;
            try {
                proxy.addDownloadListener(binder);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * ServiceConnection callback
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        IDownloadManager p = IDownloadManager.Stub.asInterface(service);
        proxy = p;
        mState = STATE_CONNECTED;
        Log.i(TAG, "onConnected");
        onConnected();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mState = STATE_DISCONNECTED;
        proxy = null;
        Log.i(TAG, "on dis connected");
    }

    @Override
    public void onBindingDied(ComponentName name) {
        ServiceConnection.super.onBindingDied(name);
    }

    @Override
    public void onNullBinding(ComponentName name) {
        ServiceConnection.super.onNullBinding(name);
    }

    /**
     * IDownloadListener.Stub
     */
    private class DownloadListenerDefaultBinder extends IDownloadListener.Stub {
        @Override
        public void onProgress(DownloadRequest request, float percent) throws RemoteException {
            for (int i = 0; i < downloadListeners.size(); i++) {
                downloadListeners.get(i).onProgress(request, percent);
            }
        }

        @Override
        public void onDownloadChanged(Download download) throws RemoteException {
            for (int i = 0; i < downloadListeners.size(); i++) {
                downloadListeners.get(i).onDownloadChanged(download);
            }
        }

        @Override
        public void onDownloadRemoved(Download download) throws RemoteException {
            for (int i = 0; i < downloadListeners.size(); i++) {
                downloadListeners.get(i).onDownloadRemoved(download);
            }
        }
    }

    /**
     * Callbacks for connection related events.
     */
    public interface ConnectionCallback {
        /**
         * Invoked after {@link MediaBrowser#connect()} when the request has successfully completed.
         */
        default void onConnected() {
        }

        /**
         * Invoked when the client is disconnected from the download service.
         */
        default void onConnectionSuspended() {
        }
    }

    public static @NotNull DownloadConfigFactory downloadConfigFactory = new DefaultDownloadConfigFactory();
}
