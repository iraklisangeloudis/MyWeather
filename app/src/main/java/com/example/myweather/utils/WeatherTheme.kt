package com.example.myweather.utils

import android.content.Context
import android.view.Window
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.example.myweather.R

object WeatherTheme {
    fun changeTheme(weatherCode: Int, isDay:Int, context: Context, window: Window, mainLayout: RelativeLayout) {
        if(weatherCode in 45..77 || weatherCode in 95..99){
            window.statusBarColor = ContextCompat.getColor(context, R.color.Rain_Blue)
            window.navigationBarColor = ContextCompat.getColor(context, R.color.Rain_Blue)
            mainLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.Rain_Blue))
        }
        else if(isDay==0){
            window.statusBarColor = ContextCompat.getColor(context, R.color.Night_Blue)
            window.navigationBarColor = ContextCompat.getColor(context, R.color.Night_Blue)
            mainLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.Night_Blue))
        }
        else{
            window.statusBarColor = ContextCompat.getColor(context, R.color.Clear_Day_Blue)
            window.navigationBarColor = ContextCompat.getColor(context, R.color.Clear_Day_Blue)
            mainLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.Clear_Day_Blue))
        }
    }

    fun getWeatherIcon(weatherCode: Int, isDay:Int): Int {
        return when (weatherCode) {
            0 -> if (isDay==1) R.drawable.clear1 else R.drawable.clear0
            1 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            2 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            3 -> if (isDay==1) R.drawable.mc_cloudy1 else R.drawable.mc_cloudy0
            45 -> R.drawable.fog
            48 -> R.drawable.fog
            51 -> R.drawable.mc_cloudy
            53 -> R.drawable.mc_cloudy
            55 -> R.drawable.mc_cloudy
            56 -> R.drawable.mc_cloudy
            57 -> R.drawable.mc_cloudy
            61 -> R.drawable.rain
            63 -> R.drawable.rain
            65 -> R.drawable.rain
            66 -> R.drawable.sleet
            67 -> R.drawable.sleet
            71 -> R.drawable.l_snow
            73 -> R.drawable.l_snow
            75 -> R.drawable.l_snow
            77 -> R.drawable.hail
            80 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            81 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            82 -> if (isDay==1) R.drawable.shower1 else R.drawable.shower0
            85 -> if (isDay==1) R.drawable.l_snow1 else R.drawable.l_snow0
            86 -> if (isDay==1) R.drawable.l_snow1 else R.drawable.l_snow0
            95 -> R.drawable.tstorm
            96 -> R.drawable.tshower
            99 -> R.drawable.tshower
            else -> R.drawable.unknown
        }
    }
}