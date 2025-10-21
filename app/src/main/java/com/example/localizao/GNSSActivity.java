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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GNSSActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_UPDATES = 1;
    private LocationManager locationManager;
    LocationListener locationListener;
    GnssStatus.Callback gnssCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gnssactivity);
        // Obtém o Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Button btnStartGNSS = findViewById(R.id.buttonStartGNSS);
        Button btnStopGNSS = findViewById(R.id.buttonStopGNSS);

        btnStartGNSS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGnssStatus();
            }
        });

        btnStopGNSS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopGnssUpdate();
            }
        });
    }

    public void startGnssStatus() {
        // Se o app já possui a permissão, ativa a chamada para atualização
        if (ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            // A permissão foi dada– OK vá em frente
            // Objeto instância de uma classe anônima que implementa a interface LocationListener
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    atualizaLocationTextView(location); // Processa nova localização
                }
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(@NonNull String provider) {}
                @Override
                public void onProviderDisabled(@NonNull String provider) {}
            };
            // Informa o provedor de localização, tempo e distância mínimos e o escutador
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, // provedor de localização
                    5*1000, // intervalo mínimo (ms)
                    0, // distância mínima (m)
                    locationListener); // objeto que irá processar as localizações

            // Objeto instância de uma classe anônima que implementa GnssStatus.Callback
            gnssCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                    super.onSatelliteStatusChanged(status);
                    // Processa as informações do sistema de satélite
                    atualizaGNSSTextView(status);
                }
            };

            // Informa o escutador do sistema de satélities e a thread para processar essas infos
            locationManager.registerGnssStatusCallback(gnssCallback, new Handler(Looper.getMainLooper()));

        } else {
            // Solicite a permissão
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_UPDATES);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_UPDATES) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // O usuário acabou de dar a permissão
                startGnssStatus();
            }
        } else {
            // O usuário não deu a permissão solicitada
            Toast.makeText(this, "Sem permissão para mostrar informações do sistema GNSS",
                Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void atualizaLocationTextView(Location location) {
        TextView locationTextView = (TextView) findViewById(R.id.textViewLocationManager);
        if (location == null) {
            String s = "Dados de Localização não disponíveis";
            locationTextView.setText(s);
            return;
        }
        String s = "Dados da Última Localização:\n";
        if (location != null) {
            s += "Latitude: " + location.getLatitude() + "\n";
            s += "Longitude: " + location.getLongitude() + "\n";
            s += "Altitude: " + location.getAltitude() + "\n";
            s += "Rumo: (radianos)" + location.getBearing() + "\n";
            s += "Velocidade (m/s): " + location.getSpeed() + "\n";
            s += "Precisão: (m)" + location.getAccuracy() + "\n";
        }
        locationTextView.setText(s);
    }

    private void atualizaGNSSTextView(GnssStatus status) {
        TextView textViewGNSS = (TextView) findViewById(R.id.textViewGNSS);
        if (status == null) {
            String s = "Sistema de Satélite não disponível";
            textViewGNSS.setText(s);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int count = status.getSatelliteCount();
        sb.append("Satélites visíveis: ").append(count).append("\n");
        for (int i = 0; i < count; i++) {
            int svid = status.getSvid(i); // ID do satélite
            float azimuth = status.getAzimuthDegrees(i); // Azimute (0º = Norte, 90º = Leste)
            float elevation = status.getElevationDegrees(i); // Elevação (0º = Horizonte, 90º = Zênite)
            boolean used = status.usedInFix(i);
            sb.append("SVID: ").append(svid)
                    .append(" | Azimute: ").append(azimuth).append("º")
                    .append(" | Elevação: ").append(elevation).append("º")
                    .append(" | Usado no fix: ").append(used)
                    .append("\n");
        }
        textViewGNSS.setText(sb.toString());
    }

    public void stopGnssUpdate() {
        // desliga a GnssCallback
        if (gnssCallback != null) {
            try {
                locationManager.unregisterGnssStatusCallback(gnssCallback);
            } catch (Exception e) {
                e.printStackTrace(); // evita crash se já tiver sido desregistrado
            }
        }
        // desliga a LocationListener
        if (locationListener != null) {
                locationManager.removeUpdates(locationListener);
        }
        atualizaGNSSTextView(null);
        atualizaLocationTextView(null);
    }
}