package com.example.weatherapp.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.lang.Exception

class DatabaseHelper (context : Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    companion object{
        private val DATABASE_NAME : String = "list_of_saved_cities"
        private val DATABASE_VERSION : Int = 1
    }

    private val COLUMN_ID : String = "id"
    private val COLUMN_CITY_NAME : String = "city_name"


    override fun onCreate(db : SQLiteDatabase) {
        db.execSQL("create table if not exists $DATABASE_NAME ($COLUMN_ID integer PRIMARY KEY AUTOINCREMENT, $COLUMN_CITY_NAME text)")
    }


    override fun onUpgrade(db : SQLiteDatabase, oldVal : Int, newVal : Int) {
        if(newVal > oldVal && newVal > DATABASE_VERSION)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    fun checkForCity(cityName: String) : Boolean{
        val db = this.readableDatabase
        val cursor = db.rawQuery("select * from $DATABASE_NAME where $COLUMN_CITY_NAME = '$cityName'", null)
        return !cursor.moveToFirst()
    }


    fun insertNewCity(cityName : String) : Boolean{
        val db = this.writableDatabase
        val contentValue = ContentValues()
        contentValue.put(COLUMN_CITY_NAME, cityName)
        return try{
            db.insert(DATABASE_NAME, null, contentValue)
            true
        }catch (e : Exception){
            e.printStackTrace()
            false
        }
    }


    fun getCities(): MutableList<String> {
        val db = this.readableDatabase
        val cursor = db.rawQuery("select * from $DATABASE_NAME", null)
        val cityList : MutableList<String> = ArrayList()
        if(cursor.moveToFirst()){
            while (!cursor.isAfterLast){
                val city = cursor.getString(cursor.getColumnIndex(COLUMN_CITY_NAME))
                cityList.add(city)
                cursor.moveToNext()
            }
        }
        return cityList
    }


    fun removeCity(cityName: String) : Boolean {
        val db = this.writableDatabase
        return try{
            db.execSQL("delete from $DATABASE_NAME where $COLUMN_CITY_NAME = '$cityName'")
            true
        } catch (e : Exception){
            e.printStackTrace()
            false
        }
    }
}