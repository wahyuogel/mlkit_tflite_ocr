package id.wahyu.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*

import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var mSelectedImage: Bitmap? = null

    private var mDigitsDetector : DigitsDetector? = null
    private var mDigitsPredictor : DigitsPredictor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mDigitsDetector = DigitsDetector(this@MainActivity)
        mDigitsPredictor = DigitsPredictor()
        button_text.setOnClickListener { detectWithFB() }
        button_run_custom_model.setOnClickListener { detectWithTF() }
        button_run_custom_model_list.setOnClickListener { detectWithTFNik() }
        val dropdown = findViewById<Spinner>(R.id.spinner)
        val items = arrayOf(
                "Test Image 1",
                "Test Image 2",
                "Test Image 3",
                "Test Image 4",
                "Test Image 5",
                "Test Image 6",
                "Test Image 7",
                "Test Image 8",
                "Test Image 9",
                "KTP 1",
                "KTP 2",
                "KTP 3")
        val adapter = ArrayAdapter(this, android.R.layout
                .simple_spinner_dropdown_item, items)
        dropdown.adapter = adapter
        dropdown.onItemSelectedListener = this
    }


    private fun detectWithFB(){
        val scaledBitmap = Bitmap.createScaledBitmap(mSelectedImage!!, 32, 32, false)
        mDigitsPredictor!!.predict(scaledBitmap,text_view)
        showToast("Please monitor result from logcat!")
    }

    private fun detectWithTF(){
        text_view!!.text = "Predicted Number (TF): " + mDigitsDetector!!.classify(mSelectedImage!!).toString()
    }

    private fun detectWithTFNik(){
        val iterate = mDigitsDetector!!.classifyList(mSelectedImage!!).listIterator()
        val result = StringBuffer()
        while (iterate.hasNext()) {
            result.append(iterate.next().prediction)
        }
        image.setImageBitmap(mDigitsDetector!!.cropImage(mSelectedImage!!, 425, 40, 210, 100))
        text_view!!.text = result.toString()

        val itemAdapter = ItemAdapter(mDigitsDetector!!.classifyList(mSelectedImage!!))

        rv.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = itemAdapter
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }


    override fun onItemSelected(parent: AdapterView<*>, v: View, position: Int, id: Long) {
        mSelectedImage = null
        when (position) {
            0 -> mSelectedImage = getBitmapFromAsset(this, "sample_0.jpg")
            1 -> mSelectedImage = getBitmapFromAsset(this, "sample_1.jpg")
            2 -> mSelectedImage = getBitmapFromAsset(this, "sample_2.jpg")
            3 -> mSelectedImage = getBitmapFromAsset(this, "sample_3.jpg")
            4 -> mSelectedImage = getBitmapFromAsset(this, "sample_4.jpg")
            5 -> mSelectedImage = getBitmapFromAsset(this, "sample_5.jpg")
            6 -> mSelectedImage = getBitmapFromAsset(this, "sample_6.jpg")
            7 -> mSelectedImage = getBitmapFromAsset(this, "sample_7.jpg")
            8 -> mSelectedImage = getBitmapFromAsset(this, "sample_8.jpg")
            9 -> mSelectedImage = getBitmapFromAsset(this, "ktp_1.jpg")
            10 -> mSelectedImage = getBitmapFromAsset(this, "ktp_2.jpg")
            11 -> mSelectedImage = getBitmapFromAsset(this, "ktp_3.jpg")
        }
        image_view!!.setImageBitmap(mSelectedImage)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        // Do nothing
    }

    fun getBitmapFromAsset(context: Context, filePath: String): Bitmap? {
        val assetManager = context.assets
        val `is`: InputStream
        var bitmap: Bitmap? = null
        try {
            `is` = assetManager.open(filePath)
            bitmap = BitmapFactory.decodeStream(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bitmap
    }




}
