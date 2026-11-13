package com.example.myweather.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import com.example.myweather.R
import com.example.myweather.presentation.HourlyData
import com.example.myweather.presentation.utils.WeatherTheme

class TemperatureAdapter : ListAdapter<HourlyData, TemperatureAdapter.ViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HourlyData>() {
            override fun areItemsTheSame(old: HourlyData, new: HourlyData) = old.time == new.time
            override fun areContentsTheSame(old: HourlyData, new: HourlyData) = old == new
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.textViewItemTime)
        val weatherImageView: ImageView = view.findViewById(R.id.imageViewItemWeather)
        val temperatureTextView: TextView = view.findViewById(R.id.textViewItemTemperature)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_temperature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.timeTextView.text = data.time.substringAfter('T')

        holder.temperatureTextView.text = "${data.temperature}°C"

        val weatherIconResId = WeatherTheme.getWeatherIcon(weatherCode = data.weatherCode, isDay = data.isDay)
        holder.weatherImageView.setImageResource(weatherIconResId)
    }


}
