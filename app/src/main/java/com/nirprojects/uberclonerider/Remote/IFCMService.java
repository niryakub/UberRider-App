package com.nirprojects.uberclonerider.Remote;

import com.nirprojects.uberclonerider.Model.FCMResponse;
import com.nirprojects.uberclonerider.Model.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({ //It's like the json we're sending through Advanced REST Client API...
            "Content-Type:application/json",
            "Authorization:key=AAAAea9S-Jw:APA91bFbkmXs-Pi7-a4w7mMtYLzkliLb1qbmNVl0XRdqmDfq1aZ523cpEQ2A-rDq9GDh-gPMfKifBLkbhrg6wNVj4yQBQw4eCGfG02NZrFPtLc7GdjmP8t6-JCn3Xzr1ZC4a0c3nj9Rq"
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
