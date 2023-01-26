package jm.droid.lib.download;

import android.content.Context;

import jm.droid.lib.download.client.DownloadConfigFactory;
import jm.droid.lib.download.client.INotificationHelper;

public final class DefaultDownloadConfigFactory implements DownloadConfigFactory {
    @Override
    public INotificationHelper createNotificationHelper(Context context) {
        return new DownloadNotificationHelper(context);
    }
}
