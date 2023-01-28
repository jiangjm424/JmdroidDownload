package jm.droid.lib.download.offline;

import static java.lang.annotation.ElementType.TYPE_USE;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jm.droid.lib.download.DefaultDownloadService;

public class DownloadHelper {

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @Target(TYPE_USE)
    @IntDef({CMD_ADD_DOWNLOAD, CMD_REMOVE_DOWNLOAD, CMD_REMOVE_ALL_DOWNLOADS, CMD_RESUME_DOWNLOADS, CMD_PAUSE_DOWNLOADS, CMD_SET_STOP_REASON})
    public @interface DownloadCmd {
    }
    public static final int CMD_ADD_DOWNLOAD = 0;
    public static final int CMD_REMOVE_DOWNLOAD = 1;
    public static final int CMD_REMOVE_ALL_DOWNLOADS = 2;
    public static final int CMD_RESUME_DOWNLOADS = 3;
    public static final int CMD_PAUSE_DOWNLOADS = 4;
    public static final int CMD_SET_STOP_REASON = 5;

    private final @DownloadCmd int cmd;
    private final @Nullable DownloadRequest request;
    private final @Nullable String taskId;
    private final boolean foreground;

    private DownloadHelper(@DownloadCmd int cmd, DownloadRequest request, String taskId, boolean foreground) {
        this.cmd = cmd;
        this.request = request;
        this.taskId = taskId;
        this.foreground = foreground;
    }

    public void commit(Context context) {
        switch (cmd) {
            case CMD_ADD_DOWNLOAD:
                DownloadService.sendAddDownload(context, DefaultDownloadService.class, request, foreground);
                break;
            case CMD_SET_STOP_REASON:
                String id = taskId;
                if (TextUtils.isEmpty(id) && request != null) id = request.id;
                DownloadService.sendSetStopReason(context, DefaultDownloadService.class, id, Download.STOP_REASON_UNKNOWN, foreground);
                break;
            case CMD_REMOVE_DOWNLOAD:
                String id2 = taskId;
                if (TextUtils.isEmpty(id2) && request != null) id2 = request.id;
                DownloadService.sendRemoveDownload(context, DefaultDownloadService.class, id2, foreground);
                break;
            case CMD_REMOVE_ALL_DOWNLOADS:
                DownloadService.sendRemoveAllDownloads(context, DefaultDownloadService.class, foreground);
                break;
            case CMD_RESUME_DOWNLOADS:
                DownloadService.sendResumeDownloads(context, DefaultDownloadService.class, foreground);
                break;
            case CMD_PAUSE_DOWNLOADS:
                DownloadService.sendPauseDownloads(context, DefaultDownloadService.class, foreground);
                break;
            default:
                break;
        }
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public static class Builder {
        private @DownloadCmd int cmd;
        private @Nullable DownloadRequest request;
        private @Nullable String taskId;
        private boolean foreground = true;

        public Builder setCmd(@DownloadCmd int cmd) {
            this.cmd = cmd;
            return this;
        }
        public Builder setDownloadRequest(DownloadRequest request) {
            this.request = request;
            return this;
        }
        public Builder setTaskId(String id) {
            this.taskId = id;
            return this;
        }
        public Builder setForeground(boolean foreground) {
            this.foreground = foreground;
            return this;
        }
        public Builder() {
        }

        public Builder(DownloadHelper helper) {
            cmd = helper.cmd;
            request = helper.request;
            taskId = helper.taskId;
            foreground = helper.foreground;
        }

        public DownloadHelper build() {
            return new DownloadHelper(cmd, request, taskId, foreground);
        }
    }
}
