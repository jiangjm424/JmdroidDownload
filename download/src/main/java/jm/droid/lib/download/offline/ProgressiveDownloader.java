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
package jm.droid.lib.download.offline;

import static jm.droid.lib.download.util.Assertions.checkNotNull;

import androidx.annotation.Nullable;

import jm.droid.lib.download.C;
import jm.droid.lib.download.upstream.DataDestination;
import jm.droid.lib.download.upstream.DataSource;
import jm.droid.lib.download.util.Assertions;
import jm.droid.lib.download.util.Log;
import jm.droid.lib.download.util.PriorityTaskManager.PriorityTooLowException;
import jm.droid.lib.download.util.RunnableFutureTask;
import jm.droid.lib.download.util.Util;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import jm.droid.lib.download.upstream.DataAdhesives;

/**
 * A downloader for progressive media streams.
 */
public final class ProgressiveDownloader implements Downloader {

    private final Executor executor;

    @Nullable
    private ProgressListener progressListener;
    private volatile RunnableFutureTask<Void, IOException> downloadRunnable;
    private volatile boolean isCanceled;
    private final DataAdhesives dataAdhesives;


    /**
     * Creates a new instance.
     *
     * @param executor An {@link Executor} used to make requests for the media being downloaded. In
     *                 the future, providing an {@link Executor} that uses multiple threads may speed up the
     *                 download by allowing parts of it to be executed in parallel.
     */
    public ProgressiveDownloader(DataSource.Factory dataSourceFactory,
                                 DataDestination.Factory dataDestinationFactory,
                                 Executor executor,
                                 Download download) {
        this.executor = Assertions.checkNotNull(executor);
        DataAdhesives.ProgressListener progressListener = this::onProgress;
        dataAdhesives = new DataAdhesives(progressListener, download, dataSourceFactory.createDataSource(), dataDestinationFactory.createDataDestination());
    }

    @Override
    public void download(@Nullable ProgressListener progressListener)
        throws IOException, InterruptedException {
        this.progressListener = progressListener;
        try {
            boolean finished = false;
            while (!finished && !isCanceled) {
                // Recreate downloadRunnable on each loop iteration to avoid rethrowing a previous error.
                downloadRunnable =
                    new RunnableFutureTask<Void, IOException>() {
                        @Override
                        protected Void doWork() throws IOException {
                            dataAdhesives.process();
                            return null;
                        }

                        @Override
                        protected void cancelWork() {
                            dataAdhesives.cancel();
                        }
                    };
                executor.execute(downloadRunnable);
                try {
                    downloadRunnable.get();
                    finished = true;
                } catch (ExecutionException e) {
                    Throwable cause = Assertions.checkNotNull(e.getCause());
                    if (cause instanceof PriorityTooLowException) {
                        // The next loop iteration will block until the task is able to proceed.
                    } else if (cause instanceof IOException) {
                        throw (IOException) cause;
                    } else {
                        // The cause must be an uncaught Throwable type.
                        Util.sneakyThrow(cause);
                    }
                }
            }
        } finally {
            // If the main download thread was interrupted as part of cancelation, then it's possible that
            // the runnable is still doing work. We need to wait until it's finished before returning.
            checkNotNull(downloadRunnable).blockUntilFinished();
        }
    }

    @Override
    public void cancel() {
        isCanceled = true;
        RunnableFutureTask<Void, IOException> downloadRunnable = this.downloadRunnable;
        if (downloadRunnable != null) {
            downloadRunnable.cancel(/* interruptIfRunning= */ true);
        }
    }

    @Override
    public void remove() {
        dataAdhesives.onRemove();
    }

    @Override
    public void success() {
        dataAdhesives.onSuccess();
    }

    private void onProgress(long contentLength, long bytesCached, long newBytesCached) {
        if (progressListener == null) {
            return;
        }
        float percentDownloaded =
            contentLength == C.LENGTH_UNSET || contentLength == 0
                ? C.PERCENTAGE_UNSET
                : ((bytesCached * 100f) / contentLength);
        progressListener.onProgress(contentLength, bytesCached, percentDownloaded, newBytesCached);
    }
}
