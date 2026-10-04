package pe.upc.simutalk.dtos;

public record GeoPoint(double latitude, double longitude) {

    public GeoPoint {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates: %s, %s".formatted(latitude, longitude));
        }
    }
}
