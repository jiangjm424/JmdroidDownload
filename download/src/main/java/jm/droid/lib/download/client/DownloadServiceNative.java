package jm.droid.lib.download.client;

import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import jm.droid.lib.download.IDownloadListener;
import jm.droid.lib.download.IDownloadManager;
import jm.droid.lib.download.offline.Download;
import jm.droid.lib.download.offline.DownloadCursor;
import jm.droid.lib.download.offline.DownloadManager;
import jm.droid.lib.download.offline.DownloadRequest;
import jm.droid.lib.download.scheduler.Requirements;
import jm.droid.lib.download.util.Log;
import jm.droid.lib.download.util.Util;

public class DownloadServiceNative extends IDownloadManager.Stub implements DownloadManager.Listener {
    private final static String TAG = "DownloadServiceNative";

    private final CopyOnWriteArrayList<IDownloadListener> listeners = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String,IDownloadListener> specialListeners = new ConcurrentHashMap<>();
    private final DownloadManager downloadManager;

    public DownloadServiceNative(DownloadManager dm) {
        downloadManager = dm;
        dm.addListener(this);
    }


    public void release(){
        downloadManager.removeListener(this);
    }
    //IDownloadManager.Stub
    @Override
    public void addDownloadListener(IDownloadListener listener) throws RemoteException {
        listeners.add(listener);
    }

    @Override
    public void removeDownloadListener(IDownloadListener listener) throws RemoteException {
        listeners.remove(listener);
    }

    @Override
    public List<Download> getDownloads() throws RemoteException {
        DownloadCursor cursor = null;
        List<Download> downloads = new ArrayList<>();
        try {
            cursor = downloadManager.getDownloadIndex().getDownloads();
            while (cursor.moveToNext()) {
                downloads.add(cursor.getDownload());
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to load index.", e);
        } finally {
            Util.closeQuietly(cursor);
        }
        return downloads;
    }

    //DownloadManager.Listener
    @Override
    public void onDownloadChanged(DownloadManager downloadManager, Download download, @Nullable Exception finalException) {
        Log.i(TAG,"onDownloadChanged:"+download.state+" id:"+download.request.id);
        DownloadManager.Listener.super.onDownloadChanged(downloadManager, download, finalException);
        try {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onDownloadChanged(download);
            }
        } catch (RemoteException ignored) {}
    }

    @Override
    public void onDownloadRemoved(DownloadManager downloadManager, Download download) {
        Log.i(TAG,"onDownloadRemoved:"+download.state+" id:"+download.request.id);
        DownloadManager.Listener.super.onDownloadRemoved(downloadManager, download);
        try {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onDownloadRemoved(download);
            }
        } catch (RemoteException ignored) {}
    }

    @Override
    public void onDownloadsPausedChanged(DownloadManager downloadManager, boolean downloadsPaused) {
        Log.i(TAG,"onDownloadsPausedChanged");
        DownloadManager.Listener.super.onDownloadsPausedChanged(downloadManager, downloadsPaused);
    }

    @Override
    public void onIdle(DownloadManager downloadManager) {
        Log.i(TAG,"onIdle");
        DownloadManager.Listener.super.onIdle(downloadManager);
    }

    @Override
    public void onInitialized(DownloadManager downloadManager) {
        Log.i(TAG,"onInitialized");
        DownloadManager.Listener.super.onInitialized(downloadManager);
    }

    @Override
    public void onRequirementsStateChanged(DownloadManager downloadManager, Requirements requirements, int notMetRequirements) {
        Log.i(TAG,"onRequirementsStateChanged");
        DownloadManager.Listener.super.onRequirementsStateChanged(downloadManager, requirements, notMetRequirements);
    }

    @Override
    public void onWaitingForRequirementsChanged(DownloadManager downloadManager, boolean waitingForRequirements) {
        Log.i(TAG,"onWaitingForRequirementsChanged");
    }

    @Override
    public void onDownloadProgress(DownloadRequest request, float percent, float downloadSpeed) {
        try {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).onProgress(request, percent, downloadSpeed);
            }
        } catch (RemoteException ignored) {}

    }
}
