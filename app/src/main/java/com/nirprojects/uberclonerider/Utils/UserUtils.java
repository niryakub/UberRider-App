package com.nirprojects.uberclonerider.Utils;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nirprojects.uberclonerider.Common.Common;
import com.nirprojects.uberclonerider.Model.DriverGeoModel;
import com.nirprojects.uberclonerider.Model.FCMResponse;
import com.nirprojects.uberclonerider.Model.FCMSendData;
import com.nirprojects.uberclonerider.Model.TokenModel;
import com.nirprojects.uberclonerider.R;
import com.nirprojects.uberclonerider.Remote.IFCMService;
import com.nirprojects.uberclonerider.Remote.RetrofitFCMClient;
import com.nirprojects.uberclonerider.RequestDriverActivity;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class UserUtils {
    public static void updateUser(View view, Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.RIDER_INFO_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(updateData)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Snackbar.make(view,e.getMessage(),Snackbar.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(view,"Update information successfully",Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    public static void updateToken(Context context, String token){ //updates token within Realtime DB
        TokenModel tokenModel = new TokenModel(token);

        FirebaseDatabase.getInstance()
                .getReference(Common.TOKEN_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(tokenModel)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                });

    }

    public static void sendRequestToDriver(Context context, RelativeLayout main_layout, DriverGeoModel foundDriver, LatLng target) {

        CompositeDisposable compositeDisposable = new CompositeDisposable();
        IFCMService ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        //Get Driver's token to send notification to...
        FirebaseDatabase.getInstance().getReference(Common.TOKEN_REFERENCE).child(foundDriver.getKey())//ref to path Token/userKey/..
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists())
                    {
                        TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);//grab driver's token..

                        Map<String,String> notificationData = new HashMap<>();
                        notificationData.put(Common.NOTI_TITLE,Common.REQUEST_DRIVER_TITLE); // apply noti' title data
                        notificationData.put(Common.NOTI_CONTENT,"This message represent for request driver action");//apply noti' content
                        notificationData.put(Common.RIDER_PICKUP_LOCATION,new StringBuilder("")
                        .append(target.latitude)
                        .append(",")
                        .append(target.longitude)
                        .toString());

                        FCMSendData fcmSendData = new FCMSendData(tokenModel.getToken(),notificationData);
                        compositeDisposable.add(ifcmService.sendNotification(fcmSendData)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<FCMResponse>() {
                            @Override
                            public void accept(FCMResponse fcmResponse) throws Exception {
                                if(fcmResponse.getSuccess() == 0)
                                {
                                    compositeDisposable.clear();
                                    Snackbar.make(main_layout,context.getString(R.string.request_driver_failed),Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                compositeDisposable.clear();
                                Snackbar.make(main_layout,throwable.getMessage(),Snackbar.LENGTH_LONG).show();
                            }
                        }));

                    }
                    else
                    {
                        Snackbar.make(main_layout,context.getString(R.string.token_not_found),Snackbar.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Snackbar.make(main_layout,error.getMessage(),Snackbar.LENGTH_LONG).show();
                }
            });

    }
}
