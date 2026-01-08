package org.smartregister.util;

import org.smartregister.domain.Location;
import org.smartregister.domain.LocationProperty;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public final class LocationStatusMapper {

    private static final Map<String, LocationProperty.PropertyStatus> LOOKUP = new HashMap<>();
    private static final Map<LocationProperty.PropertyStatus, String> SERIALIZED = new HashMap<>();

    static {
        SERIALIZED.put(LocationProperty.PropertyStatus.ACTIVE, "Active");
        SERIALIZED.put(LocationProperty.PropertyStatus.INACTIVE, "Inactive");


        for (Map.Entry<LocationProperty.PropertyStatus, String> e : SERIALIZED.entrySet()) {
            LocationProperty.PropertyStatus ps = e.getKey();
            String label = e.getValue();
            LOOKUP.put(label.toLowerCase(Locale.ROOT), ps);
            LOOKUP.put(ps.name().toLowerCase(Locale.ROOT), ps);
        }
    }

    private LocationStatusMapper() { /* utility */ }

    public static String toSerializedName(LocationProperty.PropertyStatus status) {
        if (status == null) return null;
        return SERIALIZED.get(status);
    }

    public static LocationProperty.PropertyStatus fromSerializedName(String value) {
        if (value == null) return null;
        return LOOKUP.get(value.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Copy status from Location.properties (enum) into Location.operatingStatus (string).
     * Safe if properties or status are null.
     */
    public static void copyPropertyStatusToLocation(Location location) {
        if (location == null) return;
        if (location.getProperties() == null) {
            location.setOperatingStatus(null);
            return;
        }
        LocationProperty.PropertyStatus ps = location.getProperties().getStatus();
        location.setOperatingStatus(toSerializedName(ps));
    }

    /**
     * Apply a stored column value (string) to the Location object.
     * Normalizes recognized values to the canonical serialized label; otherwise preserves the raw string.
     */
    public static void applyStoredStatusToLocation(Location location, String storedStatus) {
        if (location == null) return;
        if (storedStatus == null) {
            location.setOperatingStatus(null);
            return;
        }
        LocationProperty.PropertyStatus ps = fromSerializedName(storedStatus);
        if (ps != null) {
            location.setOperatingStatus(toSerializedName(ps));
        } else {
            location.setOperatingStatus(storedStatus);
        }
    }
}
