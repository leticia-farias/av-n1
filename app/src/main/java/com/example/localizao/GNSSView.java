package com.example.localizao;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path; // Necessário para desenhar a estrela
import android.location.GnssStatus;
import android.location.Location; // Necessário para obter a precisão do Fix
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

/**
 * Componente customizado para desenhar a projeção da esfera celeste e satélites GNSS.
 * Implementa filtros de constelação e um marcador de Zênite customizável.
 */
public class GNSSView extends View implements View.OnClickListener {
    // Variáveis do Desenho
    private GnssStatus gnssStatus = null;
    private Location lastLocation = null; // Última localização para obter a precisão (accuracy)
    private int r; // Raio da esfera celeste (projeção do horizonte)
    private int height, width;
    private Paint paint = new Paint(); // Pincel de uso geral

    // Constantes do Atributo Customizado e Estado
    private static final int ZENITH_CIRCLE = 0;
    private static final int ZENITH_CROSS = 1;
    private static final int ZENITH_STAR = 2;
    private int zenithStyle = ZENITH_CIRCLE;

    // Constantes e Variáveis para Configurações (SharedPreferences)
    private SharedPreferences sharedPrefs;
    public static final String PREF_GPS = "pref_gps";
    public static final String PREF_GLONASS = "pref_glonass";
    public static final String PREF_GALILEO = "pref_galileo";
    public static final String PREF_BEIDOU = "pref_beidou";
    public static final String PREF_SHOW_UNUSED = "pref_show_unused";

    private boolean showGPS = true;
    private boolean showGLONASS = true;
    private boolean showGALILEO = true;
    private boolean showBEIDOU = true;
    private boolean showUnused = true;

    public GNSSView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // --- 1. Leitura do Atributo Customizado via XML (zenithMarkerStyle) ---
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.GNSSView,
                0, 0);

        try {
            // O nome do styleable no XML é GNSSView, os valores são 0, 1, 2
            zenithStyle = a.getInt(R.styleable.GNSSView_zenithMarkerStyle, ZENITH_CIRCLE);
        } finally {
            a.recycle();
        }

        // --- 2. Configuração de Persistência ---
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        loadConfiguration();

        // --- 3. Configura o componente para responder a cliques (para o diálogo) ---
        this.setOnClickListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        // Raio da esfera (0.9 do menor lado)
        if (width < height)
            r = (int) (width / 2 * 0.9);
        else
            r = (int) (height / 2 * 0.9);
    }

    // --- Métodos de Atualização de Dados ---
    public void newStatus(GnssStatus gnssStatus) {
        this.gnssStatus = gnssStatus;
        invalidate(); // Redesenha a tela
    }

    public void newLocation(Location location) {
        this.lastLocation = location;
        invalidate(); // Redesenha a tela
    }

    // --- Métodos Auxiliares de Coordenadas ---
    private int computeXc(double x) { return (int) (x + width / 2); }
    private int computeYc(double y) { return (int) (-y + height / 2); }

    // --- Lógica de Persistência e Configuração ---
    private void loadConfiguration() {
        showGPS = sharedPrefs.getBoolean(PREF_GPS, true);
        showGLONASS = sharedPrefs.getBoolean(PREF_GLONASS, true);
        showGALILEO = sharedPrefs.getBoolean(PREF_GALILEO, true);
        showBEIDOU = sharedPrefs.getBoolean(PREF_BEIDOU, true);
        showUnused = sharedPrefs.getBoolean(PREF_SHOW_UNUSED, true);
    }

    public void saveConfiguration(boolean gps, boolean glonass, boolean galileo, boolean beidou, boolean showUnusedSatellites) {
        SharedPreferences.Editor editor = sharedPrefs.edit();

        this.showGPS = gps;
        editor.putBoolean(PREF_GPS, gps);
        this.showGLONASS = glonass;
        editor.putBoolean(PREF_GLONASS, glonass);
        this.showGALILEO = galileo;
        editor.putBoolean(PREF_GALILEO, galileo);
        this.showBEIDOU = beidou;
        editor.putBoolean(PREF_BEIDOU, beidou);
        this.showUnused = showUnusedSatellites;
        editor.putBoolean(PREF_SHOW_UNUSED, showUnusedSatellites);

        editor.apply();
        invalidate();
    }

    // --- Lógica do Quadro de Diálogo ---
    @Override
    public void onClick(View v) {
        showConfigurationDialog();
    }

    private void showConfigurationDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        final CheckBox gpsCb = new CheckBox(getContext());
        gpsCb.setText("GPS");
        gpsCb.setChecked(showGPS);

        final CheckBox glonassCb = new CheckBox(getContext());
        glonassCb.setText("GLONASS");
        glonassCb.setChecked(showGLONASS);

        final CheckBox galileoCb = new CheckBox(getContext());
        galileoCb.setText("GALILEO");
        galileoCb.setChecked(showGALILEO);

        final CheckBox beidouCb = new CheckBox(getContext());
        beidouCb.setText("BEIDOU");
        beidouCb.setChecked(showBEIDOU);

        final CheckBox unusedCb = new CheckBox(getContext());
        unusedCb.setText("Mostrar satélites não usados no FIX");
        unusedCb.setChecked(showUnused);

        layout.addView(gpsCb);
        layout.addView(glonassCb);
        layout.addView(galileoCb);
        layout.addView(beidouCb);
        layout.addView(unusedCb);

        new AlertDialog.Builder(getContext())
                .setTitle("Configuração de Visualização GNSS")
                .setView(layout)
                .setPositiveButton("Salvar", (dialog, which) -> saveConfiguration(
                        gpsCb.isChecked(),
                        glonassCb.isChecked(),
                        galileoCb.isChecked(),
                        beidouCb.isChecked(),
                        unusedCb.isChecked()
                ))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- Lógica de Desenho Principal (onDraw) ---
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int visibleCount = 0;
        int usedCount = 0;
        int cx = computeXc(0);
        int cy = computeYc(0);

        // --- 1. Desenho da Esfera Celeste e Eixos ---
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.BLUE);

        // Círculos de Elevação (Horizonte, 30°, 60°)
        canvas.drawCircle(cx, cy, r, paint); // 0º Elevação (Horizonte)
        canvas.drawCircle(cx, cy, r * 2 / 3, paint); // 30º Elevação
        canvas.drawCircle(cx, cy, r * 1 / 3, paint); // 60º Elevação

        // Eixos e marcações N, S, L, O
        canvas.drawLine(cx, cy - r, cx, cy + r, paint);
        canvas.drawLine(cx - r, cy, cx + r, cy, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(30);
        paint.setColor(Color.WHITE);

        canvas.drawText("N", cx, cy - r - 10, paint);
        canvas.drawText("S", cx, cy + r + 40, paint);
        canvas.drawText("L", cx + r + 10, cy + 10, paint);
        canvas.drawText("O", cx - r - 40, cy + 10, paint);

        // --- Desenho do Marcador de Zênite (Centro) com Precisão do Fix ---

        // 1. Determina a cor do marcador com base na precisão da última localização
        if (lastLocation != null && lastLocation.hasAccuracy()) {
            float accuracy = lastLocation.getAccuracy();
            if (accuracy < 5) { // Boa precisão (menos de 5m)
                paint.setColor(Color.GREEN);
            } else if (accuracy < 20) { // Precisão média (entre 5m e 20m)
                paint.setColor(Color.YELLOW);
            } else { // Baixa precisão (mais de 20m)
                paint.setColor(Color.RED);
            }
        } else {
            // Sem dados de precisão, usa cinza
            paint.setColor(Color.DKGRAY);
        }

        // 2. Desenha o estilo do marcador
        int markerSize = 15;

        // Salva o pincel antes de modificar o stroke/style
        Paint usedPaint = new Paint(paint);

        switch (zenithStyle) {
            case ZENITH_CIRCLE: // circle
                usedPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, 10, usedPaint);
                break;
            case ZENITH_CROSS: // cross
                usedPaint.setStrokeWidth(8);
                canvas.drawLine(cx - 10, cy, cx + 10, cy, usedPaint);
                canvas.drawLine(cx, cy - 10, cx, cy + 10, usedPaint);
                break;
            case ZENITH_STAR: // star
                // Lógica complexa para desenhar uma estrela (pentagrama)
                usedPaint.setStyle(Paint.Style.FILL);
                Path star = new Path();
                for (int i = 0; i < 5; i++) {
                    // Ângulo para os pontos externos e internos da estrela de 5 pontas
                    double angle = Math.toRadians(i * 72 - 90);
                    double inner = Math.toRadians(i * 72 + 36 - 90);

                    float xOuter = (float) (cx + 12 * Math.cos(angle));
                    float yOuter = (float) (cy + 12 * Math.sin(angle));
                    float xInner = (float) (cx + 5 * Math.cos(inner));
                    float yInner = (float) (cy + 5 * Math.sin(inner));

                    if (i == 0) star.moveTo(xOuter, yOuter);
                    star.lineTo(xInner, yInner);
                    star.lineTo(xOuter, yOuter);
                }
                star.close();
                canvas.drawPath(star, usedPaint);
                break;
        }

        // --- 3. Desenho e Filtragem dos Satélites ---
        if (gnssStatus != null) {
            int count = gnssStatus.getSatelliteCount();

            for (int i = 0; i < count; i++) {
                int constellation = gnssStatus.getConstellationType(i);
                boolean usedInFix = gnssStatus.usedInFix(i);

                // Aplica Filtros
                boolean passesConstellationFilter = (constellation == GnssStatus.CONSTELLATION_GPS && showGPS) ||
                        (constellation == GnssStatus.CONSTELLATION_GLONASS && showGLONASS) ||
                        (constellation == GnssStatus.CONSTELLATION_GALILEO && showGALILEO) ||
                        (constellation == GnssStatus.CONSTELLATION_BEIDOU && showBEIDOU) ||
                        (constellation == GnssStatus.CONSTELLATION_QZSS || constellation == GnssStatus.CONSTELLATION_SBAS || constellation == GnssStatus.CONSTELLATION_UNKNOWN);

                boolean passesUsedFilter = showUnused || usedInFix;

                if (!passesConstellationFilter || !passesUsedFilter) {
                    continue;
                }

                // Contagem
                visibleCount++;
                if (usedInFix) {
                    usedCount++;
                }

                float az = gnssStatus.getAzimuthDegrees(i);
                float el = gnssStatus.getElevationDegrees(i);

                // Coordenadas (Projeção Azimutal Equidistante)
                float dz = 90f - el;
                float rho = r * dz / 90f;
                float x = (float) (rho * Math.sin(Math.toRadians(az)));
                float y = (float) (rho * Math.cos(Math.toRadians(az)));

                // 1. Cor pela Constelação
                int satColor;
                if (constellation == GnssStatus.CONSTELLATION_GPS) satColor = Color.parseColor("#013220");
                else if (constellation == GnssStatus.CONSTELLATION_GLONASS) satColor = Color.YELLOW;
                else if (constellation == GnssStatus.CONSTELLATION_GALILEO) satColor = Color.CYAN;
                else if (constellation == GnssStatus.CONSTELLATION_BEIDOU) satColor = Color.MAGENTA;
                else satColor = Color.LTGRAY;
                paint.setColor(satColor);

                // 2. Forma/Estilo pelo 'Used in Fix'
                int dotSize = 12;
                if (usedInFix) {
                    // Círculo preenchido = Usado no Fix
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(computeXc(x), computeYc(y), dotSize, paint);
                } else {
                    // Círculo vazado = Não usado no Fix
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(3);
                    canvas.drawCircle(computeXc(x), computeYc(y), dotSize, paint);
                    paint.setStyle(Paint.Style.FILL);
                }

                // 3. Desenha o ID do satélite (Texto)
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(25);
                String satID = gnssStatus.getSvid(i) + "";
                canvas.drawText(satID, computeXc(x) + dotSize + 5, computeYc(y) + dotSize / 2, paint);
            }
        }

        // --- 4. Desenho do Texto de Status (Contagens) ---
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.LEFT);

        String statusText1 = "Satélites Visíveis: " + visibleCount;
        String statusText2 = "Satélites em Uso (Fix): " + usedCount;

        canvas.drawText(statusText1, 10, 50, paint);
        canvas.drawText(statusText2, 10, 100, paint);
    }
}