package com.example.weatherapp.forecast_view

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.WeatherApp
import com.example.weatherapp.R
import com.example.weatherapp.weather_objects.ForecastWeatherValues

class ForecastViews(private val mContext: Context, private val forecastDays: MutableList<String>) {
    private val activity : WeatherApp = mContext as WeatherApp
    private lateinit var adapter : ForecastViewsAdapter
    private lateinit var recyclerView : RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var recyclerForecastList : MutableList<ForecastWeatherValues.ForecastWeather>

    fun weeklyForecastDisplay(){
        recyclerView = activity.findViewById(R.id.forecastRecycleView)
        recyclerView.setHasFixedSize(true)
        layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false)
        recyclerForecastList = activity.getForecastList()
        adapter = ForecastViewsAdapter(recyclerForecastList, mContext, forecastDays)
        recyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        recyclerView.adapter = adapter
    }
}