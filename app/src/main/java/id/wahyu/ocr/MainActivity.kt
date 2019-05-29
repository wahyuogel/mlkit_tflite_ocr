package id.wahyu.ocr

import android.graphics.Bitmap
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import id.wahyu.ocr.BitmapUtils.BitmapUtils.getBitmapFromAsset

import kotlinx.android.synthetic.main.activity_main.*
import com.google.firebase.ml.vision.text.FirebaseVisionText




class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var mSelectedImage: Bitmap? = null

    private var mDigitsDetector: DigitsDetector? = null
    private var mDigitsPredictor: DigitsPredictor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDigitsDetector = DigitsDetector(this@MainActivity)
        mDigitsPredictor = DigitsPredictor()
        button_text.setOnClickListener { detectWithFB() }
        button_run_custom_model.setOnClickListener { detectWithTF() }
        button_run_custom_model_list.setOnClickListener { detectWithTFNik() }
        button_rounded_box.setOnClickListener { generateBounding() }
        val dropdown = findViewById<Spinner>(R.id.spinner)
        val items = arrayOf(
//                "Test Image 1",
//                "Test Image 2",
//                "Test Image 3",
//                "Test Image 4",
//                "Test Image 5",
//                "Test Image 6",
//                "Test Image 7",
//                "Test Image 8",
//                "Test Image 9",
                "KTP 1",
                "KTP 2",
                "KTP 3")
        val adapter = ArrayAdapter(this, android.R.layout
                .simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter
        dropdown.onItemSelectedListener = this
    }

    private fun generateBounding() {
        processTextRecognitionResult(mSelectedImage)
    }


    private fun detectWithFB() {
        val scaledBitmap = Bitmap.createScaledBitmap(mSelectedImage!!, 32, 32, false)
        mDigitsPredictor!!.predict(scaledBitmap, text_view)
        showToast("Please monitor result from logcat!")
    }

    private fun detectWithTF() {
        text_view!!.text = "Predicted Number (TF): " + mDigitsDetector!!.classify(mSelectedImage!!).toString()
    }

    private fun detectWithTFNik() {
        val iterate = mDigitsDetector!!.classifyList(mSelectedImage!!).listIterator()
        val result = StringBuffer()
        while (iterate.hasNext()) {
            result.append(iterate.next().prediction)
        }
        image.setImageBitmap(mDigitsDetector!!.cropImage(mSelectedImage!!, 425, 40, 210, 100))
        text_view!!.text = result.toString()

        val itemAdapter = ItemAdapter(mDigitsDetector!!.classifyList(mSelectedImage!!))

        rv?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = itemAdapter
        }
    }

    private fun processTextRecognitionResult(bitmap : Bitmap?) {
        graphic_overlay.clear()
        val images = FirebaseVisionImage.fromBitmap(bitmap!!)
        val recognizer = FirebaseVision.getInstance()
                .onDeviceTextRecognizer
        recognizer.processImage(images)
                .addOnSuccessListener { texts ->
                    val blocks = texts.textBlocks
                    if (blocks.size == 0) {
                        text_view.text = "No text found"
                    }
                    for (i in blocks.indices) {
                        val lines = blocks[i].lines
                        for (j in lines.indices) {
                            val elements = lines[j].elements
                            for (k in elements.indices) {
                                val textGraphic = TextGraphic(graphic_overlay, elements[k])
                                val text = elements[k].text.toString()
                                val bounding = elements[k].boundingBox
                                if(text.length >= 15) {
                                    graphic_overlay.add(textGraphic)
                                    val bitmapByBound = mDigitsDetector!!.cropImage(mSelectedImage!!, bounding)
                                    image.setImageBitmap(bitmapByBound)
                                    val itemAdapter = ItemAdapter(mDigitsDetector!!.classifyListByCharacterDivider(bitmapByBound))
                                    rv?.apply {
                                        layoutManager = LinearLayoutManager(this@MainActivity)
                                        adapter = itemAdapter
                                    }
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }


    override fun onItemSelected(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        mSelectedImage = null
        when (position) {
//            0 -> mSelectedImage = getBitmapFromAsset(this, "sample_0.jpg")
//            1 -> mSelectedImage = getBitmapFromAsset(this, "sample_1.jpg")
//            2 -> mSelectedImage = getBitmapFromAsset(this, "sample_2.jpg")
//            3 -> mSelectedImage = getBitmapFromAsset(this, "sample_3.jpg")
//            4 -> mSelectedImage = getBitmapFromAsset(this, "sample_4.jpg")
//            5 -> mSelectedImage = getBitmapFromAsset(this, "sample_5.jpg")
//            6 -> mSelectedImage = getBitmapFromAsset(this, "sample_6.jpg")
//            7 -> mSelectedImage = getBitmapFromAsset(this, "sample_7.jpg")
//            8 -> mSelectedImage = getBitmapFromAsset(this, "sample_8.jpg")
            0 -> mSelectedImage = getBitmapFromAsset(this, "ktp_4.jpg")
            1 -> mSelectedImage = getBitmapFromAsset(this, "ktp_2.jpg")
            2 -> mSelectedImage = getBitmapFromAsset(this, "ktp_3.jpg")
        }
        image_view!!.setImageBitmap(mSelectedImage)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Do nothing
    }


}
