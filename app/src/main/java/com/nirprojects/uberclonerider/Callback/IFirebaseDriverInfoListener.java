package com.nirprojects.uberclonerider.Callback;

import com.nirprojects.uberclonerider.Model.DriverGeoModel;

public interface IFirebaseDriverInfoListener {
    void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel);
}
