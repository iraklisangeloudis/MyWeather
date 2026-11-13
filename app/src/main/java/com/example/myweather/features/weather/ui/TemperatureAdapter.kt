package com.example.myweather.features.weather.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myweather.R
import com.example.myweather.features.weather.domain.model.HourlyForecast

class TemperatureAdapter : ListAdapter<HourlyForecast, TemperatureAdapter.ViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HourlyForecast>() {
            override fun areItemsTheSame(old: HourlyForecast, new: HourlyForecast) = old.time == new.time
            override fun areContentsTheSame(old: HourlyForecast, new: HourlyForecast) = old == new
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
