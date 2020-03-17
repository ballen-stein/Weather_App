package com.example.weatherapp.weather_objects

import com.squareup.moshi.Json

class ForecastWeatherValues{

    @Json(name = "list") var weeklyWeather : List<ForecastWeather> ?= null
    class ForecastWeather {
        @Json(name = "main") var temperature : Temperature ?= null
        @Json(name = "weather") var weather : List<Weather> ?= null

        // Weather Specifics
        @Json(name = "wind") var windSpeed : Windspeed ?= null
        class Windspeed {
            @Json(name = "speed") var speed : Double ?= null
            @Json(name = "deg") var degree : Int ?= null
            @Json(name = "gust") var gust : Double ?= null
        }

        @Json(name = "clouds") var clouds : Clouds ?= null
        class Clouds {
            @Json(name = "all") var cloudValue : Double ?= null
        }

        @Json(name = "rain") var rain : RainAndSnow ?= null
        @Json(name = "snow") var snow : RainAndSnow ?= null

        class RainAndSnow {
            @Json(name = "1h") var onneHour : Double ?= null
            @Json(name = "3h") var threehour : Double ?= null
        }

        //Unique to forecast
        @Json(name = "dt_txt") var date : String ?= null
    }

    @Json(name = "city") var city : CityDetails ?= null

    class CityDetails {
        @Json(name = "coord") var coordinates : Coordinates ?= null
        @Json(name = "name") var cityName : String ?= null
        @Json(name = "country") var county : String ?= null
    }
}