package pe.upc.simutalk.services;

import pe.upc.simutalk.dtos.GeoPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Round-trip commute distance avoided by an asynchronous interview: great-circle distance
 * between the candidate's and the company's districts, times a road factor (streets are not
 * straight lines), times two (going and coming back).
 */
public final class CommuteDistancePolicy {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private CommuteDistancePolicy() {
    }

    public static double haversineKm(GeoPoint from, GeoPoint to) {
        var dLat = Math.toRadians(to.latitude() - from.latitude());
        var dLon = Math.toRadians(to.longitude() - from.longitude());
        var a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(from.latitude())) * Math.cos(Math.toRadians(to.latitude()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static BigDecimal roundTripKm(GeoPoint candidate, GeoPoint company, double roadFactor) {
        if (roadFactor < 1.0) {
            throw new IllegalArgumentException("Road factor must be at least 1");
        }
        return roundTripKm(haversineKm(candidate, company) * roadFactor);
    }

    public static BigDecimal roundTripKm(double oneWayKm) {
        if (oneWayKm < 0) {
            throw new IllegalArgumentException("Distance must be zero or positive");
        }
        return BigDecimal.valueOf(oneWayKm * 2).setScale(2, RoundingMode.HALF_UP);
    }
}
