package com.crossbowffs.stfu

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class AreaCodeArrayAdapter(context: Context, entries: List<AreaCodeEntry>)
    : ArrayAdapter<AreaCodeEntry>(context, 0, entries)
{
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.spinner_item, parent, false)
        val entry = getItem(position)!!

        val primary = view.findViewById(android.R.id.text1) as TextView
        val secondary = view.findViewById(android.R.id.text2) as TextView

        primary.text = entry.name
        secondary.text = entry.areaCodes.joinToString(", ")
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}
