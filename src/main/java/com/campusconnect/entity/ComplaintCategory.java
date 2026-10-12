package com.campusconnect.entity;

public enum ComplaintCategory {
    INFRASTRUCTURE("Infrastructure"),
    ELECTRICITY("Electricity"),
    WATER_SUPPLY("Water Supply"),
    CLEANLINESS("Cleanliness"),
    WIFI_INTERNET("Wi-Fi and Internet"),
    CLASSROOM_LAB("Classroom or Laboratory Issues"),
    OTHER("Other Campus Issues");

    private final String label;

    ComplaintCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
