package jm.droid.lib.download.client;

import android.app.Notification;
import android.content.Context;

import androidx.annotation.Nullable;

import java.util.List;

import jm.droid.lib.download.offline.Download;
import jm.droid.lib.download.offline.DownloadManager;
import jm.droid.lib.download.scheduler.Requirements;

public interface INotificationHelper extends DownloadManager.Listener {

    void buildNotificationChannel(Context context);

    Notification buildProgressNotification(Context context, @Nullable String message, List<Download> downloads, @Requirements.RequirementFlags int notMetRequirements);

    Notification buildDownloadCompletedNotification(Context context, @Nullable String message);

    Notification buildDownloadFailedNotification(Context context, @Nullable String message);
}
