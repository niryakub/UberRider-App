package com.nirprojects.uberclonerider.Remote;

import android.util.Log;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {
    //Retrofit instance Singleton...
    private static final String TAG = "RetrofitClient";
    private static Retrofit instance;

    public static Retrofit getInstance(){
        Log.d(TAG,"onGetInstance()");
        return instance == null ? new Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com/")
                .addConverterFactory(ScalarsConverterFactory.create()) //same as gson-converter
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) //Adding this class to Retrofit allows you to return an Observable,
                .build() : instance;
    }

}
