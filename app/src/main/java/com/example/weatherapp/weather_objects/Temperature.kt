package com.example.weatherapp.weather_objects

import com.squareup.moshi.Json

class Temperature{
    @Json(name = "temp") var temp : Double ?= null
    @Json(name = "feels_like") var feels : Double ?= null
    @Json(name = "temp_min") var tempMin : Double ?= null
    @Json(name = "temp_max") var tempMax : Double ?= null
    @Json(name = "pressure") var pressure : Double ?= null
    @Json(name = "humidity") var humidity : Double ?= null
}