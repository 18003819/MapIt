package com.mapit.Objects;

import com.google.android.gms.maps.model.LatLng;

public class Trip
{
    String StartLocation;
    String EndLocation;
    String Duration;
    String Distance;
    String TripID;

    LatLng StartPoint;
    LatLng EndPoint;

    public String getStartLocation() {
        return StartLocation;
    }

    public String getEndLocation() {
        return EndLocation;
    }

    public String getDuration() {
        return Duration;
    }

    public String getDistance() {
        return Distance;
    }

    public String getTripID() {
        return TripID;
    }

    public LatLng getStartPoint() {
        return StartPoint;
    }

    public LatLng getEndPoint() {
        return EndPoint;
    }

    public Trip(String TripId, String StartLocation, String EndLocation, String Duration, String Distance, LatLng StartPoint, LatLng EndPoint) {
        this.TripID = TripId;
        this.StartLocation = StartLocation;
        this.EndLocation = EndLocation;
        this.Duration = Duration;
        this.Distance = Distance;
        this.StartPoint = StartPoint;
        this.EndPoint = EndPoint;
    }
}
