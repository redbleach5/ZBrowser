package com.zbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {

    // SCANDI Colors
    private static final String COLOR_BG = "#F5F3F0";
    private static final String COLOR_SURFACE = "#FFFFFF";
    private static final String COLOR_TEXT_PRIMARY = "#1A1A1A";
    private static final String COLOR_TEXT_SECONDARY = "#8A8A8A";
    private static final String COLOR_TEXT_HINT = "#B8B8B8";
    private static final String COLOR_ACCENT = "#2D5F6E";
    private static final String COLOR_ACCENT_LIGHT = "#E8F0F2";
    private static final String COLOR_DIVIDER = "#EAEAEA";
    private static final String COLOR_ERROR = "#C45B5B";

    private BrowserSettings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive mode
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        settings = new BrowserSettings(this);

        // Root layout — warm off-white
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor(COLOR_BG));

        // Top bar — clean Scandi style
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(12), dp(28), dp(12), dp(12));

        // Back button
        ImageButton backButton = new ImageButton(this);
        backButton.setImageResource(R.drawable.ic_back);
        backButton.setBackgroundColor(0x00000000);
        backButton.getDrawable().setTint(Color.parseColor(COLOR_TEXT_SECONDARY));
        backButton.setPadding(dp(8), dp(8), dp(8), dp(8));
        backButton.setOnClickListener(v -> finish());

        // Title
        TextView title = new TextView(this);
        title.setText("Настройки");
        title.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.NORMAL);
        title.setPadding(dp(12), dp(8), 0, dp(8));

        topBar.addView(backButton);
        topBar.addView(title);
        layout.addView(topBar);

        // ScrollView for settings
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor(COLOR_BG));

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(dp(20), dp(8), dp(20), dp(20));

        // ===== Search Engine Section =====
        addSectionHeader(scrollContent, "Поисковая система");

        LinearLayout searchCard = createCard();
        Spinner searchSpinner = new Spinner(this);
        String[] engines = {"Google", "Яндекс", "DuckDuckGo", "Bing"};
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, engines);
        searchAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchSpinner.setAdapter(searchAdapter);
        searchSpinner.setSelection(settings.getSearchEngine());
        searchSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                settings.setSearchEngine(position);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        searchCard.addView(searchSpinner);
        scrollContent.addView(searchCard);

        // ===== Privacy Section =====
        addSectionHeader(scrollContent, "Конфиденциальность");

        LinearLayout privacyCard = createCard();
        privacyCard.setOrientation(LinearLayout.VERTICAL);

        addSwitchRow(privacyCard, "JavaScript", settings.isJavascriptEnabled(), checked -> settings.setJavascriptEnabled(checked));
        addDivider(privacyCard);
        addSwitchRow(privacyCard, "Cookies", settings.isCookiesEnabled(), checked -> settings.setCookiesEnabled(checked));
        addDivider(privacyCard);
        addSwitchRow(privacyCard, "Блокировать рекламу", settings.isBlockAds(), checked -> settings.setBlockAds(checked));

        scrollContent.addView(privacyCard);

        // ===== Appearance Section =====
        addSectionHeader(scrollContent, "Оформление");

        LinearLayout appearanceCard = createCard();
        appearanceCard.setOrientation(LinearLayout.VERTICAL);

        addSwitchRow(appearanceCard, "Тёмная тема (WebView)", settings.isDarkMode(), checked -> settings.setDarkMode(checked));
        addDivider(appearanceCard);

        // Text size row
        LinearLayout textSizeRow = new LinearLayout(this);
        textSizeRow.setOrientation(LinearLayout.VERTICAL);
        textSizeRow.setPadding(0, dp(12), 0, dp(4));

        TextView textSizeLabel = new TextView(this);
        textSizeLabel.setText("Размер текста: " + settings.getTextSize() + "%");
        textSizeLabel.setTextSize(14);
        textSizeLabel.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        textSizeLabel.setPadding(0, 0, 0, dp(4));
        textSizeRow.addView(textSizeLabel);

        SeekBar textSizeBar = new SeekBar(this);
        textSizeBar.setMax(200);
        textSizeBar.setProgress(settings.getTextSize());
        textSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    settings.setTextSize(Math.max(progress, 50));
                    textSizeLabel.setText("Размер текста: " + progress + "%");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        textSizeRow.addView(textSizeBar);
        appearanceCard.addView(textSizeRow);

        scrollContent.addView(appearanceCard);

        // ===== User Agent Section =====
        addSectionHeader(scrollContent, "User Agent");

        LinearLayout uaCard = createCard();
        Spinner uaSpinner = new Spinner(this);
        String[] uas = {"По умолчанию (Android)", "Desktop Chrome", "Mobile Safari"};
        ArrayAdapter<String> uaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, uas);
        uaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        uaSpinner.setAdapter(uaAdapter);
        uaSpinner.setSelection(settings.getUserAgent());
        uaSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                settings.setUserAgent(position);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        uaCard.addView(uaSpinner);
        scrollContent.addView(uaCard);

        // ===== Danger Zone =====
        addSectionHeader(scrollContent, "Управление данными");

        LinearLayout dangerCard = createCard();
        dangerCard.setOrientation(LinearLayout.VERTICAL);
        dangerCard.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Clear data button — muted red
        Button clearBtn = new Button(this);
        clearBtn.setText("Очистить данные браузера");
        clearBtn.setTextColor(Color.WHITE);

        GradientDrawable clearBg = new GradientDrawable();
        clearBg.setShape(GradientDrawable.RECTANGLE);
        clearBg.setCornerRadius(dp(24));
        clearBg.setColor(Color.parseColor(COLOR_ERROR));
        clearBtn.setBackgroundDrawable(clearBg);
        clearBtn.setPadding(dp(24), dp(12), dp(24), dp(12));
        clearBtn.setAllCaps(false);
        clearBtn.setTextSize(14);

        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clearBtn.setLayoutParams(clearParams);
        clearBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Очистить данные")
                    .setMessage("Удалить все данные браузера (закладки, историю, кэш, cookies)?")
                    .setPositiveButton("Очистить", (dialog, which) -> {
                        BrowserDatabase database = new BrowserDatabase(this);
                        database.clearAllData();
                        database.close();
                        CookieManager.getInstance().removeAllCookies(null);
                        settings.clearAllData(this);
                        Toast.makeText(this, "Данные очищены", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
        dangerCard.addView(clearBtn);

        scrollContent.addView(dangerCard);

        // About text
        TextView about = new TextView(this);
        about.setText("ZBrowser v2.0.0\nЛёгкий и быстрый браузер");
        about.setTextSize(12);
        about.setTextColor(Color.parseColor(COLOR_TEXT_HINT));
        about.setPadding(0, dp(24), 0, dp(24));
        about.setGravity(Gravity.CENTER_HORIZONTAL);
        scrollContent.addView(about);

        scrollView.addView(scrollContent);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);
        layout.addView(scrollView);

        setContentView(layout);
    }

    private void addSectionHeader(LinearLayout parent, String text) {
        TextView header = new TextView(this);
        header.setText(text);
        header.setTextSize(13);
        header.setTextColor(Color.parseColor(COLOR_ACCENT));
        header.setTypeface(null, Typeface.NORMAL);
        header.setPadding(0, dp(20), 0, dp(8));
        parent.addView(header);
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(8), dp(16), dp(8));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardParams);

        // White card, 16dp radius, no border, 2dp elevation
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(Color.WHITE);
        card.setBackgroundDrawable(cardBg);
        card.setElevation(dp(2));

        return card;
    }

    private void addSwitchRow(LinearLayout parent, String label, boolean checked, SwitchListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextSize(14);
        labelText.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelText.setLayoutParams(labelParams);
        row.addView(labelText);

        Switch switchView = new Switch(this);
        switchView.setChecked(checked);
        // Accent color for switch
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            switchView.setThumbTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(COLOR_ACCENT)));
        }
        switchView.setOnCheckedChangeListener((v, isChecked) -> listener.onChanged(isChecked));
        row.addView(switchView);

        parent.addView(row);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor(COLOR_DIVIDER));
        parent.addView(divider);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    @FunctionalInterface
    private interface SwitchListener {
        void onChanged(boolean checked);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }
}
