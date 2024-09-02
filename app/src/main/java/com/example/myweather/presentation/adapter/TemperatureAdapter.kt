package com.example.myweather.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myweather.R
import com.example.myweather.presentation.HourlyData
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TemperatureAdapter(private var hourlyData: List<HourlyData>) : RecyclerView.Adapter<TemperatureAdapter.ViewHolder>() {

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
        val data = hourlyData[position]
        // Parse the time string to LocalDateTime
        val dateTime = LocalDateTime.parse(data.time)
        // Format the time part
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val formattedTime = dateTime.format(timeFormatter)
        // Set the formatted time to the TextView
        holder.timeTextView.text = formattedTime

        holder.temperatureTextView.text = "${data.temperature}°C"

        val weatherIconResId = getWeatherIcon(weatherCode = data.weatherCode, isDay = data.isDay)
        holder.weatherImageView.setImageResource(weatherIconResId)
    }

    override fun getItemCount() = hourlyData.size

    private fun getWeatherIcon(weatherCode: Int, isDay:Int): Int {
        return when (weatherCode) {
            0 -> if (isDay==1) R.drawable.clear1 else R.drawable.clear0
            1, 2, 3 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            45, 48 -> R.drawable.fog
            51, 53, 55, 56, 57 -> R.drawable.mc_cloudy
            61, 63, 65 -> R.drawable.rain
            66, 67 -> R.drawable.sleet
            71, 73, 75 -> R.drawable.l_snow
            77 -> R.drawable.hail
            80, 81, 82 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            85, 86 -> if (isDay==1) R.drawable.l_snow1 else R.drawable.l_snow0
            95 -> R.drawable.tstorm
            96, 99 -> R.drawable.tshower
            else -> R.drawable.unknown
        }
    }

    // function to update data using DiffUtil
    fun updateData(newData: List<HourlyData>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = hourlyData.size
            override fun getNewListSize() = newData.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return hourlyData[oldItemPosition].time == newData[newItemPosition].time
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return hourlyData[oldItemPosition] == newData[newItemPosition]
            }
        }

        val diffResult = DiffUtil.calculateDiff(diffCallback)
        hourlyData = newData
        diffResult.dispatchUpdatesTo(this)
    }
}
