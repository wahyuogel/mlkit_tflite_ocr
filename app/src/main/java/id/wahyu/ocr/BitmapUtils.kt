package id.wahyu.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.lang.Exception


class BitmapUtils {
    /**
     * Get list of Character from Image Path based on specific rectangle region and position.
     */

    object BitmapUtils {
        fun listCharacterFromBitmap(path: String?, imageSize: Int, charLength: Int, fontSpacing: Int): MutableList<Bitmap>? {
            val bitmapWithregion = getBitmapFromImagePath(path, 450, 50, 260, 85)
            val list: MutableList<Bitmap> = ArrayList()
            var tempX = 0
            for (x in 0..charLength) {
                list.add(cropImage(bitmapWithregion, imageSize, imageSize, tempX, 0))
                tempX += fontSpacing
            }
            return list
        }

        /**
         * Get bitmap from image path and crop image into specific sizing
         */
        fun getBitmapFromImagePath(path: String?, rectWidth: Int, rectHeight: Int, x: Int, y: Int): Bitmap {
            val bm = BitmapFactory.decodeFile(path)
            return cropImage(bm, rectWidth, rectHeight, x, y);
        }

        /**
         * Crop Image from bitmap
         */
        fun cropImage(bm: Bitmap, rectWidth: Int, rectHeight: Int, x: Int, y: Int): Bitmap {
            val rect = Rect(0, 0, rectWidth, rectHeight)
            val mutableBitmap = bm.copy(Bitmap.Config.ARGB_8888, true)
            try {
                return Bitmap.createBitmap(mutableBitmap, x, y, rect.width(), rect.height())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return bm
        }
    }
}