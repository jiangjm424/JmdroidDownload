// IDownloaderManager.aidl
package jm.droid.lib.download;

// Declare any non-default types here with import statements
import jm.droid.lib.download.IDownloadListener;
import jm.droid.lib.download.offline.Download;

interface IDownloadManager {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    oneway void addDownloadListener(IDownloadListener listener);
    oneway void removeDownloadListener(IDownloadListener listener);
    List<Download> getDownloadInfos();
}
