package com.nirprojects.uberclonerider.Model;

import java.util.ArrayList;

public class GeoQueryModel { //fields are corresponding to the way we maintain locations within-DB.
    private String g;
    private ArrayList<Double> l;

    public GeoQueryModel(){
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

    public ArrayList<Double> getL() {
        return l;
    }

    public void setL(ArrayList<Double> l) {
        this.l = l;
    }
}
