package jm.droid.lib.download.client;

import android.content.Context;

/**
 * 下载库的配置，可以配置的内容
 * 1 下载通知
 * 2 暂定
 */
public interface DownloadConfigFactory {
    INotificationHelper createNotificationHelper(Context context);
}
