package com.psx.pranav.mapsdemo;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Pranav on 02-11-2016.
 */

public class DirectionsParser {
    public List<List<HashMap<String,String>>> parse (JSONObject jsonObject) {

        List<List<HashMap<String,String>>> routes = new ArrayList<List<HashMap<String,String>>>();
        JSONArray jsonArrayRoutes = null;
        JSONArray jsonArrayLegs = null;
        JSONArray jsonArraySteps = null;
        JSONObject jsonObjectDistance = null;
        JSONObject jsonObjectDuration = null;

        try {
            jsonArrayRoutes = jsonObject.getJSONArray("routes");

            // traverse all routes
            for (int j = 0; j < jsonArrayRoutes.length(); j++) {
                jsonArrayLegs = ((JSONObject) jsonArrayRoutes.get(j)).getJSONArray("legs");
                List path = new ArrayList<HashMap<String, String>>();
                // traversing all legs
                for (int k = 0; k < jsonArrayLegs.length(); k++) {
                    jsonArraySteps = ((JSONObject) jsonArrayLegs.get(k)).getJSONArray("steps");
                    // getting diatnce from the JSON Data
                    jsonObjectDistance = ((JSONObject)jsonArrayLegs.get(k)).getJSONObject("distance");
                    HashMap<String,String> hashMapDistance = new HashMap<>();
                    hashMapDistance.put("distance",jsonObjectDistance.getString("text"));

                    // getting the duration from the json data
                    jsonObjectDuration = ((JSONObject)jsonArrayLegs.get(k)).getJSONObject("duration");
                    HashMap<String,String> hashMapDuration = new HashMap<>();
                    hashMapDuration.put("duration",jsonObjectDuration.getString("text"));

                    // add distance object to path
                    path.add(hashMapDistance);
                    //add duration to the path
                    path.add(hashMapDuration);

                    // traversing all steps
                    for (int i = 0; i < jsonArraySteps.length(); i++) {
                        String polyline = "";
                        polyline = (String) ((JSONObject) ((JSONObject) jsonArraySteps.get(i)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        // traversing all points
                        for (int l = 0;l<list.size();l++){
                            HashMap<String,String> hashMap = new HashMap<String, String>();
                            hashMap.put("lat",Double.toString(((LatLng)list.get(l)).latitude));
                            hashMap.put("lng",Double.toString(((LatLng)list.get(l)).longitude));
                            path.add(hashMap);
                        }
                    }
                    routes.add(path);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        return routes;
    }

    // method to decode polylines

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
