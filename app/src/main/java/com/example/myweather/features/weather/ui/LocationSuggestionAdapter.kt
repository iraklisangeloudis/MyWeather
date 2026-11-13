package com.example.myweather.features.weather.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.example.myweather.databinding.ItemLocationSuggestionBinding
import com.example.myweather.features.location.domain.model.LocationSuggestion

class LocationSuggestionAdapter(context: Context, locations: List<LocationSuggestion>) :
    ArrayAdapter<LocationSuggestion>(context, 0, locations) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = convertView?.let(ItemLocationSuggestionBinding::bind)
            ?: ItemLocationSuggestionBinding.inflate(LayoutInflater.from(context), parent, false)
        val location = requireNotNull(getItem(position))
        val parts = location.displayName.split(',').map(String::trim).filter(String::isNotBlank)
        binding.cityName.text = parts.firstOrNull().orEmpty()
        binding.cityRegion.text = parts.drop(1).joinToString(", ")
        binding.cityRegion.isVisible = parts.size > 1
        binding.root.contentDescription = location.displayName
        return binding.root
    }
}
