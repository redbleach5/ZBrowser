package com.zbrowser;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class BrowserSettings {

    private static final String PREFS_NAME = "zbrowser_prefs";
    private static final String KEY_SEARCH_ENGINE = "search_engine";
    private static final String KEY_JAVASCRIPT = "javascript";
    private static final String KEY_COOKIES = "cookies";
    private static final String KEY_BLOCK_ADS = "block_ads";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_USER_AGENT = "user_agent";
    private static final String KEY_HOME_PAGE = "home_page";

    // Search engine constants
    public static final int SEARCH_GOOGLE = 0;
    public static final int SEARCH_YANDEX = 1;
    public static final int SEARCH_DUCKDUCKGO = 2;
    public static final int SEARCH_BING = 3;

    // User agent constants
    public static final int UA_DEFAULT = 0;
    public static final int UA_DESKTOP_CHROME = 1;
    public static final int UA_MOBILE_SAFARI = 2;

    private final SharedPreferences prefs;

    public BrowserSettings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getSearchEngine() {
        return prefs.getInt(KEY_SEARCH_ENGINE, SEARCH_GOOGLE);
    }

    public void setSearchEngine(int engine) {
        prefs.edit().putInt(KEY_SEARCH_ENGINE, engine).apply();
    }

    public boolean isJavascriptEnabled() {
        return prefs.getBoolean(KEY_JAVASCRIPT, true);
    }

    public void setJavascriptEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_JAVASCRIPT, enabled).apply();
    }

    public boolean isCookiesEnabled() {
        return prefs.getBoolean(KEY_COOKIES, true);
    }

    public void setCookiesEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_COOKIES, enabled).apply();
    }

    public boolean isBlockAds() {
        return prefs.getBoolean(KEY_BLOCK_ADS, true);
    }

    public void setBlockAds(boolean block) {
        prefs.edit().putBoolean(KEY_BLOCK_ADS, block).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean dark) {
        prefs.edit().putBoolean(KEY_DARK_MODE, dark).apply();
    }

    public int getTextSize() {
        return prefs.getInt(KEY_TEXT_SIZE, 100);
    }

    public void setTextSize(int size) {
        prefs.edit().putInt(KEY_TEXT_SIZE, Math.max(50, Math.min(size, 200))).apply();
    }

    public int getUserAgent() {
        return prefs.getInt(KEY_USER_AGENT, UA_DEFAULT);
    }

    public void setUserAgent(int ua) {
        prefs.edit().putInt(KEY_USER_AGENT, ua).apply();
    }

    public String getHomePage() {
        return prefs.getString(KEY_HOME_PAGE, "");
    }

    public void setHomePage(String url) {
        prefs.edit().putString(KEY_HOME_PAGE, url).apply();
    }

    public String getSearchUrl(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");
            switch (getSearchEngine()) {
                case SEARCH_YANDEX:
                    return "https://yandex.ru/search/?text=" + encoded;
                case SEARCH_DUCKDUCKGO:
                    return "https://duckduckgo.com/?q=" + encoded;
                case SEARCH_BING:
                    return "https://www.bing.com/search?q=" + encoded;
                default:
                    return "https://www.google.com/search?q=" + encoded;
            }
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always available, but fallback
            return "https://www.google.com/search?q=" + query.replace(" ", "+");
        }
    }

    public String getUserAgentString(Context context) {
        switch (getUserAgent()) {
            case UA_DESKTOP_CHROME:
                return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
            case UA_MOBILE_SAFARI:
                return "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
            default:
                return null;
        }
    }

    public void clearAllData(Context context) {
        prefs.edit().clear().apply();
    }
}
