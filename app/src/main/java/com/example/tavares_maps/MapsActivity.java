package com.example.tavares_maps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Handler;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private boolean map;
    private boolean locationPermissionGranted;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private boolean isMoving = false;

    private long startTime = 0;
    private boolean isRunning = false;
    private TextView textViewtempo_deslocamento;
    private TextView textViewtempo_chegada;
    ArrayList markerPoints= new ArrayList();

    private String distanceText;
    private int distanceValue;
    private String durationText;
    private int durationValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        //inicializando ass variáveis da tela
        TextView textViewvel_media = findViewById(R.id.vel_media);
        textViewtempo_deslocamento = findViewById(R.id.tempo_deslocamento);
        TextView textViewdist_percorrida = findViewById(R.id.dist_percorrida);
        TextView textViewconsumo_combustivel = findViewById(R.id.consumo_combustivel);
        textViewtempo_chegada = findViewById(R.id.tempo_chegada);
        TextView textViewvel_recomendada = findViewById(R.id.vel_recomendada);
        TextView textViewvel_atual = findViewById(R.id.vel_atual);
        Button buttonbotao_iniciar = findViewById(R.id.botao_iniciar);
        Button buttonbotao_encerrar = findViewById(R.id.botao_encerrar);


        buttonbotao_encerrar.setVisibility(View.INVISIBLE);
        buttonbotao_iniciar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ação a ser executada quando o botão for clicado
                // Por exemplo, exibir uma mensagem de clique
                buttonbotao_iniciar.setVisibility(View.INVISIBLE);
                buttonbotao_encerrar.setVisibility(View.VISIBLE);
                startTimer();
                Toast.makeText(MapsActivity.this, "Botão clicado!", Toast.LENGTH_SHORT).show();
            }
        });

        buttonbotao_encerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ação a ser executada quando o botão for clicado
                buttonbotao_iniciar.setVisibility(View.VISIBLE);
                buttonbotao_encerrar.setVisibility(View.INVISIBLE);
                pauseTimer();
                // Por exemplo, exibir uma mensagem de clique
                Toast.makeText(MapsActivity.this, "Botão clicado!", Toast.LENGTH_SHORT).show();
            }
        });




        //CAÇANDO A VELOCIDADE
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Verifique se o dispositivo está se movendo com base na velocidade
                float speed = location.getSpeed();
                if (speed > 0) {
                    isMoving = true;
                } else {
                    isMoving = false;
                }

                // Atualize a velocidade na tela ou tome outras ações necessárias
                if (isMoving) {
                    float speedKmph = speed * 3.6f; // converta a velocidade para km/h
                    // Faça algo com a velocidade atual
                    textViewvel_media.setText((int) speedKmph);




                }
            }

            // Outros métodos do LocationListener

        };

    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);

        locationManager.removeUpdates(locationListener);
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calcule a aceleração resultante
            float acceleration = (float) Math.sqrt(x * x + y * y + z * z);

            // Verifique se a aceleração indica movimento
            if (acceleration > 1.0f) { // ajuste o valor do limiar conforme necessário
                isMoving = true;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
                /**
                 * Manipulates the map once available.
                 * This callback is triggered when the map is ready to be used.
                 * This is where we can add markers or lines, add listeners or move the camera. In this case,
                 * we just add a marker near Sydney, Australia.
                 * If Google Play services is not installed on the device, the user will be prompted to install
                 * it inside the SupportMapFragment. This method will only be triggered once the user has
                 * installed Google Play services and returned to the app.
                 */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng sydney = new LatLng(- 22.1348, -45.5611);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16));

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (markerPoints.size() > 1) {
                    markerPoints.clear();
                    mMap.clear();
                }

                // Adding new item to the ArrayList
                markerPoints.add(latLng);

                // Creating MarkerOptions
                MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(latLng);

                if (markerPoints.size() == 1) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                } else if (markerPoints.size() == 2) {
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options);

                // Checks, whether start and end locations are captured
                if (markerPoints.size() >= 2) {
                    LatLng origin = (LatLng) markerPoints.get(0);
                    LatLng dest = (LatLng) markerPoints.get(1);

                    // Getting URL to the Google Directions API
                    String url = getDirectionsUrl(origin, dest);

                    DownloadTask downloadTask = new DownloadTask();

                    // Start downloading json data from Google Directions API
                    downloadTask.execute(url);
                }

            }
        });

    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }


    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                CriacaoRota parser = new CriacaoRota();

                JSONArray routesArray = jObject.getJSONArray("routes");
                JSONObject routeObject = routesArray.getJSONObject(0);
                JSONArray legsArray = routeObject.getJSONArray("legs");
                JSONObject legObject = legsArray.getJSONObject(0);
                JSONObject distanceObject = legObject.getJSONObject("distance");
                JSONObject durationObject = legObject.getJSONObject("duration");

                distanceText = distanceObject.getString("text");
                distanceValue  = distanceObject.getInt("value");
                durationText = durationObject.getString("text");
                durationValue  = durationObject.getInt("value");

                textViewtempo_chegada.setText(durationText);


                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

// Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";
        // Building the parameters to the web service
        String key="key=AIzaSyBGgkO6xusRHK_ryDFUu_HvcixyOFhLsEg";
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + '&' + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    private void startTimer() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            isRunning = true;
            runTimer();
        }
    }

    private void pauseTimer() {
        isRunning = false;
    }

    private void runTimer() {
        final long initialTime = startTime;

        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - initialTime;

                    int hours = (int) (elapsedTime / (1000 * 60 * 60));
                    int minutes = (int) ((elapsedTime / (1000 * 60)) % 60);
                    int seconds = (int) ((elapsedTime / 1000) % 60);

                    String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                    textViewtempo_deslocamento.setText(time);

                    // Atualiza a cada 1 segundo
                    textViewtempo_deslocamento.postDelayed(this, 1000);
                }
            }
        };

        timerRunnable.run();
    }


}
