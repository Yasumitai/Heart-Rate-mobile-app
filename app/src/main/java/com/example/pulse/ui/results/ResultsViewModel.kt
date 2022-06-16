package com.example.pulse.ui.results

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ResultsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Your pulse is:"
    }
    val text: LiveData<String> = _text
}