package com.hany.stock_correlation.recycle

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.hany.stock_correlation.CompareActivity
import com.hany.stock_correlation.MainActivity
import com.hany.stock_correlation.databinding.ItemRecyclerBinding

class ContentRVAdapter(val items:ArrayList<ContentModel>) :RecyclerView.Adapter<ContentRVAdapter.Viewholder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContentRVAdapter.Viewholder {
        val binding= ItemRecyclerBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: ContentRVAdapter.Viewholder, position: Int) {
        val cont = items.get(position)
        holder.setContent(cont)


    }
    override fun getItemCount(): Int {
        return items.size
    }
    inner class Viewholder(val binding: ItemRecyclerBinding) :RecyclerView.ViewHolder(binding.root){


        fun setContent(cont:ContentModel){
            binding.rk.text = "${cont.rank}"
            binding.c1.text = "${cont.code1}"
            binding.c2.text = "${cont.code2}"
            binding.corr.text = "${cont.corr}"

            binding.c1.setOnClickListener {
                var intent = Intent(itemView.context,CompareActivity::class.java)
                intent.putExtra("code2", binding.c1.text.toString())
                itemView.context.startActivity(intent)
            }
            binding.c2.setOnClickListener {
                var intent = Intent(itemView.context,CompareActivity::class.java)
                intent.putExtra("code2", binding.c2.text.toString())
                itemView.context.startActivity(intent)
            }
        }


    }

}