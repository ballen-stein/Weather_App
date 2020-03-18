package com.example.weatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.drawerlayout.widget.DrawerLayout
import com.example.weatherapp.database.DatabaseHelper
import com.example.weatherapp.forecast_view.ForecastViews
import com.example.weatherapp.weather_objects.CurrentWeatherValues
import com.example.weatherapp.weather_objects.ForecastWeatherValues
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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

class WeatherApp : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val client : OkHttpClient = OkHttpClient()
    private val baseUrl : String = "https://api.openweathermap.org/data/2.5/weather?"
    private val forecastUrl : String = "https://api.openweathermap.org/data/2.5/forecast?"
    private lateinit var location : String
    private lateinit var calendar : Date
    private lateinit var db : DatabaseHelper
    private val forecastDays : MutableList<String> = ArrayList()

    private var lng : Double ?= null
    private var lat : Double ?= null

    private val TAG = "WeatherApiLogs"
    private val moshi : Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private var readyToStart : Boolean = true

    private var user : String ?= null
    private var pass : String ?= null

    private lateinit var cityList : MutableList<String>

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var currWeatherStats : CurrentWeatherValues
    private var forecastWeatherStats : MutableList<ForecastWeatherValues.ForecastWeather> = ArrayList()
    private var forecastMin : MutableList<Double> = ArrayList()
    private var forecastMax : MutableList<Double> = ArrayList()

    private lateinit var forecastView : ForecastViews

    private lateinit var auth : FirebaseAuth
    private var currUser : FirebaseUser ?= null

    private lateinit var drawer : DrawerLayout
    private lateinit var toggle : ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        db = DatabaseHelper(this)
        cityList = db.getCities()
        calendar = Calendar.getInstance().time
        setForecastDays(calendar.toString().substring(0,3))
        forecastView = ForecastViews(this, forecastDays)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        setFavoriteList()
        getCurrentLocation()
        setListeners()
        findViewById<ScrollView>(R.id.weatherScrollView).bringToFront()
    }


    private fun setFavoriteList(){
        val menu = findViewById<NavigationView>(R.id.nav_view).menu.getItem(3)
        menu.subMenu.clear()
        cityList.clear()
        cityList = db.getCities()
        runOnUiThread{
            for(i in cityList.indices){
                menu.subMenu.add(cityList[i])
                menu.subMenu[i].setOnMenuItemClickListener{item ->
                    location = menu.subMenu[i].title.toString()
                    drawer.closeDrawer(GravityCompat.START)
                    checkWeather(false)
                    true
                }
            }
        }
    }


    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_item_one -> {
                Toast.makeText(this, "Logging into the firebase", Toast.LENGTH_SHORT).show()
                loginToFirebase(false)
            }
            R.id.nav_item_two -> {
                if(currUser != null){
                    FirebaseAuth.getInstance().signOut()
                    findViewById<TextView>(R.id.nav_header_textView).text = ""
                    Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "You must be logged in to sign out", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.nav_item_three -> {
                loginToFirebase(true)
            }
        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    override fun onStart() {
        super.onStart()
        currUser = auth.currentUser
    }


    private fun loginToFirebase(newAccount : Boolean){
        val builder = AlertDialog.Builder(this)
        val inflater : LayoutInflater = layoutInflater
        val view : View = inflater.inflate(R.layout.firebase_auth, null)
        val okButtonText = if(newAccount){
            "Sign-up"
        } else {
            "Log-in"
        }
        builder.setView(view)
            .setTitle("Login to Firebase")
            .setPositiveButton(okButtonText) { dialogInterface, id ->
                val userView = view.findViewById<EditText>(R.id.username)
                val passView = view.findViewById<EditText>(R.id.password)
                if(userView.text.isNotBlank()  && passView.text.isNotBlank()){
                    user = userView.text.toString()
                    pass = passView.text.toString()
                    if(newAccount){
                        createFirebaseAccount(user!!, pass!!)
                    } else {
                        signIntoFirebase(user!!, pass!!)
                    }
                } else {
                    Toast.makeText(this, "Email or Password cannot be blank!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel") { dialogInterface, i ->
                dialogInterface.dismiss()
            }
            .create().show()
    }


    private fun signIntoFirebase(user: String, pass: String) {
        auth.signInWithEmailAndPassword(user, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userFromAuth = auth.currentUser
                    findViewById<TextView>(R.id.nav_header_textView).text = userFromAuth?.email
                    Toast.makeText(baseContext, "Logged in successfully.", Toast.LENGTH_SHORT).show()
                    currUser = userFromAuth
                } else {
                    Toast.makeText(baseContext, "Failed to log in.", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun createFirebaseAccount(user : String, pass : String){
        auth.createUserWithEmailAndPassword(user, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userFromAuth = auth.currentUser
                    findViewById<TextView>(R.id.nav_header_textView).text = userFromAuth?.email
                    Toast.makeText(baseContext, "Account created successfully.", Toast.LENGTH_SHORT).show()
                    currUser = userFromAuth
                }
            }
    }


    private fun setForecastDays(currDay : String) {
        val nextDays : Array<String> = when(currDay){
            "Mon" -> arrayOf("Tues", "Wed", "Thurs", "Fri", "Sat")
            "Tue"-> arrayOf("Wed", "Thurs", "Fri", "Sat", "Sun")
            "Wed" -> arrayOf("Thurs", "Fri", "Sat", "Sun", "Mon")
            "Thu"-> arrayOf("Fri", "Sat", "Sun", "Mon", "Tues")
            "Fri" -> arrayOf("Sat", "Sun", "Mon", "Tues", "Wed")
            "Sat"-> arrayOf("Sun", "Mon", "Tues", "Wed", "Thurs")
            "Sun" -> arrayOf("Mon", "Tues", "Wed", "Thurs", "Fri")
            else -> arrayOf("Mon", "Tues", "Wed", "Thurs", "Fri")
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
        saveButton()
        searchBar()
    }


    private fun saveButton(){
        val saveBtn = findViewById<LinearLayout>(R.id.save_city)
        saveBtn.setOnClickListener {
            val saveCity =
            try{
                location.capitalize()
            } catch (e : Exception){
                e.printStackTrace()
                findViewById<TextView>(R.id.currentWeatherCity).text.toString().capitalize()
            }
            if(db.checkForCity(saveCity)){
                if(db.insertNewCity(saveCity)){
                    setFavoriteList()
                    Toast.makeText(this, "City added!", Toast.LENGTH_SHORT).show()
                    findViewById<ImageView>(R.id.star_button).isSelected = true
                }
            } else{
                if(db.removeCity(saveCity)) {
                    setFavoriteList()
                    Toast.makeText(this, "City removed!", Toast.LENGTH_SHORT).show()
                    findViewById<ImageView>(R.id.star_button).isSelected = false
                }
            }
        }
    }


    private fun searchBar(){
        val sv = findViewById<SearchView>(R.id.searchCity)
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
                    setWeatherData(usingCords)
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
            readyToStart = true
        }
    }


    private fun setWeatherData(usingCords: Boolean) {
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

        if(usingCords){
            location = city
        }

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

            findViewById<ImageView>(R.id.star_button).isSelected = db.getCities().contains(location.capitalize())
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
            else -> "Couldn't Find Month"
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
            e.printStackTrace()
        }
    }


    private fun getForecastMinMax(i: Int, s: String): Double {
        var minMaxValue = -1000.0
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
