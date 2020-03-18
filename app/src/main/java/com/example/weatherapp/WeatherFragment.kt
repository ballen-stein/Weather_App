package com.example.weatherapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.weatherapp.weather_objects.CurrentWeatherValues

class WeatherFragment : Fragment() {
    private var date : TextView ?= null
    private var cityName : TextView ?= null
    private var currTemp : TextView ?= null
    private var feelsList : TextView ?= null
    private var weatherMin : TextView ?= null
    private var weatherMax : TextView ?= null
    private var description : TextView ?= null
    private var wind : TextView ?= null
    private var humidity : TextView ?= null
    private var visibility : TextView ?= null
    private var cloudiness : TextView ?= null
    private var rain : TextView ?= null
    private var snow : TextView ?= null
    private var weatherImage : ImageView ?= null

    private var favoriteCity : LinearLayout ?= null
    private var searchCity : SearchView ?= null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.weather_fragment, container, false)
        setViews(view)
        if(arguments != null){
            date!!.text = arguments!!.getString("date")
            cityName!!.text = arguments!!.getString("cityName")
            currTemp!!.text = arguments!!.getDouble("currTemp").toString()
            feelsList!!.text = arguments!!.getDouble("currFeels").toString()
            weatherMin!!.text = arguments!!.getDouble("min").toString()
            weatherMax!!.text = arguments!!.getDouble("max").toString()
            description!!.text = arguments!!.getString("description")
            wind!!.text = arguments!!.getDouble("wind").toString()
            humidity!!.text = arguments!!.getInt("humidity").toString()
            visibility!!.text = arguments!!.getInt("visibility").toString()
            cloudiness!!.text = arguments!!.getInt("cloudiness").toString()
            //rain!!.text = arguments!!.getDouble("rainOne").toString()
            //snow!!.text = arguments!!.getInt("snowOne").toString()
            //weatherImage = v.findViewById(R.id.currentWeatherImage)

        }
        return view
    }

    private fun setViews(v : View){
        date = v.findViewById(R.id.currentWeatherDate)
        cityName = v.findViewById(R.id.currentWeatherCity)
        currTemp = v.findViewById(R.id.currentWeatherTemp)
        feelsList = v.findViewById(R.id.currentWeatherFeels)
        weatherMin = v.findViewById(R.id.currentWeatherMin)
        weatherMax = v.findViewById(R.id.currentWeatherMax)
        description = v.findViewById(R.id.currentWeatherDesc)
        wind = v.findViewById(R.id.currentWeatherWind)
        humidity = v.findViewById(R.id.currentWeatherHumidity)
        visibility = v.findViewById(R.id.currentWeatherVisibility)
        cloudiness = v.findViewById(R.id.currentWeatherCloudiness)
        rain = v.findViewById(R.id.currentWeatherRain)
        snow = v.findViewById(R.id.currentWeatherSnow)
        weatherImage = v.findViewById(R.id.currentWeatherImage)

        favoriteCity = v.findViewById(R.id.save_city)
        searchCity = v.findViewById(R.id.city_search_view)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        setToNull()
    }

    private fun setToNull(){
        date = null
        cityName = null
        currTemp = null
        feelsList = null
        weatherMin = null
        weatherMax = null
        description = null
        wind = null
        humidity = null
        visibility = null
        cloudiness = null
        rain = null
        snow = null
        weatherImage = null
        favoriteCity = null
        searchCity = null
    }

    companion object{

        fun newInstance(currWeatherStats: CurrentWeatherValues) : WeatherFragment {
            val fragment = WeatherFragment()
            val bundle = Bundle()
            bundle.putString("date", currWeatherStats.dateTime.toString())
            bundle.putString("cityName", currWeatherStats.cityName)
            bundle.putDouble("currTemp", currWeatherStats.temperature?.temp!!)
            bundle.putDouble("currFeels", currWeatherStats.temperature?.feels!!)
            bundle.putDouble("min", currWeatherStats.temperature?.tempMin!!)
            bundle.putDouble("max", currWeatherStats.temperature?.tempMax!!)
            bundle.putString("desc", currWeatherStats.weather?.get(0)?.description)
            bundle.putDouble("wind", currWeatherStats.windSpeed?.speed!!)
            bundle.putInt("humidity", currWeatherStats.temperature!!.humidity?.toInt()!!)
            bundle.putInt("visibility", (currWeatherStats.visibility!! * 0.00062137).toInt())
            bundle.putInt("cloudiness", (currWeatherStats.clouds!!.cloudValue!!).toInt())
            //bundle.putDouble("rainOne", currWeatherStats.rain?.oneHour!!)
            //bundle.putDouble("rainThree", currWeatherStats.rain?.threehour?)
            //bundle.putDouble("snowOne", currWeatherStats.snow?.oneHour!!)
            //bundle.putDouble("snowThree", currWeatherStats.snow?.threehour!!)

            fragment.arguments = bundle
            return fragment
        }

    }

    /*

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
        Log.d(TAG, "Location after setting data : $location")
    }

    */

}