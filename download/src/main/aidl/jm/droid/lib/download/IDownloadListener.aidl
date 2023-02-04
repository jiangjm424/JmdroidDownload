// IDownloadProgressListener.aidl
package jm.droid.lib.download;

// Declare any non-default types here with import statements
import jm.droid.lib.download.offline.DownloadRequest;
import jm.droid.lib.download.offline.Download;

interface IDownloadListener {
    void onProgress(in DownloadRequest request, float percent);
    void onDownloadChanged(in Download download);
    void onDownloadRemoved(in Download download);
}
