package com.example.localizao;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.location.GnssStatus;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // Import necessário para ContextCompat

/**
 * Componente customizado para desenhar a projeção da esfera celeste e satélites GNSS,
 * utilizando logos (Drawable) para identificação da constelação.
 */
public class GNSSView extends View implements View.OnClickListener {
    // Variáveis de Estado de Dados
    private GnssStatus gnssStatus = null;
    private Location lastLocation = null;

    // Variáveis de Desenho e Dimensões
    private int r;
    private int height, width;
    private Paint paint = new Paint();

    // Constantes do Atributo Customizado Zênite
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

    // --- VARIÁVEIS PARA LOGOS (DRAWABLES) ---
    private Drawable gpsDrawable;
    private Drawable glonassDrawable;
    private Drawable galileoDrawable;
    private Drawable beidouDrawable;

    // Constantes de Cor para Tintura (ColorFilter)
    private static final int COLOR_GPS_BORDER = Color.parseColor("#4CAF50"); // Verde mais escuro
    private static final int COLOR_GLONASS_BORDER = Color.parseColor("#FFC107"); // Amarelo
    private static final int COLOR_GALILEO_BORDER = Color.parseColor("#03A9F4"); // Azul
    private static final int COLOR_BEIDOU_BORDER = Color.parseColor("#FF5722"); // Laranja

    public GNSSView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        // --- 1. Leitura do Atributo Customizado via XML ---
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.GNSSView, 0, 0);

        try {
            zenithStyle = a.getInt(R.styleable.GNSSView_zenithMarkerStyle, ZENITH_CIRCLE);
        } finally {
            a.recycle();
        }

        // --- 2. Carregamento dos Drawables (Logos) ---
        gpsDrawable = ContextCompat.getDrawable(context, R.drawable.ic_logo_gps);
        glonassDrawable = ContextCompat.getDrawable(context, R.drawable.ic_logo_glonass);
        galileoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_logo_galileo);
        beidouDrawable = ContextCompat.getDrawable(context, R.drawable.ic_logo_beidou);

        // --- 3. Configuração de Persistência ---
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        loadConfiguration();

        // --- 4. Configura o componente para responder a cliques ---
        this.setOnClickListener(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        if (width < height)
            r = (int) (width / 2 * 0.9);
        else
            r = (int) (height / 2 * 0.9);
    }

    // --- Métodos de Atualização de Dados (Chamados pela Activity) ---
    public void newStatus(GnssStatus gnssStatus) { this.gnssStatus = gnssStatus; invalidate(); }
    public void newLocation(Location location) { this.lastLocation = location; invalidate(); }

    private int computeXc(double x) { return (int) (x + width / 2); }
    private int computeYc(double y) { return (int) (-y + height / 2); }

    private void loadConfiguration() {
        showGPS = sharedPrefs.getBoolean(PREF_GPS, true);
        showGLONASS = sharedPrefs.getBoolean(PREF_GLONASS, true);
        showGALILEO = sharedPrefs.getBoolean(PREF_GALILEO, true);
        showBEIDOU = sharedPrefs.getBoolean(PREF_BEIDOU, true);
        showUnused = sharedPrefs.getBoolean(PREF_SHOW_UNUSED, true);
    }

    public void saveConfiguration(boolean gps, boolean glonass, boolean galileo, boolean beidou, boolean showUnusedSatellites) {
        SharedPreferences.Editor editor = sharedPrefs.edit();

        this.showGPS = gps; editor.putBoolean(PREF_GPS, gps);
        this.showGLONASS = glonass; editor.putBoolean(PREF_GLONASS, glonass);
        this.showGALILEO = galileo; editor.putBoolean(PREF_GALILEO, galileo);
        this.showBEIDOU = beidou; editor.putBoolean(PREF_BEIDOU, beidou);
        this.showUnused = showUnusedSatellites; editor.putBoolean(PREF_SHOW_UNUSED, showUnusedSatellites);

        editor.apply();
        invalidate();
    }

    @Override
    public void onClick(View v) { showConfigurationDialog(); }

    private void showConfigurationDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);

        final CheckBox gpsCb = new CheckBox(getContext()); gpsCb.setText("GPS"); gpsCb.setChecked(showGPS);
        final CheckBox glonassCb = new CheckBox(getContext()); glonassCb.setText("GLONASS"); glonassCb.setChecked(showGLONASS);
        final CheckBox galileoCb = new CheckBox(getContext()); galileoCb.setText("GALILEO"); galileoCb.setChecked(showGALILEO);
        final CheckBox beidouCb = new CheckBox(getContext()); beidouCb.setText("BEIDOU"); beidouCb.setChecked(showBEIDOU);
        final CheckBox unusedCb = new CheckBox(getContext()); unusedCb.setText("Mostrar satélites não usados no FIX"); unusedCb.setChecked(showUnused);

        layout.addView(gpsCb); layout.addView(glonassCb); layout.addView(galileoCb); layout.addView(beidouCb); layout.addView(unusedCb);

        new AlertDialog.Builder(getContext())
                .setTitle("Configuração de Visualização GNSS")
                .setView(layout)
                .setPositiveButton("Salvar", (dialog, which) -> saveConfiguration(
                        gpsCb.isChecked(), glonassCb.isChecked(), galileoCb.isChecked(), beidouCb.isChecked(), unusedCb.isChecked()
                ))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Desenha a logo da constelação (Drawable). Se NÃO USADO no FIX, desenha uma borda colorida.
     */
    private void drawSatelliteMarker(Canvas canvas, float cx, float cy, int constellation, boolean isUsed) {
        Drawable satelliteIcon = null;
        int borderColor = 0;

        // 1. Seleciona o Drawable e a Cor da Borda
        switch (constellation) {
            case GnssStatus.CONSTELLATION_GPS:
                satelliteIcon = gpsDrawable;
                borderColor = COLOR_GPS_BORDER;
                break;
            case GnssStatus.CONSTELLATION_GLONASS:
                satelliteIcon = glonassDrawable;
                borderColor = COLOR_GLONASS_BORDER;
                break;
            case GnssStatus.CONSTELLATION_GALILEO:
                satelliteIcon = galileoDrawable;
                borderColor = COLOR_GALILEO_BORDER;
                break;
            case GnssStatus.CONSTELLATION_BEIDOU:
                satelliteIcon = beidouDrawable;
                borderColor = COLOR_BEIDOU_BORDER;
                break;
            default:
                // Fallback para as outras constelações (desenho simples)
                paint.setColor(Color.GRAY);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, 10, paint);
                return;
        }

        if (satelliteIcon == null) return;

        // 2. Define o tamanho e os limites do ícone
        final int iconSize = 36;

        int left = (int) (cx - iconSize / 2);
        int top = (int) (cy - iconSize / 2);
        int right = (int) (cx + iconSize / 2);
        int bottom = (int) (cy + iconSize / 2);

        satelliteIcon.setBounds(left, top, right, bottom);

        // 3. Aplica Borda se NÃO USADO no FIX
        if (!isUsed) {
            final int borderSize = 4; // Largura da borda
            final int outerRadius = iconSize / 2 + borderSize; // Raio total do círculo da borda

            // Desenha o círculo da borda (preenchido com a cor da constelação)
            Paint borderPaint = new Paint();
            borderPaint.setAntiAlias(true);
            borderPaint.setStyle(Paint.Style.FILL);
            borderPaint.setColor(borderColor);
            canvas.drawCircle(cx, cy, outerRadius, borderPaint);
        }

        // O ColorFilter é removido para que o ícone mantenha sua cor original
        satelliteIcon.clearColorFilter();

        // 4. Desenha o ícone no Canvas
        satelliteIcon.draw(canvas);
    }

    // --- Lógica de Desenho Principal (onDraw) ---
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int visibleCount = 0;
        int usedCount = 0;
        int cx = computeXc(0);
        int cy = computeYc(0);

        // ... (Desenho da Esfera Celeste e Eixos) ...
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.BLUE);

        canvas.drawCircle(cx, cy, r, paint);
        canvas.drawCircle(cx, cy, r * 2 / 3, paint);
        canvas.drawCircle(cx, cy, r * 1 / 3, paint);

        canvas.drawLine(cx, cy - r, cx, cy + r, paint);
        canvas.drawLine(cx - r, cy, cx + r, cy, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(30);
        paint.setColor(Color.WHITE);

        canvas.drawText("N", cx, cy - r - 10, paint);
        canvas.drawText("S", cx, cy + r + 40, paint);
        canvas.drawText("L", cx + r + 10, cy + 10, paint);
        canvas.drawText("O", cx - r - 40, cy + 10, paint);

        // --- 2. Desenho do Marcador de Zênite (Centro) com Precisão do Fix ---

        // 2.1. Determina a cor do marcador com base na precisão da última localização
        if (lastLocation != null && lastLocation.hasAccuracy()) {
            float accuracy = lastLocation.getAccuracy();
            if (accuracy < 5) { paint.setColor(Color.GREEN); }
            else if (accuracy < 20) { paint.setColor(Color.YELLOW); }
            else { paint.setColor(Color.RED); }
        } else {
            paint.setColor(Color.DKGRAY);
        }

        // 2.2. Desenha o estilo do marcador (ZenithMarkerStyle)
        Paint zenithPaint = new Paint(paint);

        switch (zenithStyle) {
            case ZENITH_CIRCLE:
                zenithPaint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(cx, cy, 10, zenithPaint);
                break;
            case ZENITH_CROSS:
                zenithPaint.setStrokeWidth(8);
                canvas.drawLine(cx - 10, cy, cx + 10, cy, zenithPaint);
                canvas.drawLine(cx, cy - 10, cx, cy + 10, zenithPaint);
                break;
            case ZENITH_STAR:
                zenithPaint.setStyle(Paint.Style.FILL);
                Path star = new Path();
                for (int i = 0; i < 5; i++) {
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
                canvas.drawPath(star, zenithPaint);
                break;
        }

        // --- 3. Desenho e Filtragem dos Satélites ---
        if (gnssStatus != null) {
            int count = gnssStatus.getSatelliteCount();

            for (int i = 0; i < count; i++) {
                int constellation = gnssStatus.getConstellationType(i);
                boolean usedInFix = gnssStatus.usedInFix(i);

                // Aplica Filtros de Constelação e Uso (configuráveis pelo usuário)
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

                // Cálculo das Coordenadas (Projeção Azimutal Equidistante)
                float dz = 90f - el;
                float rho = r * dz / 90f;
                float x = (float) (rho * Math.sin(Math.toRadians(az)));
                float y = (float) (rho * Math.cos(Math.toRadians(az)));

                float sat_cx = computeXc(x);
                float sat_cy = computeYc(y);

                // Desenha o LOGO do satélite (identificação visual)
                drawSatelliteMarker(canvas, sat_cx, sat_cy, constellation, usedInFix);

                // Desenha o ID do satélite (Texto)
                paint.setColor(Color.WHITE);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(25);
                String satID = gnssStatus.getSvid(i) + "";
                // O tamanho do ícone é 36px, ajusta a posição do texto
                canvas.drawText(satID, sat_cx + 23, sat_cy + 8, paint);
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
