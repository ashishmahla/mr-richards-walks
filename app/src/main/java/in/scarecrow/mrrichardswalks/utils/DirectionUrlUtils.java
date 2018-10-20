package in.scarecrow.mrrichardswalks.utils;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

import in.scarecrow.mrrichardswalks.Config;

public class DirectionUrlUtils {
    public static String getMapsUrl(LatLng origin, LatLng dest, ArrayList<LatLng> waypoints) {
        StringBuilder builder = new StringBuilder();
        builder.append("https://maps.googleapis.com/maps/api/directions/json?origin=");
        builder.append(origin.latitude).append(",").append(origin.longitude);

        if (waypoints.size() > 0) {
            builder.append("&waypoints=");
            for (int i = 0; i < waypoints.size(); i++) {
                if (i != 0) {
                    builder.append("|");
                }
                builder.append("via:").append(waypoints.get(i).latitude).append(",").append(waypoints.get(i).longitude);
            }
        }

        builder.append("&destination=").append(dest.latitude).append(",").append(dest.longitude);
        builder.append("&mode=").append("walking");
        builder.append("&key=").append(Config.GOOGLE_MAPS_API_KEY);

        return builder.toString();
    }
}