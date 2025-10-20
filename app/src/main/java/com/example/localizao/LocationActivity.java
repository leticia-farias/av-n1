package com.example.localizao;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_UPDATES = 1; // Request code para o gerenciamento de permissões
    // Objetos da API de Localização
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        Button btnStart = findViewById(R.id.buttonStart);
        Button btnStop = findViewById(R.id.buttonStop);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocationUpdate();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationUpdate();
            }
        });
    }
    private void startLocationUpdate() {
        // Se a app já possui a permissão, ativa a chamada de localização
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            // A permissão foi dada– OK vá em frente

            // Cria o cliente (FusedLocationProviderClient)
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            // Configura a solicitação de localizações (LocationRequest)
            long timeInterval = 5 * 1000;
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, timeInterval).build();

            // Programa o escutador para consumir as novas localizações geradas (LocationCallback)
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    Location location = locationResult.getLastLocation();
                    // Processa a localização aqui
                    atualizaLocationTextView(location);
                }
            };

            // Manda o cliente começar a gerar atualizações de localização.
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);

        } else {
            // Solicite a permissão
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_UPDATES);
        }
    }

    private void stopLocationUpdate() {
        if (fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        atualizaLocationTextView(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_UPDATES) {

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // O usuário acabou de dar a permissão
                startLocationUpdate();
            } else {
                // O usuário não deu a permissão solicitada
                Toast.makeText(this, "Sem permissão para mostrar atualizações da sua localização",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void atualizaLocationTextView (Location location) {
        TextView locationTextView = (TextView) findViewById(R.id.LocationTextView);
        String s = "Dados da Última Localização:\n";
        if (location != null) {
            s += "Latitude: (G/M/S)" + Location.convert(location.getLatitude(),Location.FORMAT_SECONDS) + "\n";
            s += "Longitude: (G/M/S) " + Location.convert(location.getLongitude(),Location.FORMAT_SECONDS) + "\n";
            s += "Altitude: " + location.getAltitude() + "\n";
            s += "Rumo: (radianos)" + location.getBearing() + "\n";
            s += "Velocidade (m/s): " + location.getSpeed() + "\n";
            s += "Precisão: (m)" + location.getAccuracy() + "\n";
        }
        locationTextView.setText(s);
    }
}
