// IDownloadProgressListener.aidl
package jm.droid.lib.download;

// Declare any non-default types here with import statements
import jm.droid.lib.download.offline.DownloadRequest;
import jm.droid.lib.download.offline.Download;

interface IDownloadListener {
    oneway void onProgress(in DownloadRequest request, float percent, float downloadSpeed);
    oneway void onDownloadChanged(in Download download);
    oneway void onDownloadRemoved(in Download download);
}
