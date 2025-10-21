package com.example.localizao;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF; // Import necessário para o desenho do Zênite
import android.location.GnssStatus;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

public class GNSSView extends View implements View.OnClickListener {
    // Variáveis do Desenho
    private GnssStatus gnssStatus = null; // Satélites do sistema GNSS
    private int r; // Raio da esfera celeste (projeção do horizonte)
    private int height, width; // altura e largura do componente
    private Paint paint = new Paint(); // Pincel para o desenho

    // Constantes do Atributo Customizado
    private static final int ZENITH_DOT = 0;
    private static final int ZENITH_CROSS = 1;
    private static final int ZENITH_CIRCLE = 2;
    private int zenithStyle = ZENITH_DOT; // Estilo padrão

    // Constantes e Variáveis para SharedPreferences e Configurações
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

    private SharedPreferences sharedPrefs;

    public GNSSView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // --- 1. Leitura do Atributo Customizado via XML  ---
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.GNSSView,
                0, 0);

        try {
            // Obtém o valor do atributo 'zenithMarkerStyle'. O valor padrão é ZENITH_DOT (0).
            zenithStyle = a.getInt(R.styleable.GNSSView_zenithMarkerStyle, ZENITH_DOT);
        } finally {
            a.recycle();
        }

        // --- 2. Configuração de Persistência  ---
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        loadConfiguration();

        // --- 3. Configura o componente para responder a cliques  ---
        this.setOnClickListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

        // Raio da esfera (usando um fator de 0.9 do menor lado)
        if (width < height)
            r = (int) (width / 2 * 0.9);
        else
            r = (int) (height / 2 * 0.9);
    }

    // --- Métodos de Persistência e Configuração ---
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

        invalidate(); // Força o redesenho [cite: 5]
    }

    // Método chamado pelo GNSSPlotActivity para atualizar os dados [cite: 5]
    public void newStatus(GnssStatus gnssStatus) {
        this.gnssStatus = gnssStatus;
        invalidate();
    }

    private int computeXc(double x) {
        return (int) (x+width/2);
    }

    private int computeYc(double y) {
        // Zênite (0,0) deve estar no centro e o Norte (azimute 0) para cima [cite: 4]
        return (int) (-y+height/2);
    }

    // --- Lógica do Quadro de Diálogo  ---
    @Override
    public void onClick(View v) {
        showConfigurationDialog();
    }

    private void showConfigurationDialog() {
        // Layout principal do diálogo (vertical)
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        // CheckBoxes para as constelações
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

        // CheckBox para satélites não usados no FIX
        final CheckBox unusedCb = new CheckBox(getContext());
        unusedCb.setText("Mostrar satélites não usados no FIX");
        unusedCb.setChecked(showUnused);

        layout.addView(gpsCb);
        layout.addView(glonassCb);
        layout.addView(galileoCb);
        layout.addView(beidouCb);
        layout.addView(unusedCb);

        // Cria e exibe o diálogo
        new AlertDialog.Builder(getContext())
                .setTitle("Configuração de Visualização GNSS")
                .setView(layout)
                .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Salva as novas configurações
                        saveConfiguration(
                                gpsCb.isChecked(),
                                glonassCb.isChecked(),
                                galileoCb.isChecked(),
                                beidouCb.isChecked(),
                                unusedCb.isChecked()
                        );
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // --- Lógica de Desenho Principal (onDraw)  ---
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int visibleCount = 0;
        int usedCount = 0;
        int centerX = computeXc(0);
        int centerY = computeYc(0);

        // --- 1. Desenho da Esfera Celeste e Eixos [cite: 4] ---
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.BLUE);

        // Círculos de Elevação (Horizonte, 30°, 60°)
        canvas.drawCircle(centerX, centerY, r, paint); // 0º Elevação (Horizonte)
        canvas.drawCircle(centerX, centerY, r * 2 / 3, paint); // 30º Elevação
        canvas.drawCircle(centerX, centerY, r * 1 / 3, paint); // 60º Elevação

        // Eixos (Norte, Sul, Leste, Oeste)
        canvas.drawLine(centerX, centerY - r, centerX, centerY + r, paint); // N-S
        canvas.drawLine(centerX - r, centerY, centerX + r, centerY, paint); // L-O

        // Desenha as marcações N, S, L, O (Texto)
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(30);
        paint.setColor(Color.WHITE);

        canvas.drawText("N", centerX, centerY - r - 10, paint);
        canvas.drawText("S", centerX, centerY + r + 40, paint);
        canvas.drawText("L", centerX + r + 10, centerY + 10, paint);
        canvas.drawText("O", centerX - r - 40, centerY + 10, paint);

        // Desenho do Marcador de Zênite (Centro)
        paint.setColor(Color.RED);
        int markerSize = 15;

        if (zenithStyle == ZENITH_DOT) {
            canvas.drawCircle(centerX, centerY, markerSize, paint);
        } else if (zenithStyle == ZENITH_CROSS) {
            paint.setStrokeWidth(8);
            canvas.drawLine(centerX - markerSize, centerY - markerSize, centerX + markerSize, centerY + markerSize, paint);
            canvas.drawLine(centerX - markerSize, centerY + markerSize, centerX + markerSize, centerY - markerSize, paint);
            paint.setStrokeWidth(5); // Restaura o stroke width
        } else if (zenithStyle == ZENITH_CIRCLE) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);
            canvas.drawCircle(centerX, centerY, markerSize, paint);
            paint.setStrokeWidth(5); // Restaura o stroke width
        }

        paint.setStyle(Paint.Style.FILL);


        // --- 2. Desenho e Filtragem dos Satélites [cite: 6, 7, 8] ---
        if (gnssStatus != null) {
            int count = gnssStatus.getSatelliteCount();

            for (int i = 0; i < count; i++) {
                int constellation = gnssStatus.getConstellationType(i);
                boolean usedInFix = gnssStatus.usedInFix(i);

                // --- Lógica de Filtragem ---
                boolean passesConstellationFilter = false;
                if (constellation == GnssStatus.CONSTELLATION_GPS && showGPS) {
                    passesConstellationFilter = true;
                } else if (constellation == GnssStatus.CONSTELLATION_GLONASS && showGLONASS) {
                    passesConstellationFilter = true;
                } else if (constellation == GnssStatus.CONSTELLATION_GALILEO && showGALILEO) {
                    passesConstellationFilter = true;
                } else if (constellation == GnssStatus.CONSTELLATION_BEIDOU && showBEIDOU) {
                    passesConstellationFilter = true;
                } else if (constellation == GnssStatus.CONSTELLATION_QZSS || constellation == GnssStatus.CONSTELLATION_SBAS || constellation == GnssStatus.CONSTELLATION_UNKNOWN) {
                    passesConstellationFilter = true;
                }

                boolean passesUsedFilter = showUnused || usedInFix;

                if (!passesConstellationFilter || !passesUsedFilter) {
                    continue; // Pula o satélite se não passar nos filtros
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

                // --- Diferenciação Visual (Constelação e Fix) [cite: 6, 7, 8] ---

                // 1. Cor pela Constelação
                int satColor;
                if (constellation == GnssStatus.CONSTELLATION_GPS) {
                    satColor = Color.GREEN;
                } else if (constellation == GnssStatus.CONSTELLATION_GLONASS) {
                    satColor = Color.YELLOW;
                } else if (constellation == GnssStatus.CONSTELLATION_GALILEO) {
                    satColor = Color.CYAN;
                } else if (constellation == GnssStatus.CONSTELLATION_BEIDOU) {
                    satColor = Color.MAGENTA;
                } else {
                    satColor = Color.LTGRAY;
                }
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

                // 3. Desenha o ID do satélite (Texto) [cite: 6]
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(25);
                String satID = gnssStatus.getSvid(i) + "";
                canvas.drawText(satID, computeXc(x) + dotSize + 5, computeYc(y) + dotSize / 2, paint);
            }
        }

        // --- 3. Desenho do Texto de Status (Contagens) [cite: 9] ---
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.LEFT);

        String statusText1 = "Satélites Visíveis: " + visibleCount;
        String statusText2 = "Satélites em Uso (Fix): " + usedCount;

        // Desenha o texto de status no topo
        canvas.drawText(statusText1, 10, 50, paint);
        canvas.drawText(statusText2, 10, 100, paint);
    }
}