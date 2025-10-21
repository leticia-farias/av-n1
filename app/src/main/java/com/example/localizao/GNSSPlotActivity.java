package com.example.localizao;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/**
 * Atividade responsável por configurar e exibir o componente customizado GNSSView
 * e fornecer dados de GNSS Status e Location para ele.
 */
public class GNSSPlotActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_UPDATES = 1;
    private LocationManager locationManager;
    LocationListener locationListener;
    GnssStatus.Callback gnssCallback;
    GNSSView gnssView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnssplot);

        // Obtém o Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Referência para o controle customizado (GNSSView)
        gnssView = findViewById(R.id.GNSSViewid);

        startGnssUpdate();
    }

    public void startGnssUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Escutador de Localização: Repassa a última localização para o GNSSView (necessário para a precisão do Fix)
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    // Repassa a localização para que o GNSSView possa usar a precisão (accuracy)
                    gnssView.newLocation(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) { }
                @Override
                public void onProviderEnabled(@NonNull String provider) { }
                @Override
                public void onProviderDisabled(@NonNull String provider) { }
            };

            // Solicita atualizações de localização para obter o objeto Location
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // 1 segundo
                    0, // 0 metros
                    locationListener);

            // Escutador de Status GNSS: Repassa o status dos satélites
            gnssCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                    super.onSatelliteStatusChanged(status);
                    gnssView.newStatus(status);
                }
            };

            locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.getMainLooper()));

        } else {
            // Solicita a permissão se ainda não foi concedida
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_UPDATES);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_UPDATES) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGnssUpdate();
            } else {
                Toast.makeText(
                        this,
                        "Sem permissão para mostrar informações do sistema GNSS",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void stopGNSSUpdate() {
        if (gnssCallback != null) {
            try {
                locationManager.unregisterGnssStatusCallback(gnssCallback);
            } catch (SecurityException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        if (locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
        if (gnssView != null) {
            gnssView.newStatus(null);
            gnssView.newLocation(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopGNSSUpdate();
    }
}