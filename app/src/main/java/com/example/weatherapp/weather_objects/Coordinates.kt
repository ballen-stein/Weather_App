package com.example.weatherapp.weather_objects

import com.squareup.moshi.Json

class Coordinates{
    @Json(name = "lat") var lat : String ?= null
    @Json(name = "lon") var lng : String ?= null
}