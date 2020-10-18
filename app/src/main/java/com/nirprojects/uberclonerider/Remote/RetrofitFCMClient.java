package com.nirprojects.uberclonerider.Remote;

import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitFCMClient {
    //handles the connection to Firebase-messaging services.
    private static final String TAG = "RetrofitFCMClient";
    private static Retrofit instance;

    public static Retrofit getInstance(){
        Log.d(TAG,"getInstance()");
        return instance == null ? new Retrofit.Builder()
                .baseUrl("https://fcm.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create()) //same as gson-converter
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) //Adding this class to Retrofit allows you to return an Observable,
                .build() : instance;
    }
}
