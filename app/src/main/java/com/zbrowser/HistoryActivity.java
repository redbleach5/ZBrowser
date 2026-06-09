package com.zbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends Activity {

    // SCANDI Colors
    private static final String COLOR_BG = "#F5F3F0";
    private static final String COLOR_SURFACE = "#FFFFFF";
    private static final String COLOR_TEXT_PRIMARY = "#1A1A1A";
    private static final String COLOR_TEXT_SECONDARY = "#8A8A8A";
    private static final String COLOR_TEXT_HINT = "#B8B8B8";
    private static final String COLOR_ACCENT = "#2D5F6E";
    private static final String COLOR_DIVIDER = "#EAEAEA";
    private static final String COLOR_ERROR = "#C45B5B";

    private BrowserDatabase database;
    private List<JSONObject> historyItems;
    private LinearLayout layout;
    private LinearLayout cardsContainer;
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

        database = new BrowserDatabase(this);

        // Root layout — warm off-white
        layout = new LinearLayout(this);
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
        title.setText("История");
        title.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        title.setTextSize(20);
        title.setTypeface(null, Typeface.NORMAL);
        title.setPadding(dp(12), dp(8), 0, dp(8));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        title.setLayoutParams(titleParams);

        // Clear all button — minimal text
        TextView clearButton = new TextView(this);
        clearButton.setText("Очистить");
        clearButton.setTextSize(13);
        clearButton.setTextColor(Color.parseColor(COLOR_TEXT_HINT));
        clearButton.setPadding(dp(12), dp(8), dp(12), dp(8));
        clearButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Очистить историю")
                    .setMessage("Удалить всю историю?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        database.clearHistory();
                        loadHistory();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        topBar.addView(backButton);
        topBar.addView(title);
        topBar.addView(clearButton);
        layout.addView(topBar);

        // Scrollable content
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.parseColor(COLOR_BG));
        scrollView.setFillViewport(true);

        LinearLayout scrollContent = new LinearLayout(this);
        scrollContent.setOrientation(LinearLayout.VERTICAL);
        scrollContent.setPadding(dp(20), dp(8), dp(20), dp(20));

        // Empty state
        emptyView = new TextView(this);
        emptyView.setText("Нет истории");
        emptyView.setTextSize(16);
        emptyView.setTextColor(Color.parseColor(COLOR_TEXT_HINT));
        emptyView.setPadding(0, dp(100), 0, 0);
        emptyView.setGravity(Gravity.CENTER_HORIZONTAL);
        emptyView.setVisibility(View.GONE);
        scrollContent.addView(emptyView);

        // Cards container
        cardsContainer = new LinearLayout(this);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        scrollContent.addView(cardsContainer);

        scrollView.addView(scrollContent);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollView.setLayoutParams(scrollParams);
        layout.addView(scrollView);

        setContentView(layout);
        loadHistory();
    }

    private void loadHistory() {
        historyItems = database.getHistory(500);
        cardsContainer.removeAllViews();

        if (historyItems.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        emptyView.setVisibility(View.GONE);

        // Group history by date
        String currentDateGroup = "";
        Calendar now = Calendar.getInstance();
        Calendar itemCal = Calendar.getInstance();

        for (int i = 0; i < historyItems.size(); i++) {
            JSONObject hItem = historyItems.get(i);
            long timestamp = hItem.optLong("visited_at", 0) * 1000;
            String dateGroup;

            if (timestamp > 0) {
                itemCal.setTimeInMillis(timestamp);

                if (isSameDay(itemCal, now)) {
                    dateGroup = "Сегодня";
                } else if (isSameDay(itemCal, getYesterday())) {
                    dateGroup = "Вчера";
                } else if (isThisWeek(itemCal, now)) {
                    dateGroup = new SimpleDateFormat("EEEE", new Locale("ru")).format(new Date(timestamp));
                } else {
                    dateGroup = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru")).format(new Date(timestamp));
                }
            } else {
                dateGroup = "Ранее";
            }

            // Add date group header if changed
            if (!dateGroup.equals(currentDateGroup)) {
                currentDateGroup = dateGroup;
                TextView groupHeader = new TextView(this);
                groupHeader.setText(dateGroup);
                groupHeader.setTextSize(13);
                groupHeader.setTextColor(Color.parseColor(COLOR_ACCENT));
                groupHeader.setTypeface(null, Typeface.NORMAL);
                groupHeader.setPadding(0, dp(16), 0, dp(4));
                cardsContainer.addView(groupHeader);

                // Subtle divider
                View divider = new View(this);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(Color.parseColor(COLOR_DIVIDER));
                cardsContainer.addView(divider);
            }

            // Create history item card
            View card = createHistoryCard(hItem, i);
            cardsContainer.addView(card);
        }
    }

    private View createHistoryCard(JSONObject hItem, int position) {
        String hTitle = hItem.optString("title", "Без названия");
        String hUrl = hItem.optString("url", "");
        long timestamp = hItem.optLong("visited_at", 0) * 1000;

        // Card container — white, 16dp radius
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(6));
        card.setLayoutParams(cardParams);

        // Card background — white, 16dp radius
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setShape(GradientDrawable.RECTANGLE);
        cardBg.setCornerRadius(dp(16));
        cardBg.setColor(Color.WHITE);
        card.setBackgroundDrawable(cardBg);
        card.setElevation(dp(1));

        // Text area
        LinearLayout textArea = new LinearLayout(this);
        textArea.setOrientation(LinearLayout.VERTICAL);
        textArea.setPadding(0, 0, dp(8), 0);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textArea.setLayoutParams(textParams);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText(hTitle);
        titleView.setTextSize(14);
        titleView.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        titleView.setMaxLines(2);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textArea.addView(titleView);

        // URL
        TextView urlView = new TextView(this);
        String displayUrl = hUrl;
        if (displayUrl.length() > 50) {
            displayUrl = displayUrl.substring(0, 47) + "...";
        }
        urlView.setText(displayUrl);
        urlView.setTextSize(12);
        urlView.setTextColor(Color.parseColor(COLOR_TEXT_SECONDARY));
        urlView.setMaxLines(1);
        urlView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        urlView.setPadding(0, dp(2), 0, 0);
        textArea.addView(urlView);

        // Time
        if (timestamp > 0) {
            TextView timeView = new TextView(this);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeView.setText(sdf.format(new Date(timestamp)));
            timeView.setTextSize(11);
            timeView.setTextColor(Color.parseColor(COLOR_TEXT_HINT));
            timeView.setPadding(0, dp(2), 0, 0);
            textArea.addView(timeView);
        }

        card.addView(textArea);

        // Right arrow icon
        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.ic_forward);
        arrow.setColorFilter(Color.parseColor(COLOR_TEXT_HINT));
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        arrow.setLayoutParams(arrowParams);
        card.addView(arrow);

        // Long-press to delete
        card.setOnLongClickListener(v -> {
            String itemName = hItem.optString("title", "запись");
            long hid = hItem.optLong("id", -1);
            new AlertDialog.Builder(this)
                    .setTitle("Удалить запись")
                    .setMessage("Удалить \"" + itemName + "\" из истории?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        if (hid > 0) {
                            database.deleteHistoryItem(hid);
                        }
                        loadHistory();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
            return true;
        });

        // Click to open
        card.setOnClickListener(v -> {
            try {
                String url = hItem.getString("url");
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("open_url", url);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return card;
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private Calendar getYesterday() {
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        return yesterday;
    }

    private boolean isThisWeek(Calendar item, Calendar now) {
        Calendar weekAgo = Calendar.getInstance();
        weekAgo.add(Calendar.DAY_OF_YEAR, -7);
        return item.after(weekAgo);
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
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
