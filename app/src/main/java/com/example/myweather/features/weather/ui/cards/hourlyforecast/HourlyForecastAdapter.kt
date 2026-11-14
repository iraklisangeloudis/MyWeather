package com.example.myweather.features.weather.ui.cards.hourlyforecast

import com.example.myweather.features.weather.ui.util.WeatherIcons
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myweather.R
import com.example.myweather.features.weather.domain.model.HourlyForecast

class HourlyForecastAdapter : ListAdapter<HourlyForecast, HourlyForecastAdapter.ViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<HourlyForecast>() {
            override fun areItemsTheSame(old: HourlyForecast, new: HourlyForecast) = old.time == new.time
            override fun areContentsTheSame(old: HourlyForecast, new: HourlyForecast) = old == new
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.textViewItemTime)
        val weatherImageView: LottieAnimationView = view.findViewById(R.id.imageViewItemWeather)
        val temperatureTextView: TextView = view.findViewById(R.id.textViewItemTemperature)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_temperature, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = getItem(position)
        holder.timeTextView.text = data.time.substringAfter('T')

        holder.temperatureTextView.text = holder.itemView.context.getString(R.string.temperature_value, data.temperature)

        WeatherIcons.bind(holder.weatherImageView, data.weatherCode, data.isDay)
    }

}
