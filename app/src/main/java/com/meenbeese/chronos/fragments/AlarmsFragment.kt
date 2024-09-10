package com.meenbeese.chronos.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.meenbeese.chronos.R
import com.meenbeese.chronos.adapters.AlarmsAdapter
import com.meenbeese.chronos.interfaces.ContextFragmentInstantiator


class AlarmsFragment : BasePagerFragment() {
    private lateinit var alarmsAdapter: AlarmsAdapter
    private var empty: View? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_recycler, container, false)
        recyclerView = v.findViewById(R.id.recycler)
        empty = v.findViewById(R.id.empty)
        (v.findViewById<View>(R.id.emptyText) as TextView).setText(R.string.msg_alarms_empty)
        recyclerView?.layoutManager = GridLayoutManager(context, 1)
        alarmsAdapter = AlarmsAdapter(chronos!!, recyclerView!!, parentFragmentManager)
        recyclerView?.adapter = alarmsAdapter

        onChanged()

        return v
    }

    override fun getTitle(context: Context?): String? {
        return context?.getString(R.string.title_alarms)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onAlarmsChanged() {
        recyclerView?.post { alarmsAdapter.notifyDataSetChanged() }
        onChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onTimersChanged() {
        recyclerView?.post { alarmsAdapter.notifyDataSetChanged() }
        onChanged()
    }

    private fun onChanged() {
        empty?.visibility = if (alarmsAdapter.itemCount > 0) View.GONE else View.VISIBLE
    }

    class Instantiator(context: Context?) : ContextFragmentInstantiator(context!!) {
        override fun getTitle(context: Context?, position: Int): String? {
            return context?.getString(R.string.title_alarms)
        }

        override fun newInstance(position: Int): BasePagerFragment {
            return AlarmsFragment()
        }
    }
}
