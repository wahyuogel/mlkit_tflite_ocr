package id.wahyu.ocr

import android.app.Activity
import android.graphics.*
import android.os.SystemClock
import android.util.Log

import org.tensorflow.lite.Interpreter

import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.graphics.Bitmap
import com.google.firebase.ml.common.FirebaseMLException
import android.R.attr.left



class DigitsDetector(activity: Activity) {
    private val TAG = this.javaClass.simpleName

    // The tensorflow lite file
    private var tflite: Interpreter? = null

    // Input byte buffer
    private var inputBuffer: ByteBuffer? = null

    // Output array [batch_size, 10]
    private var mnistOutput: Array<FloatArray>? = null


    init {
        try {
            tflite = Interpreter(loadModelFile(activity))
            inputBuffer = ByteBuffer.allocateDirect(
                    BYTE_SIZE_OF_FLOAT * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
            inputBuffer!!.order(ByteOrder.nativeOrder())
            mnistOutput = Array(DIM_BATCH_SIZE) { FloatArray(NUMBER_LENGTH) }
            Log.d(TAG, "Created a Tensorflow Lite Classifier.")
        } catch (e: IOException) {
            Log.e(TAG, "IOException loading the tflite file")
        }

    }

    /**
     * Run the TFLite model
     */
    protected fun runInference() {
        tflite!!.run(inputBuffer!!, mnistOutput!!)
    }

    /**
     * Classifies the number with the mnist model.
     *
     * @param bitmap
     * @return the identified number
     */
    fun classify(bitmap: Bitmap): Int {
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
        }
        preprocess(toGrayscale(bitmap))
        runInference()
        return postprocess()
    }

    fun classifyList(bitmap: Bitmap): MutableList<ItemPrediction> {
        val list: MutableList<ItemPrediction> = ArrayList()
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
        }

        val iterate = listCharacterFromBitmap(bitmap)!!.listIterator()
        while (iterate.hasNext()) {
            val bitmapChar = iterate.next()
            try {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmapChar!!, 32, 32, false)
                preprocess(scaledBitmap)
                runInference()
                list.add(ItemPrediction(scaledBitmap, toGrayscale(bitmapChar), "" + postprocess()))
            } catch (e: FirebaseMLException) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun classifyListByCharacterDivider(bitmap: Bitmap): MutableList<ItemPrediction> {
        val list: MutableList<ItemPrediction> = ArrayList()
        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.")
        }

        val iterate = divideCharacterFromBitmap(bitmap)!!.listIterator()
        while (iterate.hasNext()) {
            val bitmapChar = iterate.next()
            try {
                val scaledBitmap = Bitmap.createScaledBitmap(bitmapChar!!, 32, 32, false)
                preprocess(scaledBitmap)
                runInference()
                list.add(ItemPrediction(scaledBitmap, toGrayscale(bitmapChar), "" + postprocess()))
            } catch (e: FirebaseMLException) {
                e.printStackTrace()
            }
        }
        return list
    }

    /**
     * Go through the output and find the number that was identified.
     *
     * @return the number that was identified (returns -1 if one wasn't found)
     */
    private fun postprocess(): Int {
        for (i in 0 until mnistOutput!![0].size) {
            val value = mnistOutput!![0][i]
            Log.d(TAG, "Output (TF) for " + Integer.toString(i) + ": " + java.lang.Float.toString(value))
            if (value == 1f) {
                return i
            }
        }
        return -1
    }


    /**
     * Load the model file from the assets folder
     */
    @Throws(IOException::class)
    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor = activity.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Converts it into the Byte Buffer to feed into the model
     *
     * @param bitmap
     */
    private fun preprocess(bitmap: Bitmap?) {
        if (bitmap == null || inputBuffer == null) {
            return
        }


        // Reset the image data
        inputBuffer!!.rewind()

        val width = bitmap.width
        val height = bitmap.height

        val startTime = SystemClock.uptimeMillis()

        // The bitmap shape should be 32 x 32
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            // Set 0 for white and 255 for black pixels
            val pixel = pixels[i]
            // The color of the input is black so the blue channel will be 0xFF.
            val channel = pixel and 0xff
            inputBuffer!!.putFloat((0xff - channel).toFloat())
        }
        val endTime = SystemClock.uptimeMillis()
        Log.d(TAG, "Time cost to put values into ByteBuffer: " + java.lang.Long.toString(endTime - startTime))
    }


    fun toGrayscale(srcImage: Bitmap): Bitmap {

        val width = srcImage.getWidth()
        val height = srcImage.getHeight()
        // create output bitmap
        val bmOut = Bitmap.createBitmap(width, height, srcImage.getConfig())
        // color information
        var A: Int
        var R: Int
        var G: Int
        var B: Int
        var pixel: Int
        for (x in 0 until width) {
            for (y in 0 until height) {
                // get pixel color
                pixel = srcImage.getPixel(x, y)
                A = Color.alpha(pixel)
                R = Color.red(pixel)
                G = Color.green(pixel)
                B = Color.blue(pixel)
                var gray = (0.2989 * R + 0.5870 * G + 0.1140 * B).toInt()
                // use 128 as threshold, above -> white, below -> black
                if (gray > 128) {
                    gray = 255
                } else {
                    gray = 0
                }
                // set new pixel color to output bitmap
                bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray))
            }
        }
        return bmOut
    }

    /**
     * Get list of Character from Image Path based on specific rectangle region and position.
     */
    private fun divideCharacterFromBitmap(bitmap: Bitmap?): MutableList<Bitmap>? {
        val list: MutableList<Bitmap> = ArrayList()
        var tempX = 0
        for (x in 0..15) {
            list.add(cropImage(bitmap!!, bitmap.width/16, bitmap.width/16, tempX, bitmap.width/16/3))
            tempX += bitmap.width/16
        }
        return list
    }

    /**
     * Get list of Character from Image Path based on specific rectangle region and position.
     */
    private fun listCharacterFromBitmap(bitmap: Bitmap?): MutableList<Bitmap>? {
        val bitmapWithregion = cropImage(bitmap!!, 425, 40, 210, 100)
        val list: MutableList<Bitmap> = ArrayList()
        var tempX = 0
        for (x in 0..15) {
            list.add(cropImage(bitmapWithregion, 32, 32, tempX, THRESHOLD_TOLERANCE_Y))
            if (x > 6)
                tempX += 25 + 2
            else
                tempX += 25
        }
        return list
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

    /**
     * Crop Image from bitmap
     */
    fun cropImage(bm: Bitmap, rectf: Rect?) : Bitmap{
        val mutableBitmap = bm.copy(Bitmap.Config.ARGB_8888, true)
        try {
            return Bitmap.createBitmap(mutableBitmap, rectf!!.left, rectf!!.top, rectf!!.width(), rectf!!.height(), null, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return bm
    }

    companion object {

        // Name of the file in the assets folder
        private val MODEL_PATH = "model_6d09c6e6fef4461fada742a7d3c42d87.tflite"

        // Specify the output size
        private val NUMBER_LENGTH = 10

        // Specify the input size
        private val DIM_BATCH_SIZE = 1
        private val DIM_IMG_SIZE_X = 32
        private val DIM_IMG_SIZE_Y = 32
        private val DIM_PIXEL_SIZE = 1

        // Number of bytes to hold a float (32 bits / float) / (8 bits / byte) = 4 bytes / float
        private val BYTE_SIZE_OF_FLOAT = 4

        private val THRESHOLD_TOLERANCE_X = 5
        private val THRESHOLD_TOLERANCE_Y = 6
    }
}