package com.fitnessapp.utils;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java implementation of the Google Polyline Algorithm (5-decimal precision).
 * Used for compressing high-resolution GPS tracks into compact strings and decoding for Map rendering.
 */
public class PolylineEncoder {

    /**
     * Encodes a list of LatLng coordinates into a Google Polyline string.
     */
    public static String encode(List<LatLng> points) {
        if (points == null || points.isEmpty()) {
            return "";
        }

        StringBuilder encoded = new StringBuilder();
        long lastLat = 0;
        long lastLng = 0;

        for (LatLng point : points) {
            long lat = Math.round(point.latitude * 1e5);
            long lng = Math.round(point.longitude * 1e5);

            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encodeValue(dLat, encoded);
            encodeValue(dLng, encoded);

            lastLat = lat;
            lastLng = lng;
        }

        return encoded.toString();
    }

    private static void encodeValue(long value, StringBuilder sb) {
        long v = value < 0 ? ~(value << 1) : (value << 1);
        while (v >= 0x20) {
            sb.append((char) ((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        sb.append((char) ((int) (v + 63)));
    }

    /**
     * Decodes a Google Polyline string back into a list of LatLng coordinates.
     */
    public static List<LatLng> decode(String encodedPath) {
        List<LatLng> poly = new ArrayList<>();
        if (encodedPath == null || encodedPath.isEmpty()) {
            return poly;
        }

        int index = 0;
        int len = encodedPath.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                if (index >= len) break;
                b = encodedPath.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                if (index >= len) break;
                b = encodedPath.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((double) lat / 1e5, (double) lng / 1e5);
            poly.add(p);
        }

        return poly;
    }
}
