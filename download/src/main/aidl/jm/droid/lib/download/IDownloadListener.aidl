// IDownloadProgressListener.aidl
package jm.droid.lib.download;

// Declare any non-default types here with import statements
import jm.droid.lib.download.offline.DownloadRequest;

interface IDownloadListener {
    void onProgress(in DownloadRequest request, float percent);
}
