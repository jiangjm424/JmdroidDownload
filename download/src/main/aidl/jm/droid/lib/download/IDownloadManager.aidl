// IDownloaderManager.aidl
package jm.droid.lib.download;

// Declare any non-default types here with import statements
import jm.droid.lib.download.IDownloadListener;
import jm.droid.lib.download.offline.Download;

interface IDownloadManager {
    oneway void addDownloadListener(IDownloadListener listener);
    oneway void removeDownloadListener(IDownloadListener listener);
    List<Download> getDownloads();
}
