package com.zbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TabsActivity extends Activity {

    // SCANDI Colors
    private static final String COLOR_BG = "#F5F3F0";
    private static final String COLOR_SURFACE = "#FFFFFF";
    private static final String COLOR_TEXT_PRIMARY = "#1A1A1A";
    private static final String COLOR_TEXT_SECONDARY = "#8A8A8A";
    private static final String COLOR_TEXT_HINT = "#B8B8B8";
    private static final String COLOR_ACCENT = "#2D5F6E";
    private static final String COLOR_DIVIDER = "#EAEAEA";
    private static final String COLOR_ERROR = "#C45B5B";
    private static final String COLOR_INCOGNITO = "#2A2A2A";

    private List<TabInfo> tabs;
    private int currentTabId;
    private GridLayout tabsGrid;
    private ScrollView scrollView;
    private TextView emptyView;

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

        tabs = getIntent().getParcelableArrayListExtra("tabs");
        currentTabId = getIntent().getIntExtra("current_tab", 0);

        if (tabs == null) {
            tabs = new ArrayList<>();
        }

        // Root layout — warm off-white
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.parseColor(COLOR_BG));

        // Top bar — clean, no gradient, Scandi style
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(20), dp(28), dp(20), dp(12));

        // Back button
        ImageButton backButton = new ImageButton(this);
        backButton.setImageResource(R.drawable.ic_back);
        backButton.setBackgroundColor(0x00000000);
        backButton.getDrawable().setTint(Color.parseColor(COLOR_TEXT_SECONDARY));
        backButton.setPadding(dp(8), dp(8), dp(8), dp(8));
        backButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Title + tab count
        LinearLayout titleArea = new LinearLayout(this);
        titleArea.setOrientation(LinearLayout.HORIZONTAL);
        titleArea.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleArea.setLayoutParams(titleParams);
        titleArea.setPadding(dp(12), 0, 0, 0);

        TextView title = new TextView(this);
        title.setText("Вкладки");
        title.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.NORMAL);
        titleArea.addView(title);

        // Tab count in small accent pill
        TextView countPill = new TextView(this);
        countPill.setText(String.valueOf(tabs.size()));
        countPill.setTextSize(12);
        countPill.setTextColor(Color.parseColor(COLOR_ACCENT));
        countPill.setTypeface(null, android.graphics.Typeface.BOLD);
        countPill.setPadding(dp(8), dp(2), dp(8), dp(2));
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        pillParams.setMargins(dp(8), 0, 0, 0);
        countPill.setLayoutParams(pillParams);

        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setShape(GradientDrawable.RECTANGLE);
        pillBg.setCornerRadius(dp(10));
        pillBg.setColor(Color.parseColor("#E8F0F2"));
        countPill.setBackgroundDrawable(pillBg);
        titleArea.addView(countPill);

        topBar.addView(backButton);
        topBar.addView(titleArea);
        layout.addView(topBar);

        // Scrollable content area
        scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor(COLOR_BG));
        scrollView.setFillViewport(true);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(dp(16), dp(8), dp(16), dp(16));

        // Empty state
        emptyView = new TextView(this);
        emptyView.setText("Нет открытых вкладок");
        emptyView.setTextSize(16);
        emptyView.setTextColor(Color.parseColor(COLOR_TEXT_HINT));
        emptyView.setPadding(0, dp(100), 0, 0);
        emptyView.setGravity(Gravity.CENTER_HORIZONTAL);
        emptyView.setVisibility(tabs.isEmpty() ? View.VISIBLE : View.GONE);
        scrollContent.addView(emptyView);

        // Grid for tab cards — 2-column
        tabsGrid = new GridLayout(this);
        tabsGrid.setColumnCount(2);
        tabsGrid.setUseDefaultMargins(false);
        tabsGrid.setPadding(0, 0, 0, dp(12));

        // Populate tab cards
        for (int i = 0; i < tabs.size(); i++) {
            TabInfo tab = tabs.get(i);
            View card = createTabCard(tab, tab.getId() == currentTabId);
            tabsGrid.addView(card);
        }

        // Add "New tab" card at the end
        View newTabCard = createNewTabCard();
        tabsGrid.addView(newTabCard);

        scrollContent.addView(tabsGrid);
        scrollView.addView(scrollContent);

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);
        layout.addView(scrollView);

        // Bottom: "Закрыть все" text link in error color, centered
        TextView closeAllBtn = new TextView(this);
        closeAllBtn.setText("Закрыть все");
        closeAllBtn.setTextSize(14);
        closeAllBtn.setTextColor(Color.parseColor(COLOR_ERROR));
        closeAllBtn.setGravity(Gravity.CENTER);
        closeAllBtn.setPadding(dp(20), dp(12), dp(20), dp(28));
        closeAllBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Закрыть все вкладки")
                    .setMessage("Закрыть все вкладки?")
                    .setPositiveButton("Закрыть", (dialog, which) -> {
                        Intent result = new Intent();
                        result.putExtra("action", 4);
                        setResult(RESULT_OK, result);
                        finish();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
        layout.addView(closeAllBtn);

        setContentView(layout);
    }

    private View createTabCard(TabInfo tab, boolean isActive) {
        int cardWidth = (getResources().getDisplayMetrics().widthPixels - dp(40)) / 2;

        // Card container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(0, 0, 0, 0);

        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = cardWidth;
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        cardParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(cardParams);

        // Card background — white, 16dp radius, 2dp elevation
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(Color.WHITE);
        if (isActive) {
            cardBg.setStroke(dp(1), Color.parseColor(COLOR_ACCENT));
        }
        card.setBackgroundDrawable(cardBg);
        card.setElevation(dp(2));

        // Top colored strip: 3dp height, accent for normal, charcoal for incognito
        View strip = new View(this);
        LinearLayout.LayoutParams stripParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(3));
        strip.setLayoutParams(stripParams);

        GradientDrawable stripBg = new GradientDrawable();
        stripBg.setShape(GradientDrawable.RECTANGLE);
        float[] radii = {dp(16), dp(16), dp(16), dp(16), 0, 0, 0, 0};
        stripBg.setCornerRadii(radii);

        if (tab.isIncognito()) {
            stripBg.setColor(Color.parseColor(COLOR_INCOGNITO));
        } else {
            stripBg.setColor(Color.parseColor(COLOR_ACCENT));
        }
        strip.setBackgroundDrawable(stripBg);
        card.addView(strip);

        // Content area
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setPadding(dp(12), dp(12), dp(8), dp(12));
        content.setGravity(Gravity.CENTER_VERTICAL);

        // Left: title + URL
        LinearLayout textArea = new LinearLayout(this);
        textArea.setOrientation(LinearLayout.VERTICAL);
        textArea.setPadding(0, 0, dp(8), 0);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textArea.setLayoutParams(textParams);

        // Incognito badge
        if (tab.isIncognito()) {
            TextView incognitoBadge = new TextView(this);
            incognitoBadge.setText("Инкогнито");
            incognitoBadge.setTextSize(10);
            incognitoBadge.setTextColor(Color.parseColor(COLOR_INCOGNITO));
            incognitoBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            incognitoBadge.setPadding(0, 0, 0, dp(4));
            textArea.addView(incognitoBadge);
        }

        // Title — 14sp medium
        TextView titleView = new TextView(this);
        String tabTitle = tab.getTitle() != null && !tab.getTitle().isEmpty() ? tab.getTitle() : "Новая вкладка";
        titleView.setText(tabTitle);
        titleView.setTextSize(14);
        titleView.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        titleView.setMaxLines(2);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleView.setTypeface(null, android.graphics.Typeface.NORMAL);
        textArea.addView(titleView);

        // URL — 12sp secondary
        TextView urlView = new TextView(this);
        String tabUrl = tab.getUrl();
        if (tabUrl != null && !tabUrl.isEmpty()) {
            if (tabUrl.length() > 40) {
                tabUrl = tabUrl.substring(0, 37) + "...";
            }
        } else {
            tabUrl = "";
        }
        urlView.setText(tabUrl);
        urlView.setTextSize(12);
        urlView.setTextColor(Color.parseColor(COLOR_TEXT_SECONDARY));
        urlView.setMaxLines(1);
        urlView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        urlView.setPadding(0, dp(2), 0, 0);
        textArea.addView(urlView);

        content.addView(textArea);

        // Close button — small × in top-right, hint color
        ImageButton closeBtn = new ImageButton(this);
        closeBtn.setImageResource(R.drawable.ic_close);
        closeBtn.setBackgroundColor(0x00000000);
        closeBtn.getDrawable().setTint(Color.parseColor(COLOR_TEXT_HINT));
        closeBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        closeBtn.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("action", 2);
            result.putExtra("tab_id", tab.getId());
            setResult(RESULT_OK, result);
            finish();
        });
        content.addView(closeBtn);

        card.addView(content);

        // Click card to switch tab
        card.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("action", 1);
            result.putExtra("tab_id", tab.getId());
            setResult(RESULT_OK, result);
            finish();
        });

        return card;
    }

    private View createNewTabCard() {
        int cardWidth = (getResources().getDisplayMetrics().widthPixels - dp(40)) / 2;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(16), dp(32), dp(16), dp(32));

        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = cardWidth;
        cardParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
        cardParams.setMargins(dp(4), dp(4), dp(4), dp(4));
        card.setLayoutParams(cardParams);

        // Dashed border style
        card.setBackgroundDrawable(getResources().getDrawable(R.drawable.new_tab_card_bg));

        // Plus icon
        TextView plusIcon = new TextView(this);
        plusIcon.setText("+");
        plusIcon.setTextSize(28);
        plusIcon.setTextColor(Color.parseColor(COLOR_ACCENT));
        plusIcon.setGravity(Gravity.CENTER);
        plusIcon.setTypeface(null, android.graphics.Typeface.NORMAL);
        card.addView(plusIcon);

        // "Новая вкладка" label
        TextView label = new TextView(this);
        label.setText("Новая вкладка");
        label.setTextSize(12);
        label.setTextColor(Color.parseColor(COLOR_TEXT_HINT));
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, dp(4), 0, 0);
        card.addView(label);

        card.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra("action", 3);
            result.putExtra("incognito", false);
            setResult(RESULT_OK, result);
            finish();
        });

        return card;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
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
