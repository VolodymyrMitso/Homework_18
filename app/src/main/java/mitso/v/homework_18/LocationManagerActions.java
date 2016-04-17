package mitso.v.homework_18;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;

public class LocationManagerActions {

    public void getCurrentLocation(final Context _context, final LocationManager _locationManager, final GoogleMap _googleMap) {

        final boolean[] done = new boolean[1];

        try {
            /**  Якщо GPS був включений і працював до початку роботи программи,
             *   просто витягую останню локацію. Спрацює швидко. */

            Location currentLocation = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            animateCamera(currentLocation, _googleMap);
            done[0] = true;

            Toast.makeText(_context, R.string.s_last_location, Toast.LENGTH_SHORT).show();

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, new LocationListener() {
            @Override
            public void onLocationChanged(Location _location) {

                /**  Якщо ж ні, і стався NullPointerException, boolean done = false,
                 *   тоді беру поточну локацію, цикл для точності і вихожу.
                 *   Тут прийдеться трохи почекати. */

                if (!done[0]) {
                    try {
                        for (int i = 0; i < 1000; i++)
                            animateCamera(_location, _googleMap);

                        _locationManager.removeUpdates(this);

                        Toast.makeText(_context, R.string.s_current_location , Toast.LENGTH_SHORT).show();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            @Override
            public void onProviderEnabled(String provider) {
            }
            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

    private void animateCamera(Location _location, GoogleMap _googleMap) {

        double latitude = _location.getLatitude();
        double longitude = _location.getLongitude();
        LatLng currentLatLng = new LatLng(latitude, longitude);

        /** zoom = 18, буде видно номери будинків: */

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(currentLatLng, 18);
        _googleMap.animateCamera(cameraUpdate);
    }
}
