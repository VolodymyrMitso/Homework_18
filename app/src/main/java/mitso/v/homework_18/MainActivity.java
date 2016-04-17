package mitso.v.homework_18;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends AbstractMapActivity {

    public static final String SAVED_LIST_KEY = "SAVED_LIST_KEY";

    private GoogleMap           mGoogleMap;
    private LocationManager     mLocationManager;
    private ArrayList<Marker>   mMarkerList;

    private Circle              mCircle;
    private Polyline            mPolyline;
    private Polygon             mPolygon;

    private int                 markerPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isGoogleMapsAvailable())
            setContentView(R.layout.activity_main);
        else {
            finish();
        }

        ((MapFragment) getFragmentManager().findFragmentById(R.id.google_map)).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap _googleMap) {
                mGoogleMap = _googleMap;
                initMap();

                mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                /**  Перевіряю чи включений GPS. Якщо ні, пропоную включити.
                 *   Якщо виключений, программа не впаде, можна буде ставити маркери,
                 *   але показати поточну локацію не вийде, поки не буде включений. */

                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    Toast.makeText(MainActivity.this, R.string.s_gps_enabled, Toast.LENGTH_SHORT).show();
                else
                    showGPSDisabledAlertDialog();

                new LocationManagerActions().getCurrentLocation(MainActivity.this, mLocationManager, mGoogleMap);

                mMarkerList = new ArrayList<>();

                /**  У мене тут, в onCreate в onMapReady, і ініціалізація змінних і метод getCurrentLocation
                 *   і загрузка списку з SharedPreferences. Якщо ініціалізувати змінні і визивати метод
                 *   getCurrentLocation в onCreate, але не в onMapReady, а загрузку списку з SharedPreferences
                 *   робити в onResume, то буде багато нестиковок, щось раніше завантажується, щось пізніше. */

                loadMarkerList();
            }
        });
    }

    private void initMap() {

        /**  Налаштовую map. Current location button не ставлю. */

        mGoogleMap.getUiSettings().setCompassEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.getUiSettings().setIndoorLevelPickerEnabled(true);

        mGoogleMap.getUiSettings().setAllGesturesEnabled(true);

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        MapListeners listeners = new MapListeners();
        mGoogleMap.setOnMapLongClickListener(listeners);
        mGoogleMap.setOnMarkerDragListener(listeners);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mi_ClearMap:

                /**  Тут все чищу. Роблю і remove() для фігур і clear() для map.
                 *   І то і то для надійності, бо не завжди спрацьовувало. */

                if (mCircle != null)
                    mCircle.remove();

                if (mPolyline != null)
                    mPolyline.remove();

                if (mPolygon != null)
                    mPolygon.remove();

                for (Marker marker : mMarkerList)
                    marker.remove();
                mMarkerList.clear();

                mGoogleMap.clear();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class MapListeners implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMarkerDragListener {

        @Override
        public void onMapLongClick(LatLng latLng) {

            /**  Створюю і налаштовую маркер, додаю на карту, додаю в список, малюю, веду камеру. */

            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.draggable(true);
            markerOptions.title(latLng.toString());
            markerOptions.snippet(String.valueOf(mMarkerList.size() + 1));
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(new Random().nextInt(360)));

            mMarkerList.add(mGoogleMap.addMarker(markerOptions));

            drawMapFigures();

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            mGoogleMap.animateCamera(cameraUpdate);
        }

        @Override
        public void onMarkerDragStart(Marker _marker) {

            /**  Запам'ятовую позийію маркера в списку, щоб потім додати на правильне місце.
             *   Видаляю маркер. */

            for (int i = 0; i < mMarkerList.size(); i++)
                if (mMarkerList.get(i).getPosition().toString().equals(_marker.getPosition().toString()))
                    markerPosition = i;

            mMarkerList.remove(_marker);
        }

        @Override
        public void onMarkerDrag(Marker marker) { }

        @Override
        public void onMarkerDragEnd(Marker _marker) {

            /**  Додаю маркер, малюю, веду камеру. */

            mMarkerList.add(markerPosition, _marker);

            drawMapFigures();

            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(_marker.getPosition(), 15);
            mGoogleMap.animateCamera(cameraUpdate);
        }
    }

    private void drawMapFigures() {

        /**  Метод для малювання фігур на карті з маркерів з списку. */

        double radius = 200;
        int fillColor = getResources().getColor(R.color.c_fill);
        int strokeColor = getResources().getColor(R.color.c_stroke);
        int lineColor = getResources().getColor(R.color.c_line);

        if (mMarkerList.size() == 1) {

            if (mCircle != null)
                mCircle.remove();

            CircleOptions circleOptions = new CircleOptions().center(mMarkerList.get(0).getPosition()).radius(radius).fillColor(fillColor).strokeColor(strokeColor).strokeWidth(5);
            mCircle = mGoogleMap.addCircle(circleOptions);

        } else if (mMarkerList.size() == 3) {

            if (mPolygon != null)
                mPolygon.remove();

            PolygonOptions polygonOptions = new PolygonOptions().fillColor(fillColor);
            for (Marker marker : mMarkerList) {
                polygonOptions.add(marker.getPosition());
            }

            mPolygon = mGoogleMap.addPolygon(polygonOptions);

        } else if (mMarkerList.size() == 4) {

            if (mPolygon != null)
                mPolygon.remove();

            PolygonOptions polygonOptions = new PolygonOptions().fillColor(fillColor);
            for (Marker marker : mMarkerList) {
                polygonOptions.add(marker.getPosition());
            }

            mPolygon = mGoogleMap.addPolygon(polygonOptions);

        } else if (mMarkerList.size() > 4) {

            if (mPolyline != null)
                mPolyline.remove();

            PolylineOptions polylineOptions = new PolylineOptions().width(5).color(lineColor).geodesic(true);
            for (Marker marker : mMarkerList) {
                polylineOptions.add(marker.getPosition());
            }

            mPolyline = mGoogleMap.addPolyline(polylineOptions);
        }

        if (mMarkerList.size() > 1) {
            if (mCircle != null)
                mCircle.remove();
        }

        if (mMarkerList.size() > 4) {
            if (mPolygon != null)
                mPolygon.remove();
        }
    }

    private void showGPSDisabledAlertDialog() {

        /**  Діалог GPS. */

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(R.string.s_gps_disabled)
                .setCancelable(false)
                .setPositiveButton("Go to SETTINGS page to enable GPS", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(callGPSSettingIntent);
                    }
                })
                .setNegativeButton(R.string.s_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create();
        alertDialogBuilder.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        /**  Зберігаю список. */

        saveMarkerList();
    }

    private void saveMarkerList() {
        new AsyncTask<Void, Void, Void>() {

            SharedPreferences sharedPreferences;
            ArrayList<Double> latLngList;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                /**  Тут готую все для збереження.
                 *   Вибрав ArrayList<Double>, щоб займав чим менше місця. */

                sharedPreferences = getPreferences(MODE_PRIVATE);

                latLngList = new ArrayList<>();
                for (int i = 0; i < mMarkerList.size(); i++) {
                    latLngList.add(mMarkerList.get(i).getPosition().latitude);
                    latLngList.add(mMarkerList.get(i).getPosition().longitude);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {

                /**  В іншому потоці тільки збереження. */

                SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                String listString = new Gson().toJson(latLngList);
                editor.putString(SAVED_LIST_KEY, listString);
                editor.apply();

                return null;
            }
        }.execute();
    }

    private void loadMarkerList() {
        new AsyncTask<Void, Void, Void>() {

            SharedPreferences sharedPreferences;
            List<Double> latLngList;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                sharedPreferences = getPreferences(MODE_PRIVATE);
                latLngList = new ArrayList<>();
            }

            @Override
            protected Void doInBackground(Void... params) {

                /**  В іншому потоці тільки завантаження. */

                if (sharedPreferences.contains(SAVED_LIST_KEY)) {
                    String jsonFavorites = sharedPreferences.getString(SAVED_LIST_KEY, null);
                    Double[] personsArray = new Gson().fromJson(jsonFavorites, Double[].class);
                    latLngList = Arrays.asList(personsArray);
                    latLngList = new ArrayList<>(latLngList);
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                /**  Коли ArrayList<Double> готовий, в головному потоці
                 *   створюю і налаштовую маркери, додаю на карту, додаю в список, малюю.
                 *   Камеру не веду, при поворотах екрана і нових завантаженнях программи
                 *   камера буде показувати поточну локацію. */

                if (!latLngList.isEmpty()) {

                    mMarkerList.clear();

                    for (int i = 0; i < latLngList.size(); i = i + 2) {

                        LatLng latLng = new LatLng(latLngList.get(i), latLngList.get(i + 1));

                        MarkerOptions markerOptions = new MarkerOptions();
                        markerOptions.position(latLng);
                        markerOptions.draggable(true);
                        markerOptions.title(latLng.toString());
                        markerOptions.snippet(String.valueOf(i));
                        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(new Random().nextInt(360)));

                        mMarkerList.add(mGoogleMap.addMarker(markerOptions));

                        drawMapFigures();
                    }
                }
            }
        }.execute();
    }
}
