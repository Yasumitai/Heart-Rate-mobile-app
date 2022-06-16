package com.example.pulse.ui.results

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.example.pulse.databinding.FragmentResultsBinding
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt


class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null

    private var value: Float? = 0.0F

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val resultsViewModel =
            ViewModelProvider(this).get(ResultsViewModel::class.java)

        _binding = FragmentResultsBinding.inflate(inflater, container, false)

        val root: View = binding.root
        val bpm: TextView = binding.textView
        val button: Button = binding.button2
        val textView: TextView = binding.textHome
        val stat: TextView = binding.textView5
//        val scatterChart = binding.pchart
        resultsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        val values = activity?.intent?.extras?.getParcelableArrayList<Entry>("df")

        val py: Python = Python.getInstance()
        val pyObj: PyObject = py.getModule("processing")
        val obj: String = pyObj.callAttr(
            "main", values.toString()
        ).toString()

//        Log.d("BPM", obj)

        val parsedArr = obj.removeSurrounding("[","]").replace("'", "").split(",")
        val arr : MutableList<Float> = mutableListOf<Float>()
        for (i in (0..(parsedArr.size-2))) {
            arr.add(parsedArr[i+1].toFloat()-parsedArr[i].toFloat())
        }

        var sum = 0.0F

        val scatterEntries = ArrayList<BarEntry>()

        for (i in (0..(arr.size-2))) {
            sum += (arr[i] - arr[i + 1]).pow(2)
            scatterEntries.add(BarEntry(arr[i], arr[i+1]))
        }

        val n: Float = sum/((arr.size-2).toFloat())
        val rmssd: Float = sqrt(n)
        stat.text = "RMSSD: $rmssd"

        bpm.text = "${parsedArr.size} bpm"

//        val scatterDataSet = ScatterDataSet(scatterEntries as List<Entry>?, "")
//        val scatterData = ScatterData(scatterDataSet)
//        scatterChart.data = scatterData
//        scatterDataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE)
//        scatterDataSet.color = Color.RED
//        scatterDataSet.setDrawValues(false)
//        scatterDataSet.valueTextSize = 18f
//        scatterChart.legend.isEnabled = false
//        scatterChart.description.isEnabled = false
//        scatterChart.setTouchEnabled(false)
//        scatterChart.setScaleEnabled(false)
//        scatterChart.setPinchZoom(false)
//        scatterChart.isDragEnabled = false

        button.setOnClickListener{
            val cal = Calendar.getInstance();
            val intent = Intent(Intent.ACTION_EDIT);
            intent.type = "vnd.android.cursor.item/event";
            intent.putExtra("beginTime", cal.timeInMillis);
            intent.putExtra("allDay", true);
            intent.putExtra("rrule", "FREQ=YEARLY");
            intent.putExtra("endTime", cal.timeInMillis +60*60*1000);
            intent.putExtra("title", "Pulse: ${parsedArr.size}");
            startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}