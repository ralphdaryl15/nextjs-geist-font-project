package com.example.gpsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

public class MainActivity extends Activity implements LocationListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private LocationManager locationManager;
    private TextView txtLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLocation = findViewById(R.id.txtLocation);
        Button btnGetLocation = findViewById(R.id.btnGetLocation);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnGetLocation.setOnClickListener(view -> {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showGPSDisabledAlert();
            } else {
                requestLocationPermission();
            }
        });
    }

    private void showGPSDisabledAlert() {
        new AlertDialog.Builder(this)
            .setTitle("GPS Disabled")
            .setMessage("GPS is disabled. Please enable GPS in settings to obtain location.")
            .setPositiveButton("Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void requestLocationPermission() {
        String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        int fineLocation = checkCallingOrSelfPermission(permissions[0]);
        int coarseLocation = checkCallingOrSelfPermission(permissions[1]);
        
        if (fineLocation != PackageManager.PERMISSION_GRANTED || 
            coarseLocation != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getGPSLocation();
        }
    }

    private void getGPSLocation() {
        try {
            boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                showGPSDisabledAlert();
                return;
            }

            txtLocation.setText("Initializing location services...");

            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 1 second
                    1,    // 1 meter
                    this
                );
                Log.d("Location", "GPS Provider enabled");
            }

            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000, // 1 second
                    1,    // 1 meter
                    this
                );
                Log.d("Location", "Network Provider enabled");
            }

            // Try to get initial location
            Location location = null;
            if (isGPSEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Log.d("Location", "Trying GPS last location");
            }
            
            if (location == null && isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Log.d("Location", "Trying Network last location");
            }

            if (location != null) {
                Log.d("Location", "Initial location found");
                onLocationChanged(location);
            } else {
                txtLocation.setText("Acquiring location...\nPlease wait or move to an open area");
                Toast.makeText(this, "Searching for location signal...", Toast.LENGTH_LONG).show();
                Log.d("Location", "No initial location available");
            }
            
        } catch (SecurityException e) {
            Log.e("Location", "Security Exception: " + e.getMessage());
            e.printStackTrace();
            txtLocation.setText("Error: Location permission required");
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            try {
                locationManager.removeUpdates(this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        final String coordinates = String.format(
            "Latitude: %.6f\nLongitude: %.6f\nAccuracy: %.1f meters\nProvider: %s",
            location.getLatitude(),
            location.getLongitude(),
            location.getAccuracy(),
            location.getProvider()
        );
        
        Log.d("Location", "Location updated: " + coordinates);
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtLocation.setText(coordinates);
                Toast.makeText(MainActivity.this, 
                    "Location Updated (" + location.getProvider() + ")", 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getGPSLocation();
            } else {
                Toast.makeText(this, "Location permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}
}
