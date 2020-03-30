package com.example.weatherapp.firebase

import android.content.Context
import android.widget.Toast
import com.example.weatherapp.WeatherApp
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseConnection(private val mContext : Context){

    private var auth : FirebaseAuth
    private var currUser : FirebaseUser?= null
    private var activity : WeatherApp ?= null

    init {
        FirebaseApp.initializeApp(mContext)
        auth = FirebaseAuth.getInstance()
        activity = mContext as WeatherApp
        setFirebaseUser(auth.currentUser)
    }

    fun signIntoFirebase(user: String, pass: String) {
        auth.signInWithEmailAndPassword(user, pass)
            .addOnCompleteListener{ task ->
            if (task.isSuccessful) {
                val userFromAuth = auth.currentUser
                Toast.makeText(mContext, "Logged in successfully.", Toast.LENGTH_SHORT).show()
                currUser = userFromAuth
                activity?.setUser(true)
                activity?.setFavoriteList()
            } else {
                Toast.makeText(mContext, "Failed to log in.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun createFirebaseAccount(user : String, pass : String){
        auth.createUserWithEmailAndPassword(user, pass)
            .addOnCompleteListener{ task ->
                if (task.isSuccessful) {
                    val userFromAuth = auth.currentUser
                    Toast.makeText(mContext, "Account created successfully.", Toast.LENGTH_SHORT).show()
                    currUser = userFromAuth
                    activity?.setUser(true)
                    activity?.setFavoriteList()
                }
            }
    }

    fun signOutFirebase(){
        FirebaseAuth.getInstance().signOut()
    }

    fun setFirebaseUser(user: FirebaseUser?){
        currUser = user
    }

    fun getCurrUser() : FirebaseUser? {
        return currUser
    }

}