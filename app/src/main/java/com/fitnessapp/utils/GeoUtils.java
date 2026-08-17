package com.fitnessapp.utils;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

/**
 * Geodesic calculation utility using Haversine formula and Instantaneous Pace calculator.
 */
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Calculates distance between two coordinates in kilometers using Haversine formula.
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static double calculateDistanceKm(LatLng p1, LatLng p2) {
        return calculateDistanceKm(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
    }

    /**
     * Calculates instantaneous pace in minutes per kilometer.
     * Pace = (Time in minutes) / (Distance in km)
     */
    public static double calculatePaceMinPerKm(double distanceKm, long durationSeconds) {
        if (distanceKm <= 0.005 || durationSeconds <= 0) {
            return 0.0;
        }
        double durationMinutes = durationSeconds / 60.0;
        return durationMinutes / distanceKm;
    }

    /**
     * Formats pace into "M'SS\"" string e.g., "5'12\""
     */
    public static String formatPace(double paceMinPerKm) {
        if (paceMinPerKm <= 0.0 || Double.isInfinite(paceMinPerKm) || Double.isNaN(paceMinPerKm) || paceMinPerKm > 60.0) {
            return "--'--\"";
        }
        int minutes = (int) paceMinPerKm;
        int seconds = (int) ((paceMinPerKm - minutes) * 60.0);
        return String.format("%d'%02d\"", minutes, seconds);
    }

    /**
     * Generates a realistic mock route for emulator / indoor testing around a base coordinate.
     */
    public static List<LatLng> generateSimulatedRoute(LatLng startLocation, int numPoints) {
        List<LatLng> route = new ArrayList<>();
        double currentLat = startLocation.latitude;
        double currentLng = startLocation.longitude;
        route.add(new LatLng(currentLat, currentLng));

        double heading = Math.random() * 2 * Math.PI;
        double stepSize = 0.00015; // ~15 meters per tick

        for (int i = 1; i < numPoints; i++) {
            heading += (Math.random() - 0.5) * 0.4; // gentle curve
            currentLat += Math.sin(heading) * stepSize;
            currentLng += Math.cos(heading) * stepSize;
            route.add(new LatLng(currentLat, currentLng));
        }

        return route;
    }
}
