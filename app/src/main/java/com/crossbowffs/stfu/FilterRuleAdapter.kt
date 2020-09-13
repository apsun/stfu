package com.crossbowffs.stfu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class FilterRuleAdapter(private val context: Context) : BaseAdapter() {
    private val manager = FilterRuleManager(context)

    override fun getItem(position: Int): String {
        return manager.getByIndex(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return manager.getCount()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = getItem(position)
        return view
    }

    fun addRules(newRules: List<String>): Int {
        val ret = manager.addRules(newRules)
        notifyDataSetChanged()
        return ret
    }

    fun deleteByIndex(i: Int) {
        manager.deleteByIndex(i)
        notifyDataSetChanged()
    }
}
