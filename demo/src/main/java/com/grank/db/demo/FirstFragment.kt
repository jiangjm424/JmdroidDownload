package com.grank.db.demo

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import jm.droid.lib.download.offline.DownloadRequest
import jm.droid.lib.download.offline.DownloadService
import jm.droid.lib.download.util.Log
import com.grank.db.demo.databinding.FragmentFirstBinding
import jm.droid.lib.download.DefaultDownloadService
import jm.droid.lib.download.client.DownloadClient
import jm.droid.lib.download.client.DownloadListenerImpl
import jm.droid.lib.download.offline.Download
import jm.droid.lib.download.offline.DownloadHelper

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }
//    private val LIULISHUO_APK_URL = "https://dl0002.liqucn.com/0e74524526a594fc0d692d9830f9228d/63ccfa7f/upload/2021/310/h/com.huawei.health_12.0.11.300_liqucn.com.apk"
//    private val LIULISHUO_APK_URL = "https://tse4-mm.cn.bing.net/th/id/OIP-C.2EbOk5g-nxqS0gja5pBgfAHaEC?pid=ImgDet&rs=1"
    private val LIULISHUO_APK_URL = "http://110.81.196.233:49155/down.qq.com/xunxian/patch/ManualPatch4.5.5.1-4.5.7.1-SD.exe?mkey=63ce31b6e7ed56832482e0fce9c4779b&arrive_key=26595237213&cip=116.25.41.233&proto=http&access_type="

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireContext().getExternalFilesDir(null)?.absolutePath
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
        binding.startDownload.setOnClickListener {
            DownloadHelper.Builder().setCmd(DownloadHelper.CMD_ADD_DOWNLOAD).setForeground(true).setDownloadRequest(
                DownloadRequest.Builder(Uri.parse(LIULISHUO_APK_URL+"/"+ (iii++))).build()
            ).build().commit(requireContext())
        }
        binding.startIdDownload.setOnClickListener {
            DownloadHelper.Builder().setCmd(DownloadHelper.CMD_ADD_DOWNLOAD).setDownloadRequest(
                DownloadRequest.Builder(Uri.parse(LIULISHUO_APK_URL)).setDisplayName("aaa.exe").setPath(requireContext().getExternalFilesDir(null)?.path+"/bbb.exe").build(),
            ).build().commit(requireContext())
        }
        binding.stopIdDownload.setOnClickListener {
            DownloadHelper.Builder().setCmd(DownloadHelper.CMD_SET_STOP_REASON).setTaskId("6118eef38cee72b9199dbc0e16df942c").build().commit(requireContext())
        }
        binding.stopDownload.setOnClickListener {
            DownloadHelper.Builder().setCmd(DownloadHelper.CMD_REMOVE_DOWNLOAD).setTaskId("6118eef38cee72b9199dbc0e16df942c").build().commit(requireContext())
        }
        binding.pauseAllDownload.setOnClickListener {
            DownloadHelper.Builder().setCmd(DownloadHelper.CMD_PAUSE_DOWNLOADS).build().commit(requireContext())
        }
        binding.removeAllDownload.setOnClickListener {
            DownloadHelper.Builder().setCmd(DownloadHelper.CMD_REMOVE_ALL_DOWNLOADS).build().commit(requireContext())
        }
        binding.resumeAllDownload.setOnClickListener {
            DownloadHelper.Builder().setCmd(DownloadHelper.CMD_RESUME_DOWNLOADS).build().commit(requireContext())
        }

        binding.bindDownloadService.setOnClickListener {
            downloadClient.connect()
        }
        binding.unbindDownloadService.setOnClickListener {
            downloadClient.disConnect()
        }
        binding.basicTypes.setOnClickListener {
            downloadClient.basicTypes()
        }
        binding.addProgress.setOnClickListener {
            downloadClient.registerDownloadListener(ppp)
        }
        binding.delProgress.setOnClickListener {
            downloadClient.unRegisterDownloadListener(ppp)
        }
        binding.checkTaskInfo.setOnClickListener {
            downloadClient.downloadInfos.forEach {
                Log.i("jiang","display name:${it.request.id}")
            }
        }
    }
    private var iii = 0;
    private val downloadClient by lazy {  DownloadClient(requireContext()) }
    private val ppp = object :DownloadListenerImpl{
        override fun onProgress(request: DownloadRequest, percent: Float) {
            Log.i("jiang","id:${request.id}, per:$percent")
        }

        override fun onDownloadChanged(download: Download) {

        }

        override fun onDownloadRemoved(download: Download) {
            TODO("Not yet implemented")
        }

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
