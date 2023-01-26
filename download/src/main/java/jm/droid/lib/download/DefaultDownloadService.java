/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Notification;
import android.content.Context;

import androidx.annotation.Nullable;

import jm.droid.lib.download.client.DownloadClient;
import jm.droid.lib.download.client.INotificationHelper;
import jm.droid.lib.download.database.DownloadDatabaseProvider;
import jm.droid.lib.download.offline.Download;
import jm.droid.lib.download.offline.DownloadManager;
import jm.droid.lib.download.offline.DownloadService;
import jm.droid.lib.download.scheduler.PlatformScheduler;
import jm.droid.lib.download.scheduler.Requirements;
import jm.droid.lib.download.scheduler.Scheduler;
import jm.droid.lib.download.util.NotificationUtil;
import jm.droid.lib.download.util.Util;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * A service for downloading media.
 */
public class DefaultDownloadService extends DownloadService {

    private static final int JOB_ID = 1;
    private static final int FOREGROUND_NOTIFICATION_ID = 1;
    private static final String DOWNLOAD_NOTIFICATION_CHANNEL_ID = "download_channel";

    private INotificationHelper downloadNotificationHelper;

    public DefaultDownloadService() {
        super(
            FOREGROUND_NOTIFICATION_ID,
            DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
            DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            R.string.exo_download_notification_channel_name,
            /* channelDescriptionResourceId= */ 0);
    }

    public synchronized INotificationHelper getDownloadNotificationHelper(Context context) {
        if (downloadNotificationHelper == null) {
            downloadNotificationHelper = DownloadClient.downloadConfigFactory.createNotificationHelper(context);
            downloadNotificationHelper.buildNotificationChannel(context);
        }
        return downloadNotificationHelper;
    }
    @Override
    protected DownloadManager getDownloadManager() {
        // This will only happen once, because getDownloadManager is guaranteed to be called only once
        // in the life cycle of the process.
        DownloadManager downloadManager = new DownloadManager(this,
            new DownloadDatabaseProvider(this),
            Executors.newFixedThreadPool(/* nThreads= */ 6));
        INotificationHelper downloadNotificationHelper =
            getDownloadNotificationHelper(/* context= */ this);
        downloadManager.addListener(
            new TerminalStateNotificationHelper(
                this, downloadNotificationHelper, FOREGROUND_NOTIFICATION_ID + 1));
        return downloadManager;
    }

    @Override
    protected Scheduler getScheduler() {
        return Util.SDK_INT >= 21 ? new PlatformScheduler(this, JOB_ID) : null;
    }

    @Override
    protected Notification getForegroundNotification(
        List<Download> downloads, @Requirements.RequirementFlags int notMetRequirements) {
        return getDownloadNotificationHelper(/* context= */ this)
            .buildProgressNotification(
                /* context= */ this,
                /* message= */ null,
                downloads,
                notMetRequirements);
    }

    /**
     * Creates and displays notifications for downloads when they complete or fail.
     *
     * <p>This helper will outlive the lifespan of a single instance of {@link DefaultDownloadService}.
     * It is static to avoid leaking the first {@link DefaultDownloadService} instance.
     */
    private static final class TerminalStateNotificationHelper implements DownloadManager.Listener {

        private final Context context;
        private final INotificationHelper notificationHelper;

        private int nextNotificationId;

        public TerminalStateNotificationHelper(
            Context context, INotificationHelper notificationHelper, int firstNotificationId) {
            this.context = context.getApplicationContext();
            this.notificationHelper = notificationHelper;
            nextNotificationId = firstNotificationId;
        }

        @Override
        public void onDownloadChanged(
            DownloadManager downloadManager, Download download, @Nullable Exception finalException) {
            Notification notification;
            if (download.state == Download.STATE_COMPLETED) {
                notification =
                    notificationHelper.buildDownloadCompletedNotification(
                        context,
                        Util.fromUtf8Bytes(download.request.data));
            } else if (download.state == Download.STATE_FAILED) {
                notification =
                    notificationHelper.buildDownloadFailedNotification(
                        context,
                        Util.fromUtf8Bytes(download.request.data));
            } else {
                return;
            }
            NotificationUtil.setNotification(context, nextNotificationId++, notification);
        }
    }
}
