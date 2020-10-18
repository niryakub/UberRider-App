package com.nirprojects.uberclonerider.Model;

import java.util.Map;

public class FCMSendData {
    private String to; //driver's targeted token..
    private Map<String,String> data; //holds notification title and content/

    public FCMSendData(String to, Map<String, String> data) {
        this.to = to;
        this.data = data;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
