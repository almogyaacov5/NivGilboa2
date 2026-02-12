package com.example.nivgilboaapp;

import java.io.Serializable;

public class Restaurant implements Serializable {
    // Firebase key (child.getKey())
    public String id;

    public String name;
    public String address;
    public String cuisine;
    public String priceLevel;
    public String reviewSummary;
    public String videoUrl;
    public float rating;
    public double lat, lng;
    public boolean kosher;

    public Restaurant() {
        // Required empty constructor for Firebase
    }

    public Restaurant(String name, String address, String cuisine, float rating,
                      double lat, double lng, boolean kosher, String priceLevel) {
        this.name = name;
        this.address = address;
        this.cuisine = cuisine;
        this.rating = rating;
        this.lat = lat;
        this.lng = lng;
        this.kosher = kosher;
        this.priceLevel = priceLevel;
        this.reviewSummary = "ביקורת ניב גלבוע";
        this.videoUrl = "";
    }

    public Restaurant(String name, String address, String cuisine, float rating,
                      double lat, double lng, boolean kosher, String priceLevel,
                      String reviewSummary, String videoUrl) {
        this(name, address, cuisine, rating, lat, lng, kosher, priceLevel);
        this.reviewSummary = reviewSummary;
        this.videoUrl = videoUrl;
    }
}
