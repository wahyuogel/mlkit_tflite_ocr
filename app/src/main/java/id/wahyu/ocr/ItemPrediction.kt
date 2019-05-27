package id.wahyu.ocr

import android.graphics.Bitmap

data class ItemPrediction(
        val image: Bitmap,
        val imageGS: Bitmap,
        val prediction: String
)