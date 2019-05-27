package id.wahyu.ocr

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.codelab.ocr.R
import kotlinx.android.synthetic.main.item.view.image
import kotlinx.android.synthetic.main.item.view.imagegs
import kotlinx.android.synthetic.main.item.view.prediction

class ItemAdapter(private val items: List<ItemPrediction>) : RecyclerView.Adapter<ItemHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): ItemHolder {
        return ItemHolder(LayoutInflater.from(viewGroup.context).inflate(R.layout.item, viewGroup, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        holder.bind(items[position])
    }
}

interface ItemListener {
    fun onClick(item: ItemPrediction)
}

class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val tvPrediction = view.prediction
    private val img = view.image
    private val imggs = view.imagegs

    fun bind(item: ItemPrediction) {
        tvPrediction.text = item.prediction
        img.setImageBitmap(item.image)
        imggs.setImageBitmap(item.imageGS)
//        img.setOnClickListener { listener.onClick(item) }
    }
}
