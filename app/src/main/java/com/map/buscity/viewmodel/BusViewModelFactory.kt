package com.map.buscity.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BusViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BusViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BusViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}