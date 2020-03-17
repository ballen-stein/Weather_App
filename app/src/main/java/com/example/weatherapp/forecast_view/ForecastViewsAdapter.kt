package com.example.weatherapp.forecast_view

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.WeatherApp
import com.example.weatherapp.weather_objects.ForecastWeatherValues

class ForecastViewsAdapter internal constructor(
    private val dataSet: MutableList<ForecastWeatherValues.ForecastWeather>,
    private val mContext: Context,
    private val forecastDays: MutableList<String>
) : RecyclerView.Adapter<ForecastViewsAdapter.ViewHolder>() {

    private val activity : WeatherApp = mContext as WeatherApp

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currForecast = dataSet[position]
        val date = holder.date
        val desc = holder.desc
        val img = holder.img
        val tempMin = holder.minTemp
        val tempMax = holder.maxTemp

        val dateSplit = currForecast.date?.split("/")
        val month = (activity.getStringMonth(dateSplit?.get(0).toString())).substring(0, 3)
        val day = dateSplit?.get(1)
        val forecastDate = "${forecastDays[position]}, $month $day"
        val degSymbol = mContext.getString(R.string.degree_symbol)
        val min = "${((currForecast.temperature?.tempMin!! - 273.15) * 9/5 + 32).toInt()}$degSymbol"
        val max = "${((currForecast.temperature?.tempMax!! - 273.15) * 9/5 + 32).toInt()}$degSymbol"

        date.text = forecastDate
        desc.text = currForecast.weather?.get(0)?.description
        tempMin.text = min
        tempMax.text = max
        img.background = activity.getWeatherStatusImage(currForecast.weather?.get(0)?.description)

        holder.setForecast(currForecast)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.forecast,
                parent,
                false
            )
        )
    }


    override fun getItemCount(): Int {
        return dataSet.size
    }


    class ViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        private lateinit var forecastWeather : ForecastWeatherValues.ForecastWeather
        var date : TextView
        var img : ImageView
        var desc : TextView
        var minTemp : TextView
        var maxTemp : TextView

        init {
            super.itemView
            date = itemView.findViewById(R.id.forecast_date)
            img = itemView.findViewById(R.id.forecast_img)
            desc = itemView.findViewById(R.id.forecast_desc)
            minTemp = itemView.findViewById(R.id.forecast_min)
            maxTemp = itemView.findViewById(R.id.forecast_max)
            itemView.setOnClickListener(this)
        }

        override fun onClick(view: View) { }

        fun setForecast(currForecast : ForecastWeatherValues.ForecastWeather) {
                forecastWeather = currForecast
        }

    }
}
