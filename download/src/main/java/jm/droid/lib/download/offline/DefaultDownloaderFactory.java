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
package jm.droid.lib.download.offline;

import jm.droid.lib.download.upstream.DataDestination;
import jm.droid.lib.download.upstream.DataSource;
import jm.droid.lib.download.upstream.DefaultHttpDataSource;
import jm.droid.lib.download.upstream.FileDataDestination;
import jm.droid.lib.download.util.Assertions;
import jm.droid.lib.download.util.Log;

import java.util.concurrent.Executor;

/**
 * Default {@link DownloaderFactory}, supporting creation of progressive, DASH, HLS and
 * SmoothStreaming downloaders. Note that for the latter three, the corresponding library module
 * must be built into the application.
 */
public class DefaultDownloaderFactory implements DownloaderFactory {

    private final Executor executor;
    private final DataSource.Factory dataSourceFactory;
    private final DataDestination.Factory dataDestionationFactory;

    /**
     * Creates an instance.
     *
     * @param executor An {@link Executor} used to download data. Passing {@code Runnable::run} will
     *                 cause each download task to download data on its own thread. Passing an {@link Executor}
     *                 that uses multiple threads will speed up download tasks that can be split into smaller
     *                 parts for parallel execution.
     */
    public DefaultDownloaderFactory(Executor executor) {
        this(new DefaultHttpDataSource.Factory(), executor, new FileDataDestination.Factory());
    }

    public DefaultDownloaderFactory(DataSource.Factory dataSourceFactory, Executor executor, DataDestination.Factory dataDestinationFactory) {
        this.executor = Assertions.checkNotNull(executor);
        this.dataSourceFactory = Assertions.checkNotNull(dataSourceFactory);
        this.dataDestionationFactory = dataDestinationFactory;
    }

    /**
     * 这里返回Downloader下载器
     * 可以通过{@link DownloadRequest}的请求生成不同的downloader
     * 当然我们也可以在{@link DownloadManager}中使用不同的DownloadFactory
     * 目前只用{@link ProgressiveDownloader}
     * 作为实现参考
     *
     * @param download The download request.
     * @return
     */
    @Override
    public Downloader createDownloader(Download download) {
        return new ProgressiveDownloader(dataSourceFactory, dataDestionationFactory, executor, download);
    }


}
