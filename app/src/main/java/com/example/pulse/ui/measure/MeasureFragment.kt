package com.example.pulse.ui.measure

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.pulse.MainActivity
import com.example.pulse.databinding.FragmentMeasureBinding

class MeasureFragment : Fragment() {

    private var _binding: FragmentMeasureBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(MeasureViewModel::class.java)

        _binding = FragmentMeasureBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val button: Button = binding.button
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            button.text = it
        }

        button.setOnClickListener{
            val intent = Intent(activity, MainActivity::class.java)
            activity?.startActivity(intent)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}