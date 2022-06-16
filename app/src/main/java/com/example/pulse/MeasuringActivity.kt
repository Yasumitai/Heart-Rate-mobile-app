package com.example.pulse

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.pulse.databinding.ActivityMeasuringBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.ln

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private val entries: ArrayList<Entry> = ArrayList()
    private val entries2: ArrayList<Entry> = ArrayList()
    private var lineDataSet = LineDataSet(entries, "")
    private var data = LineData(lineDataSet)
    var counter = 0
    var timeCounter: Long = 0
    var time: Long = 30000

    private lateinit var viewBinding: ActivityMeasuringBinding

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMeasuringBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        lineChart = findViewById(R.id.pchart)
        initLineChart()
    }

    private fun startCamera() {
        startTimeCounter()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "Average luminosity: $luma")
                        if (timeCounter <= time) {
                            entries.add(Entry(counter.toFloat(), -ln(luma.toFloat())))
                            entries2.add(Entry((time-timeCounter).toFloat(), -ln(luma.toFloat())))
                            lineDataSet.addEntry(Entry(counter.toString().toFloat(), -ln(luma.toFloat())))
                            data.notifyDataChanged()
                            lineChart.notifyDataSetChanged()
                            lineChart.setVisibleXRangeMaximum(50F)
                            lineChart.moveViewToX(data.entryCount.toFloat())
                            counter++
                        }

                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val cam = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

                if (cam.cameraInfo.hasFlashUnit())
                    cam.cameraControl.enableTorch(true)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startTimeCounter() {
        val countTime: TextView = findViewById(R.id.count)
        object : CountDownTimer(time+5000, 10) {
            override fun onTick(p0: Long) {
                timeCounter = p0
                if (timeCounter >= 30000)
                    countTime.text = "Measuring starts in "+((timeCounter-30000)/1000).toInt().toString()+" s"
                else
                    countTime.text = "Measuring: "+((timeCounter)/1000).toInt().toString() + " s"
            }

            override fun onFinish() {
                cameraExecutor.shutdown()
                countTime.text = "Finished!"
                intent = Intent(this@MainActivity, MenuActivity::class.java)
                intent.putExtra("df", entries2)
                intent.putExtra("page", R.id.navigation_home)
                startActivity(intent)
                finish()
            }
        }.start()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun initLineChart() {
        lineChart.data = data
        lineChart.setTouchEnabled(false)
        lineChart.setScaleEnabled(false)
        lineChart.setPinchZoom(false)
        lineChart.isDragEnabled = false
//        hide grid lines
        val xAxis: XAxis = lineChart.xAxis
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.setAvoidFirstLastClipping(true)
        //remove right y-axis
        val yAxis: YAxis = lineChart.axisLeft
        yAxis.spaceTop = 10.0F
        yAxis.spaceBottom = 10.0F
        yAxis.setDrawLabels(true)


        lineChart.axisRight.isEnabled = false

        //remove legend
        lineChart.legend.isEnabled = false
        //remove description label
        lineChart.description.isEnabled = false
        //add animation
        lineChart.animateX(1000, Easing.EaseInSine)

        // to draw label on xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM_INSIDE
        xAxis.setDrawLabels(true)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = +90f

        lineDataSet.setDrawCircles(false)
        lineDataSet.lineWidth = 3F
        lineDataSet.color = Color.RED
        lineDataSet.setDrawValues(false)
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        lineDataSet.cubicIntensity = 0.2F

        lineChart.invalidate()
    }


}
private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(image: ImageProxy) {

        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()

        listener(luma)

        image.close()
    }
}