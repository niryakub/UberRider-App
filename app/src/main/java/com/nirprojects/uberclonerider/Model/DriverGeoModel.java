package com.nirprojects.uberclonerider.Model;

import com.firebase.geofire.GeoLocation;

public class DriverGeoModel { //necessary data required for querries within firebase-db, on DriversLocation sub-table.
    private String key;
    private GeoLocation geoLocation;
    private DriverInfoModel driverInfoModel;

    public DriverGeoModel(){}

    public DriverGeoModel(String key, GeoLocation geoLocation) {
        this.key = key;
        this.geoLocation = geoLocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public DriverInfoModel getDriverInfoModel() {
        return driverInfoModel;
    }

    public void setDriverInfoModel(DriverInfoModel driverInfoModel) {
        this.driverInfoModel = driverInfoModel;
    }
}
