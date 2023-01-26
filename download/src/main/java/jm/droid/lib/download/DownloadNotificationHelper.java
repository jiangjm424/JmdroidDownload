/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jm.droid.lib.download;

import static androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.DoNotInline;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import java.util.List;

import jm.droid.lib.download.client.INotificationHelper;
import jm.droid.lib.download.offline.Download;
import jm.droid.lib.download.scheduler.Requirements;
import jm.droid.lib.download.util.NotificationUtil;
import jm.droid.lib.download.util.Util;

/**
 * Helper for creating download notifications.
 */
public final class DownloadNotificationHelper implements INotificationHelper {

    private static final @StringRes
    int NULL_STRING_ID = 0;

    private final NotificationCompat.Builder notificationBuilder;

    public static final String CHANNEL_ID = "id_download_channel";

    /**
     * @param context   A context.
     */
    public DownloadNotificationHelper(Context context) {
        this.notificationBuilder =
            new NotificationCompat.Builder(context.getApplicationContext(), CHANNEL_ID);
    }

    @Override
    public void buildNotificationChannel(Context context) {
        NotificationUtil.createNotificationChannel(context, CHANNEL_ID, R.string.exo_download_notification_channel_name, R.string.exo_download_description, NotificationUtil.IMPORTANCE_LOW);
    }

    /**
     * Returns a progress notification for the given downloads.
     *
     * @param context            A context.
     * @param message            An optional message to display on the notification.
     * @param downloads          The downloads.
     * @param notMetRequirements Any requirements for downloads that are not currently met.
     * @return The notification.
     */
    @Override
    public Notification buildProgressNotification(
        Context context,
        @Nullable String message,
        List<Download> downloads,
        @Requirements.RequirementFlags int notMetRequirements) {
        float totalPercentage = 0;
        int downloadTaskCount = 0;
        boolean allDownloadPercentagesUnknown = true;
        boolean haveDownloadedBytes = false;
        boolean haveDownloadingTasks = false;
        boolean haveQueuedTasks = false;
        boolean haveRemovingTasks = false;
        for (int i = 0; i < downloads.size(); i++) {
            Download download = downloads.get(i);
            switch (download.state) {
                case Download.STATE_REMOVING:
                    haveRemovingTasks = true;
                    break;
                case Download.STATE_QUEUED:
                    haveQueuedTasks = true;
                    break;
                case Download.STATE_RESTARTING:
                case Download.STATE_DOWNLOADING:
                    haveDownloadingTasks = true;
                    float downloadPercentage = download.getPercentDownloaded();
                    if (downloadPercentage != C.PERCENTAGE_UNSET) {
                        allDownloadPercentagesUnknown = false;
                        totalPercentage += downloadPercentage;
                    }
                    haveDownloadedBytes |= download.getBytesDownloaded() > 0;
                    downloadTaskCount++;
                    break;
                // Terminal states aren't expected, but if we encounter them we do nothing.
                case Download.STATE_STOPPED:
                case Download.STATE_COMPLETED:
                case Download.STATE_FAILED:
                default:
                    break;
            }
        }

        int titleStringId;
        boolean showProgress = true;
        if (haveDownloadingTasks) {
            titleStringId = R.string.exo_download_downloading;
        } else if (haveQueuedTasks && notMetRequirements != 0) {
            showProgress = false;
            if ((notMetRequirements & Requirements.NETWORK_UNMETERED) != 0) {
                // Note: This assumes that "unmetered" == "WiFi", since it provides a clearer message that's
                // correct in the majority of cases.
                titleStringId = R.string.exo_download_paused_for_wifi;
            } else if ((notMetRequirements & Requirements.NETWORK) != 0) {
                titleStringId = R.string.exo_download_paused_for_network;
            } else {
                titleStringId = R.string.exo_download_paused;
            }
        } else if (haveRemovingTasks) {
            titleStringId = R.string.exo_download_removing;
        } else {
            // There are either no downloads, or all downloads are in terminal states.
            titleStringId = NULL_STRING_ID;
        }

        int maxProgress = 0;
        int currentProgress = 0;
        boolean indeterminateProgress = false;
        if (showProgress) {
            maxProgress = 100;
            if (haveDownloadingTasks) {
                currentProgress = (int) (totalPercentage / downloadTaskCount);
                indeterminateProgress = allDownloadPercentagesUnknown && haveDownloadedBytes;
            } else {
                indeterminateProgress = true;
            }
        }

        return buildNotification(
            context,
            R.drawable.ic_download,
            null,
            message,
            titleStringId,
            maxProgress,
            currentProgress,
            indeterminateProgress,
            /* ongoing= */ true,
            /* showWhen= */ false);
    }

    /**
     * Returns a notification for a completed download.
     *
     * @param context       A context.
     * @param smallIcon     A small icon for the notifications.
     * @param contentIntent An optional content intent to send when the notification is clicked.
     * @param message       An optional message to display on the notification.
     * @return The notification.
     */
    @Override
    public Notification buildDownloadCompletedNotification(
        Context context,
        @Nullable String message) {
        int titleStringId = R.string.exo_download_completed;
        return buildEndStateNotification(context, R.drawable.ic_download_done, null, message, titleStringId);
    }

    /**
     * Returns a notification for a failed download.
     *
     * @param context       A context.
     * @param smallIcon     A small icon for the notifications.
     * @param contentIntent An optional content intent to send when the notification is clicked.
     * @param message       An optional message to display on the notification.
     * @return The notification.
     */
    @Override
    public Notification buildDownloadFailedNotification(
        Context context,
        @Nullable String message) {
        @StringRes int titleStringId = R.string.exo_download_failed;
        return buildEndStateNotification(context, R.drawable.ic_download_done, null, message, titleStringId);
    }

    private Notification buildEndStateNotification(
        Context context,
        @DrawableRes int smallIcon,
        @Nullable PendingIntent contentIntent,
        @Nullable String message,
        @StringRes int titleStringId) {
        return buildNotification(
            context,
            smallIcon,
            contentIntent,
            message,
            titleStringId,
            /* maxProgress= */ 0,
            /* currentProgress= */ 0,
            /* indeterminateProgress= */ false,
            /* ongoing= */ false,
            /* showWhen= */ true);
    }

    private Notification buildNotification(
        Context context,
        @DrawableRes int smallIcon,
        @Nullable PendingIntent contentIntent,
        @Nullable String message,
        @StringRes int titleStringId,
        int maxProgress,
        int currentProgress,
        boolean indeterminateProgress,
        boolean ongoing,
        boolean showWhen) {
        notificationBuilder.setSmallIcon(smallIcon);
        notificationBuilder.setContentTitle(
            titleStringId == NULL_STRING_ID ? null : context.getResources().getString(titleStringId));
        notificationBuilder.setContentIntent(contentIntent);
        notificationBuilder.setStyle(
            message == null ? null : new NotificationCompat.BigTextStyle().bigText(message));
        notificationBuilder.setProgress(maxProgress, currentProgress, indeterminateProgress);
        notificationBuilder.setOngoing(ongoing);
        notificationBuilder.setShowWhen(showWhen);
        if (Util.SDK_INT >= 31) {
            Api31.setForegroundServiceBehavior(notificationBuilder);
        }
        return notificationBuilder.build();
    }

    @RequiresApi(31)
    private static final class Api31 {
        @SuppressLint("WrongConstant") // TODO(b/254277605): remove lint suppression
        @DoNotInline
        public static void setForegroundServiceBehavior(
            NotificationCompat.Builder notificationBuilder) {
            notificationBuilder.setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE);
        }
    }
}
