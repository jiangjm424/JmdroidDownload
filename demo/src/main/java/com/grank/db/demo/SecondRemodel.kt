package com.grank.db.demo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import jm.droid.lib.download.client.DownloadClient
import jm.droid.lib.download.client.DownloadListenerImpl
import jm.droid.lib.download.offline.Download
import jm.droid.lib.download.offline.DownloadRequest
import jm.droid.lib.download.util.Log

class SecondRemodel(application: Application) : AndroidViewModel(application),
    DownloadClient.ConnectionCallback {
    private val TAG = "jiang2"
    private val download by lazy { DownloadClient(application, this) }
    private val _downloads = mutableListOf<Download>()
    private val _connected = MutableLiveData(false)

    //    val downloads = MutableLiveData<List<Download>>()
    val downloads = Transformations.map(_connected) {
        Log.i(TAG,"connected:$it")
        if (it) download.downloads else emptyList()
    }
    val progress = MutableLiveData<Triple<String, Float,Float>>()
    val update = MutableLiveData<Download>()
    val remove = MutableLiveData<Download>()
    private val lll = object : DownloadListenerImpl {
        override fun onProgress(request: DownloadRequest, percent: Float, speed:Float) {
            Log.i(TAG, "onProgress: id:${request.id} percent:$percent")
            progress.value = (Triple(request.id, percent, speed))
        }

        override fun onDownloadChanged(download: Download) {
            Log.i(TAG, "changed:${download.request.id} state:${download.state}")
            update.value = (download)
        }

        override fun onDownloadRemoved(download: Download) {
            Log.i(TAG, "remove: ${download.request.id}")
            remove.value = download
        }

    }

    fun aaa(id:String?) {
//        downloads.value = download.downloads
//        download.registerDownloadListener(lll)
        id?:return
        download.subscribeOn(id, SubscribeDownloadListenerImp(id))
    }

    init {
        download.connect()
        Log.i(TAG,"second vm init")
//        val a = download.downloads
//        _downloads.addAll(a)
//        Log.i(TAG, "init gg:${a.size}")
        download.registerDownloadListener(lll)
    }

    override fun onConnected() {
        super.onConnected()
        _connected.value = true
    }

    override fun onConnectionSuspended() {
        super.onConnectionSuspended()
        _connected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        Log.i(TAG,"second vm onclear")
        download.unRegisterDownloadListener(lll)
        download.unSubscribeOn()
    }
}
