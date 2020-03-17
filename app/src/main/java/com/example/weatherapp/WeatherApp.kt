package com.example.weatherapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.weatherapp.forecast_view.ForecastViews
import com.example.weatherapp.weather_objects.CurrentWeatherValues
import com.example.weatherapp.weather_objects.ForecastWeatherValues
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class WeatherApp : AppCompatActivity() {

    private val client : OkHttpClient = OkHttpClient()
    private val baseUrl : String = "https://api.openweathermap.org/data/2.5/weather?"
    private val forecastUrl : String = "https://api.openweathermap.org/data/2.5/forecast?"
    private lateinit var location : String
    private lateinit var calendar : Date
    private val forecastDays : MutableList<String> = ArrayList()

    private var lng : Double ?= null
    private var lat : Double ?= null

    private val TAG = "WeatherApiLogs"
    private val moshi : Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private var readyToStart : Boolean = true

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var currWeatherStats : CurrentWeatherValues
    private var forecastWeatherStats : MutableList<ForecastWeatherValues.ForecastWeather> = ArrayList()
    private var forecastMin : MutableList<Double> = ArrayList()
    private var forecastMax : MutableList<Double> = ArrayList()

    private lateinit var forecastView : ForecastViews

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        calendar = Calendar.getInstance().time
        setForecastDays(calendar.toString().substring(0,3))
        forecastView = ForecastViews(this, forecastDays)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()
        setListeners()
    }


    private fun setForecastDays(currDay : String) {
        var nextDays : Array<String> ?= null
        when(currDay){
            "Mon" -> nextDays = arrayOf("Tues", "Wed", "Thurs", "Fri", "Sat")
            "Tues"-> nextDays = arrayOf("Wed", "Thurs", "Fri", "Sat", "Sun")
            "Wed" -> nextDays = arrayOf("Thurs", "Fri", "Sat", "Sun", "Mon")
            "Thurs"-> nextDays = arrayOf("Fri", "Sat", "Sun", "Mon", "Tues")
            "Fri" -> nextDays = arrayOf("Sat", "Sun", "Mon", "Tues", "Wed")
            "Sat"-> nextDays = arrayOf("Sun", "Mon", "Tues", "Wed", "Thurs")
            "Sun" -> nextDays = arrayOf("Mon", "Tues", "Wed", "Thurs", "Fri")
            else ->
                nextDays = arrayOf("Mon", "Tues", "Wed", "Thurs", "Fri")
        }
        forecastDays.addAll(nextDays)
    }


    private fun getCurrentLocation() {
        if(!locationPermissionCheck()){
            requestLocationPermission()
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                    location : Location? ->
                if(location != null) {
                    setLongLat(location.longitude, location.latitude)
                    checkWeather(true)
                }
            }
        }
    }


    private fun locationPermissionCheck(): Boolean {
        val permCheck = applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        return permCheck == PackageManager.PERMISSION_GRANTED
    }


    private fun requestLocationPermission(){
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
    }


    private fun setLongLat(longitude: Double, latitude: Double) {
        lng = longitude
        lat = latitude
    }


    override fun onRequestPermissionsResult(requestCode : Int, permissions : Array<String>, grantResults : IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 1){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation()
            }
        }
    }


    private fun setListeners(){
        searchBar()
    }


    private fun searchBar(){
        val sv = findViewById<SearchView>(R.id.city_search_view)
        sv.isSubmitButtonEnabled = true
        sv.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(queryText: String?): Boolean {
                location = queryText.toString()
                if(readyToStart){
                    checkWeather(false)
                    readyToStart = false
                }
                return true
            }
        })
    }


    private fun checkWeather(usingCords : Boolean){
        val apiUrl : String = when(usingCords){
            true -> baseUrl + "lat=$lat&lon=$lng&appid=${getString(R.string._ak)}"
            false -> baseUrl + "q=$location&appid=${getString(R.string._ak)}"
        }

        Observable.defer {
            try{
                val response : Response = getResponse(apiUrl)
                Observable.just(response)
            } catch (e : Exception){
                Observable.error<Exception>(e)
            }
        }.subscribeOn(Schedulers.io())
            .debounce (1, TimeUnit.SECONDS)
            .observeOn(Schedulers.newThread())
            .subscribe(
                { onNext ->
                    setCurrentWeather(onNext as Response)
                    setWeatherData()
                },
                { onError ->
                    println(onError) },
                { getWeeklyForecast(usingCords)
                }
            )
    }


    private fun getResponse(apiUrl : String) : Response {
        val request : Request = Request.Builder()
            .url(apiUrl)
            .build()
        return client.newCall(request).execute()
    }


    private fun setCurrentWeather(response : Response?) {
        val jsonAdapter = moshi.adapter(CurrentWeatherValues::class.java)
        try{
            val responseBody = response!!.body!!.string()
            val weatherFromResponse = jsonAdapter.fromJson(responseBody)
            if (weatherFromResponse != null) {
                currWeatherStats = weatherFromResponse
            }
        } catch (e : Exception){
            e.printStackTrace()
        }
    }


    private fun printTestValues(weatherFromResponse: CurrentWeatherValues) {
        println(
            "Long : ${weatherFromResponse.coordinates?.lng} \t Lat: ${weatherFromResponse.coordinates?.lat} \t" +
                    "\nSpeed : ${weatherFromResponse.windSpeed?.speed} \t Gust : ${weatherFromResponse.windSpeed?.gust} \t Degree : ${weatherFromResponse.windSpeed?.degree}" +
                    "\nTemp : ${weatherFromResponse.temperature?.temp} \t Feels : ${weatherFromResponse.temperature?.feels}" +
                    "\nVisibility : ${weatherFromResponse.visibility}" +
                    "\nClouds : ${weatherFromResponse.clouds?.cloudValue}"
        )
    }


    private fun setWeatherData(){
        val date = calcTime(currWeatherStats.dateTime.toString(), false)
        val city : String = currWeatherStats.cityName.toString()
        val currTemp : String = getTempAsFahrenheit((currWeatherStats.temperature?.temp!!)).toString() + resources.getString(R.string.degree_symbol) + "F"
        val currFeels : String = "Feels like: ${getTempAsFahrenheit((currWeatherStats.temperature?.feels!!))}${resources.getString(R.string.degree_symbol)}"
        val min : String = "L: ${getTempAsFahrenheit(currWeatherStats.temperature?.tempMin!!)}${resources.getString(R.string.degree_symbol)}"
        val max : String = "H: ${getTempAsFahrenheit(currWeatherStats.temperature?.tempMax!!)}${resources.getString(R.string.degree_symbol)}"
        val desc : String = (currWeatherStats.weather?.get(0)?.description).toString().capitalize()

        val wind : String = "${currWeatherStats.windSpeed?.speed} mph"
        val humidity : String = "${currWeatherStats.temperature!!.humidity?.toInt()}%"
        val visibility : String = "${(currWeatherStats.visibility!! * 0.00062137).toInt()} miles"
        val cloudiness : String = "${currWeatherStats.clouds?.cloudValue?.toInt()}% coverage"
        val rain : String = checkForNullRainOrSnow(currWeatherStats.rain)
        val snow : String = checkForNullRainOrSnow(currWeatherStats.snow)

        runOnUiThread{
            findViewById<TextView>(R.id.currentWeatherDate).text = date
            findViewById<TextView>(R.id.currentWeatherCity).text = city
            findViewById<TextView>(R.id.currentWeatherTemp).text = currTemp
            findViewById<TextView>(R.id.currentWeatherFeels).text = currFeels
            findViewById<TextView>(R.id.currentWeatherMin).text = min
            findViewById<TextView>(R.id.currentWeatherMax).text = max
            findViewById<TextView>(R.id.currentWeatherDesc).text = desc
            findViewById<ImageView>(R.id.currentWeatherImage).background = getWeatherStatusImage(currWeatherStats.weather?.get(0)?.description)

            findViewById<TextView>(R.id.currentWeatherWind).text = wind
            findViewById<TextView>(R.id.currentWeatherHumidity).text = humidity
            findViewById<TextView>(R.id.currentWeatherVisibility).text = visibility
            findViewById<TextView>(R.id.currentWeatherCloudiness).text = cloudiness
            findViewById<TextView>(R.id.currentWeatherRain).text = rain
            findViewById<TextView>(R.id.currentWeatherSnow).text = snow
        }
    }


    private fun calcTime(dateTime : String?, forecast : Boolean) : String {
        val newDate = if(!forecast) {
            val date = java.time.format.DateTimeFormatter.ISO_INSTANT
                .format(java.time.Instant.ofEpochSecond(dateTime!!.toLong()))
            (date.split("T"))[0].split("-".toRegex(), 3)
        } else{
            dateTime?.split("-".toRegex(), 3)
        }

        return if(!forecast){
            "${getStringMonth(newDate!![1])} ${newDate[2]}, ${newDate[0]}"
        } else {
            "${(newDate!![1])}/${newDate[2]}"
        }
    }


    fun getStringMonth(intDate : String) : String {
        return when(intDate){
            "01" -> "January"
            "02" -> "February"
            "03" -> "March"
            "04" -> "April"
            "05" -> "May"
            "06" -> "June"
            "07" -> "July"
            "08" -> "August"
            "09" -> "September"
            "10" -> "October"
            "11" -> "November"
            "12" -> "December"
            else -> {
                "Couldn't Find Month"
            }
        }
    }


    private fun getTempAsFahrenheit(kelvinTemp : Double) : Int {
        return ((kelvinTemp - 273.15) * 9/5 + 32).toInt()
    }


    private fun checkForNullRainOrSnow(value : CurrentWeatherValues.RainAndSnow?) : String{
        return if(value?.oneHour != null){
            if(value.threehour != null){
                "Coverage:\n1 Hour - ${value.oneHour} in\n3 Hour - ${value.threehour} in"
            } else {
                "Coverage:\n1 Hour - ${value.oneHour} in"
            }
        } else {
            "Currently none"
        }
    }


    fun getWeatherStatusImage(weatherDescription : String?): Drawable? {
        return when(weatherDescription){
            "few clouds" -> ContextCompat.getDrawable(this, R.drawable.ic_few_clouds)
            "broken clouds" -> ContextCompat.getDrawable(this, R.drawable.ic_broken_clouds)
            "overcast clouds" -> ContextCompat.getDrawable(this, R.drawable.ic_overcast_clouds)
            "scattered clouds" -> ContextCompat.getDrawable(this, R.drawable.ic_scattered_clouds)
            "clear sky" -> ContextCompat.getDrawable(this, R.drawable.ic_sunny)
            "light rain" -> ContextCompat.getDrawable(this, R.drawable.ic_light_rain)
            "moderate rain" -> ContextCompat.getDrawable(this, R.drawable.ic_moderate_rain)
            "heavy intensity rain" -> ContextCompat.getDrawable(this, R.drawable.ic_heavy_rain)
            "light snow" -> ContextCompat.getDrawable(this, R.drawable.ic_fog)
            else -> {
                ContextCompat.getDrawable(this, R.drawable.ic_sunny)
            }
        }
    }


    private fun getWeeklyForecast(usingCords: Boolean){
        val apiUrl : String = when(usingCords){
            true -> forecastUrl + "lat=$lat&lon=$lng&appid=${getString(R.string._ak)}"
            false -> forecastUrl + "q=$location&appid=${getString(R.string._ak)}"
        }

        Observable.defer {
            try{
                val response : Response = getResponse(apiUrl)
                Observable.just(response)
            } catch (e : Exception){
                Observable.error<Exception>(e)
            }
        }.subscribeOn(Schedulers.io())
            .debounce (1, TimeUnit.SECONDS)
            .observeOn(Schedulers.newThread())
            .subscribe(
                { onNext ->
                    setForecastWeather(onNext as Response)
                },
                { onError ->
                    println(onError) },
                { displayForecast()
                }
            )
    }


    private fun setForecastWeather(response : Response){
        val jsonAdapter = moshi.adapter(ForecastWeatherValues::class.java)
        try{
            val responseBody = response.body!!.string()
            val weatherFromResponse = jsonAdapter.fromJson(responseBody)
            forecastWeatherStats.clear()
            if (weatherFromResponse != null) {
                for(i in weatherFromResponse.weeklyWeather!!.indices){
                    forecastWeatherStats.add(weatherFromResponse.weeklyWeather!![i])
                    forecastWeatherStats[i].date = calcTime(forecastWeatherStats[i].date, true)

                }
                for(i in 0..4){
                    forecastMax.add(getForecastMinMax(i, "max"))
                    forecastMin.add(getForecastMinMax(i, "min"))
                }
            }
        } catch (e : Exception){
            println("Failed in the forecast because ")
        }
    }


    private fun getForecastMinMax(i: Int, s: String): Double {
        var minMaxValue = 0.0
        if(s=="max"){
            for(k in 0..7){
                val tempVal = forecastWeatherStats[(8*i)+k].temperature?.tempMax!!
                if(tempVal > minMaxValue)
                    minMaxValue = tempVal
            }
        } else {
            minMaxValue = 1000.0
            for(k in 0..7){
                val tempVal = forecastWeatherStats[(8*i)+k].temperature?.tempMin!!
                if(tempVal < minMaxValue)
                    minMaxValue = tempVal
            }
        }
        return minMaxValue
    }


    private fun displayForecast(){
        runOnUiThread{
            forecastView.weeklyForecastDisplay()
        }
    }


    fun getForecastList() : MutableList<ForecastWeatherValues.ForecastWeather> {
        val tempList = mutableListOf<ForecastWeatherValues.ForecastWeather>()
        for(i in 0..4){
            tempList.add(forecastWeatherStats[(4+(8*i))])
            if(i > 1)
                if(tempList[i-1].temperature?.tempMin!! > tempList[i].temperature?.tempMin!!){
                    tempList[i].temperature?.tempMin = tempList[i].temperature?.tempMin
                }
        }
        for(i in tempList.indices){
            tempList[i].date = tempList[i].date?.split(" ")?.get(0)
            tempList[i].temperature?.tempMin = forecastMin[i]
            tempList[i].temperature?.tempMax = forecastMax[i]
        }
        forecastMax.clear()
        forecastMin.clear()
        readyToStart = true
        return tempList
    }

}
