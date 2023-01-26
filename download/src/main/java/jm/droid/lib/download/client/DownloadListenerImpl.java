package jm.droid.lib.download.client;

import android.os.IBinder;

import jm.droid.lib.download.IDownloadListener;

public interface DownloadListenerImpl extends IDownloadListener {
    @Override
    default IBinder asBinder(){return null;}
}
