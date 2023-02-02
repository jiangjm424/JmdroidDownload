package com.grank.db.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import jm.droid.lib.download.client.DownloadClient
import jm.droid.lib.download.client.DownloadListenerImpl
import jm.droid.lib.download.offline.Download
import jm.droid.lib.download.offline.DownloadRequest
import jm.droid.lib.download.util.Log

class SecondRemodel(application: Application) : AndroidViewModel(application) {
    private val download by lazy { DownloadClient(application) }
    private val TAG = "jiang2"
    private val _downloads = mutableListOf<Download>()
    val downloads = MutableLiveData<List<Download>>()
    val progress = MutableLiveData<Pair<String,Float>>()
    val update = MutableLiveData<Download>()
    private val lll = object : DownloadListenerImpl {
        override fun onProgress(request: DownloadRequest, percent: Float) {
            Log.i(TAG,"onProgress: id:${request.id} percent:$percent")
            progress.postValue(Pair(request.id, percent))
        }

        override fun onDownloadChanged(download: Download) {
            Log.i(TAG,"changed:${download.request.id} state:${download.state}")
            update.postValue(download)
        }

        override fun onDownloadRemoved(download: Download) {
            Log.i(TAG, "remove: ${download.request.id}")
        }

    }

    fun aaa() {
        downloads.value = download.downloadInfos
        download.registerDownloadListener(lll)
    }
    init {
        download.connect()
        val a = download.downloadInfos
        _downloads.addAll(a)
        Log.i(TAG,"init gg:${a.size}")
//        download.registerDownloadListener(lll)
    }

    override fun onCleared() {
        super.onCleared()
        download.unRegisterDownloadListener(lll)
    }
}
