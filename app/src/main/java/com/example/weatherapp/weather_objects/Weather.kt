package com.example.weatherapp.weather_objects

import com.squareup.moshi.Json

class Weather {
    @Json(name = "id") var id : Int ?= null
    @Json(name = "main") var mainWeather : String ?= null
    @Json(name = "description") var description : String ?= null
    @Json(name = "icon") var icon : String ?= null
}