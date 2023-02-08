package com.grank.db.demo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.db.view.extfuns.visible
import com.grank.db.demo.databinding.FragmentSecondBinding
import com.grank.db.demo.databinding.LayoutItemBinding
import jm.droid.lib.download.client.DownloadClient
import jm.droid.lib.download.client.DownloadListenerImpl
import jm.droid.lib.download.offline.Download
import jm.droid.lib.download.offline.DownloadHelper
import jm.droid.lib.download.offline.DownloadRequest
import jm.droid.lib.download.util.Log

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val vvv by viewModels<SecondRemodel>()
    private val adapter by lazy { Ada() }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        binding.buttonFresh.setOnClickListener {
            vvv.aaa(vvv.downloads.value?.getOrNull(0)?.request?.id)
        }
        binding.listDownload.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            this.adapter = this@SecondFragment.adapter
//            itemAnimator = null
            (itemAnimator as? DefaultItemAnimator)?.supportsChangeAnimations = false
        }
        vvv.downloads.observe(viewLifecycleOwner) {
            adapter.setdata(it)
        }
        vvv.update.observe(viewLifecycleOwner) {
            adapter.update(it)
        }
        vvv.remove.observe(viewLifecycleOwner) {
            adapter.remove(it)
        }
        vvv.progress.observe(viewLifecycleOwner) { (id, p, speed) ->
            adapter.progress(id, p, speed)
        }
    }

    private class VV(val itemBinding: LayoutItemBinding) : RecyclerView.ViewHolder(itemBinding.root)
    private class Ada() : RecyclerView.Adapter<VV>() {
        private val lll = mutableListOf<Download>()
        fun setdata(l: List<Download>) {
            lll.clear()
            lll.addAll(l)
            notifyItemRangeChanged(0, l.size)
        }

        private fun getIndex(id: String): Int {
            for (i in 0 until lll.size) {
                if (lll[i].request.id == id) return i
            }
            return -1
        }

        fun progress(id: String, percent: Float, speed:Float) {
            val i = getIndex(id)
            Log.i("jiang3","adapter progress: $id, percent: $percent, found : $i")
            if (i >= 0) {
                val item = lll.get(i)
                item.percentDownloaded = percent
                item.downloadSpeed = speed
                notifyItemChanged(i)
            }
        }

        fun update(download: Download) {
            val i = getIndex(download.request.id)
            Log.i("jiang3","adapter progress: ${download.request.id},  found : $i")
            if (i >= 0) {
                lll[i] = download
                notifyItemChanged(i)
            }
        }

        fun remove(download: Download) {
            val i = getIndex(download.request.id)
            if (i<0)return
            lll.removeAt(i)
            notifyItemRemoved(i)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VV {
            return VV(LayoutItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: VV, position: Int, payloads: MutableList<Any>) {
            super.onBindViewHolder(holder, position, payloads)
        }
        private val aa = 1024*1024
        override fun onBindViewHolder(holder: VV, position: Int) {
            val item = lll.get(position)
            holder.itemBinding.displayName.text = item.request.displayName
            holder.itemBinding.downladId.text = item.request.id
            holder.itemBinding.process.text = item.bytesDownloaded.toString()
            holder.itemBinding.state.text = item.state.toString()
            if (item.state == Download.STATE_DOWNLOADING) {

                val speed = if (item.downloadSpeed > aa) {
                    String.format("%.2fMBps",item.downloadSpeed/aa)
                } else {
                    String.format("%.2fKBps",item.downloadSpeed/1024)
                }
                holder.itemBinding.speed.text = speed
                holder.itemBinding.speed.visible = true
            } else {
                holder.itemBinding.speed.visible = false
            }
            holder.itemBinding.btnPauseStart.text = if (item.state == Download.STATE_DOWNLOADING) "pause" else "start"
            holder.itemBinding.btnPauseStart.setOnClickListener {
                if (item.state == Download.STATE_COMPLETED) return@setOnClickListener
                if (item.state != Download.STATE_DOWNLOADING) {
                    DownloadHelper.Builder().setCmd(DownloadHelper.CMD_RESUME_DOWNLOAD).setDownloadRequest(item.request).build().commit(holder.itemBinding.root.context)
                }else{
                    DownloadHelper.Builder().setCmd(DownloadHelper.CMD_PAUSE_DOWNLOAD).setTaskId(item.request.id).build().commit(holder.itemBinding.root.context)
                }
            }
            holder.itemBinding.btnDelete.setOnClickListener {
                DownloadHelper.Builder().setCmd(DownloadHelper.CMD_REMOVE_DOWNLOAD).setTaskId(item.request.id).setDeleteFileWhenRemove(true).build().commit(holder.itemBinding.root.context)
            }
        }

        override fun getItemCount(): Int {
            return lll.size
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
