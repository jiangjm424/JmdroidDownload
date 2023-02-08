package com.grank.db.demo

import jm.droid.lib.download.client.DownloadListenerImpl
import jm.droid.lib.download.offline.Download
import jm.droid.lib.download.offline.DownloadRequest
import jm.droid.lib.download.util.Log

class SubscribeDownloadListenerImp(val id:String):DownloadListenerImpl {
    companion object {
        private const val TAG = "Subscribe"
    }
    override fun onProgress(request: DownloadRequest, percent: Float, downloadSpeed: Float) {
        Log.i(TAG,"id:$id, req:${request.id}")
    }

    override fun onDownloadChanged(download: Download) {
        Log.i(TAG,"change id:${download.request.id}")
    }

    override fun onDownloadRemoved(download: Download) {
        Log.i(TAG,"remove id:${download.request.id}")
    }
}
