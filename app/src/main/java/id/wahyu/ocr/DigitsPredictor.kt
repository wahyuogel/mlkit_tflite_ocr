package id.wahyu.ocr

import com.google.firebase.ml.common.FirebaseMLException
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.graphics.*
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import android.graphics.Bitmap
import android.util.Log
import android.os.SystemClock
import android.widget.TextView
class DigitsPredictor {

    private val TAG = this.javaClass.simpleName

    /**
     * An instance of the driver class to run model inference with Firebase.
     */
    private var mModelInterpreter: FirebaseModelInterpreter? = null
    /**
     * Data configuration of input & output data of model.
     */
    private var mModelDataOptions: FirebaseModelInputOutputOptions? = null

    // Input byte buffer
    private var inputBuffer: ByteBuffer? = null

    init {
        try {

            inputBuffer = ByteBuffer.allocateDirect(
                    BYTE_SIZE_OF_FLOAT * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)
            inputBuffer!!.order(ByteOrder.nativeOrder())

            mModelDataOptions = FirebaseModelInputOutputOptions.Builder()
                    .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE))
                    .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(DIM_BATCH_SIZE, NUMBER_LENGTH))
                    .build()
            val localModel = FirebaseLocalModel.Builder(ASSET_FOLDER)
                    .setAssetFilePath(MODEL_PATH).build()

            FirebaseModelManager.getInstance().registerLocalModel(localModel)

            val modelOptions = FirebaseModelOptions.Builder()
                    .setLocalModelName(ASSET_FOLDER)
                    .build()

            mModelInterpreter = FirebaseModelInterpreter.getInstance(modelOptions)
        } catch (e: FirebaseMLException) {
            e.printStackTrace()
        }

    }

    /**
     * Get Text Prediction from Image with running tensorflow lite model inference
     */

    fun getTextPredictionFromImage(path: String?) {
        val iterate = BitmapUtils.BitmapUtils.listCharacterFromBitmap(path, DIM_IMG_SIZE_X, ID_CHAR_SIZE, ID_CHAR_PADDING)!!.listIterator()
        while (iterate.hasNext()) {
            val bitmapChar = iterate.next()
            preprocess(bitmapChar)
            try {
//                runModelInference(inputBuffer!!)
            } catch (e: FirebaseMLException) {
                e.printStackTrace()
            }
        }
    }

    fun predict(bitmap: Bitmap, tv : TextView){
        preprocess(bitmap)
        try {
            runModelInference(inputBuffer!!, tv)
        } catch (e: FirebaseMLException) {
            e.printStackTrace()
        }
    }

    /**
     * Run model inference from bytebuffer with interpreter into result prediction with output
     */

    fun runModelInference(byteBufferChar: ByteBuffer, tv: TextView) {
        val inputs = FirebaseModelInputs.Builder().add(byteBufferChar).build()
        mModelInterpreter!!
                .run(inputs, mModelDataOptions!!)
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                .addOnSuccessListener { result ->
                    for (i in 0 until result.getOutput<Array<FloatArray>>(0)!![0].size) {
                        val value = result.getOutput<Array<FloatArray>>(0)!![0][i]
                        Log.d(TAG, "Output (MLKIT) for " + Integer.toString(i) + ": " + java.lang.Float.toString(value))
                        if (value == 1f) {
                            tv.text = "Predicted Number (FB): " + i
                        }
                    }
                    tv.text = "Predicted Number (FB): " + -1

                }
                .continueWith { null }


    }

    /**
     * Writes Image data into a `ByteBuffer`.
     */
    @Synchronized
    private fun convertBitmapToByteBuffer(bitmap: Bitmap?): ByteBuffer {
        var bm = bitmap
        val imgData = ByteBuffer.allocateDirect(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * 4)
        imgData.order(ByteOrder.nativeOrder())
        bm = Bitmap.createScaledBitmap(bm!!, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true)

        val batchNum = 0
        val input = Array(1) { Array(DIM_IMG_SIZE_X) { Array(DIM_IMG_SIZE_Y) { FloatArray(3) } } }
        for (x in 0 until DIM_IMG_SIZE_X) {
            for (y in 0 until DIM_IMG_SIZE_Y) {
                val pixel = bm!!.getPixel(x, y)
                input[batchNum][x][y][0] = (Color.red(pixel) - 127) / 128.0f
                input[batchNum][x][y][1] = (Color.green(pixel) - 127) / 128.0f
                input[batchNum][x][y][2] = (Color.blue(pixel) - 127) / 128.0f
            }
        }
        return imgData
    }

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



    /**
     * Run on device / cloud text recognizer from Firebase Vision based on bitmap from image
     */
    fun imageToTextRecognizer(bitmap: Bitmap?, byCloud : Boolean): String {
        val stringBuffer = StringBuffer()
        val image = FirebaseVisionImage.fromBitmap(bitmap!!)
        var recognizer = FirebaseVision.getInstance().onDeviceTextRecognizer
        if(byCloud)
            recognizer = FirebaseVision.getInstance().cloudTextRecognizer
        recognizer.processImage(image)
                .addOnSuccessListener { texts ->
                    for (block in texts.textBlocks) {
                        for (line in block.lines) {
                            for (element in line.elements) {
                                stringBuffer.append(element.text)
                            }
                        }
                    }

                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
        return stringBuffer.toString()
    }


    companion object {

        // Name of the file in the assets folder
        private val MODEL_PATH = "model_tf.tflite"

        // Specify the output size
        private val NUMBER_LENGTH = 10

        // Specify the input size
        private val DIM_BATCH_SIZE = 1
        private val DIM_IMG_SIZE_X = 32
        private val DIM_IMG_SIZE_Y = 32
        private val DIM_PIXEL_SIZE = 1

        // Number of bytes to hold a float (32 bits / float) / (8 bits / byte) = 4 bytes / float
        private val BYTE_SIZE_OF_FLOAT = 4

        //ID length
        private val ID_CHAR_SIZE = 15

        //Padding between ID Character
        private val ID_CHAR_PADDING = 24

        private val ASSET_FOLDER = "asset"

    }

}



