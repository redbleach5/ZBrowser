package com.zbrowser;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Activity;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;

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
    private static final String COLOR_SUCCESS = "#5B8C6E";
    private static final String COLOR_INCOGNITO = "#2A2A2A";

    // Views
    private EditText urlBar;
    private ImageButton clearUrlButton;
    private ImageButton tabsButton;
    private ImageButton menuButton;
    private ImageView sslIcon;
    private ProgressBar progressBar;
    private WebView webView;
    private ScrollView homePage;
    private FrameLayout webViewContainer;
    private LinearLayout bottomBar;
    private LinearLayout findBar;
    private LinearLayout toolbarContainer;
    private LinearLayout suggestionsContainer;
    private ListView suggestionsList;
    private EditText findInput;
    private TextView findResultCount;
    private TextView tabCountBadge;
    private TextView greetingText;
    private EditText homeSearchBar;
    private ImageButton btnBack, btnForward, btnHome, btnRefresh, btnBookmark;
    private LinearLayout fullscreenExitBar;
    private TextView fullscreenTitle;
    private ImageButton fullscreenExitBtn;

    // Menu bottom sheet
    private FrameLayout menuOverlay;
    private LinearLayout menuSheet;

    // State
    private BrowserSettings settings;
    private BrowserDatabase database;
    private List<TabInfo> tabs = new ArrayList<>();
    private int currentTabId = -1;
    private boolean isIncognito = false;
    private String currentUrl = "";
    private String currentTitle = "";

    // Feature state
    private boolean isFullScreen = false;
    private boolean isNightMode = false;
    private boolean isDataSaver = false;
    private boolean isVolumeScrollEnabled = false;
    private int nextTabId = 0;

    // GestureDetector for double-tap
    private GestureDetector gestureDetector;

    // Ad blocker patterns
    private static final Pattern[] AD_PATTERNS = {
            Pattern.compile(".*\\.doubleclick\\.net/.*"),
            Pattern.compile(".*\\.googlesyndication\\.com/.*"),
            Pattern.compile(".*\\.googleadservices\\.com/.*"),
            Pattern.compile(".*\\.google-analytics\\.com/.*"),
            Pattern.compile(".*adservice\\.google\\..*/.*"),
            Pattern.compile(".*ad\\.mo\\.doubleclick\\.net/.*"),
            Pattern.compile(".*pagead2\\.googlesyndication\\.com/.*"),
            Pattern.compile(".*\\.adnxs\\.com/.*"),
            Pattern.compile(".*\\.taboola\\.com/.*"),
            Pattern.compile(".*\\.outbrain\\.com/.*"),
            Pattern.compile(".*\\.moatads\\.com/.*"),
            Pattern.compile(".*\\.amazon-adsystem\\.com/.*"),
            Pattern.compile(".*facebook\\.com/tr\\?.*"),
            Pattern.compile(".*analytics\\.twitter\\.com/.*"),
            Pattern.compile(".*mc\\.yandex\\.ru/watch/.*"),
            Pattern.compile(".*an\\.yandex\\.ru/.*"),
            Pattern.compile(".*adfox\\.yandex\\.ru/.*"),
            Pattern.compile(".*/pagead/.*"),
            Pattern.compile(".*/adserver/.*"),
            Pattern.compile(".*/advertpro/.*"),
            Pattern.compile(".*/adframe.*"),
            Pattern.compile(".*/adscript.*"),
            Pattern.compile(".*/banner.*\\.js.*"),
            Pattern.compile(".*/popunder.*\\.js.*")
    };

    // Night mode CSS injection
    private static final String NIGHT_MODE_CSS = "javascript:(function(){" +
            "var style=document.createElement('style');" +
            "style.type='text/css';" +
            "style.id='zbrowser-night-mode';" +
            "style.innerHTML='html{filter:invert(1) hue-rotate(180deg);}img,video{filter:invert(1) hue-rotate(180deg);}';" +
            "document.head.appendChild(style);" +
            "})()";

    private static final String NIGHT_MODE_REMOVE_CSS = "javascript:(function(){" +
            "var style=document.getElementById('zbrowser-night-mode');" +
            "if(style)style.remove();" +
            "})()";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immersive sticky full-screen mode
        enableImmersiveMode();

        setContentView(R.layout.activity_main);

        settings = new BrowserSettings(this);
        database = new BrowserDatabase(this);

        initViews();
        applyScandiTints();
        applyWindowInsets();
        setupUrlBar();
        setupHomeSearchBar();
        setupSuggestions();
        setupWebView();
        setupBottomBar();
        setupHomePage();
        setupFindBar();
        setupMenuButton();
        setupMenuOverlay();
        setupFullScreen();
        setupGestureDetector();
        setupVolumeScroll();

        requestNotificationPermission();

        createNewTab(false);
        handleIntent(getIntent());
    }

    // ============ Immersive Mode ============

    private void enableImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            // Allow content to render into the display cutout area
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
    }

    // Cached safe insets for reuse (e.g. after immersive mode re-entry)
    private int cachedSafeTop = 0;
    private int cachedSafeBottom = 0;
    private int cachedSafeLeft = 0;
    private int cachedSafeRight = 0;

    /**
     * Apply window insets (display cutout / notch, status bar, navigation bar)
     * to properly position toolbar and bottom bar so content is not obscured.
     * Handles all notch types: center, corner, dual, hole-punch cameras.
     * Works reliably even with immersive sticky mode where status bar is hidden
     * but the cutout safe area must still be respected.
     */
    private void applyWindowInsets() {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView == null) return;

        rootView.setOnApplyWindowInsetsListener((v, insets) -> {
            applySafeInsets(insets);
            return insets;
        });

        // Also try to get cutout info directly as a fallback (for immersive mode)
        // Post delayed so the window is fully laid out first
        rootView.post(() -> {
            rootView.requestApplyInsets();
            // Fallback: directly read display cutout if insets haven't fired yet
            rootView.postDelayed(() -> {
                if (cachedSafeTop == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    tryDirectCutoutDetection();
                }
            }, 300);
        });
    }

    /**
     * Core method that applies safe insets from WindowInsets to all UI elements.
     * Called from onApplyWindowInsetsListener and also from direct cutout detection.
     */
    private void applySafeInsetsFromValues(int safeTop, int safeBottom, int safeLeft, int safeRight) {
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView == null) return;

        // Apply horizontal safe padding for rounded corners + side cutouts
        rootView.setPadding(safeLeft > 0 ? safeLeft : dp(8), 0, safeRight > 0 ? safeRight : dp(8), 0);

        // Toolbar: pad below notch/cutout
        if (toolbarContainer != null) {
            toolbarContainer.setPadding(
                    toolbarContainer.getPaddingLeft(),
                    safeTop > 0 ? safeTop : dp(28),
                    toolbarContainer.getPaddingRight(),
                    toolbarContainer.getPaddingBottom()
            );
        }

        // Home page: dynamic top padding instead of fixed 56dp
        if (homePage != null) {
            LinearLayout homeContent = findViewById(R.id.home_content);
            if (homeContent != null) {
                homeContent.setPadding(
                        homeContent.getPaddingLeft(),
                        safeTop > 0 ? safeTop + dp(24) : dp(56),
                        homeContent.getPaddingRight(),
                        homeContent.getPaddingBottom()
                );
            }
        }

        // Bottom bar container: pad above nav bar
        View bottomContainer = findViewById(R.id.bottom_bar_container);
        if (bottomContainer != null) {
            bottomContainer.setPadding(
                    bottomContainer.getPaddingLeft(),
                    bottomContainer.getPaddingTop(),
                    bottomContainer.getPaddingRight(),
                    safeBottom > 0 ? safeBottom : dp(16)
            );
        }

        // Menu bottom sheet: pad above nav bar
        if (menuSheet != null) {
            menuSheet.setPadding(
                    menuSheet.getPaddingLeft(),
                    menuSheet.getPaddingTop(),
                    menuSheet.getPaddingRight(),
                    Math.max(safeBottom, dp(24))
            );
        }

        // Fullscreen exit bar: pad below notch
        if (fullscreenExitBar != null) {
            fullscreenExitBar.setPadding(
                    fullscreenExitBar.getPaddingLeft(),
                    safeTop > 0 ? safeTop : 0,
                    fullscreenExitBar.getPaddingRight(),
                    fullscreenExitBar.getPaddingBottom()
            );
        }
    }

    private void applySafeInsets(WindowInsets insets) {
        int statusBarTop = insets.getSystemWindowInsetTop();
        int navBarBottom = insets.getSystemWindowInsetBottom();
        int insetLeft = insets.getSystemWindowInsetLeft();
        int insetRight = insets.getSystemWindowInsetRight();

        // On API 28+, account for display cutout (notch) specifically
        int cutoutTop = 0;
        int cutoutLeft = 0;
        int cutoutRight = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.view.DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                cutoutTop = cutout.getSafeInsetTop();
                cutoutLeft = cutout.getSafeInsetLeft();
                cutoutRight = cutout.getSafeInsetRight();
            }
        }

        // Use the MAX of status bar and cutout — content must be below BOTH
        cachedSafeTop = Math.max(statusBarTop, cutoutTop);
        cachedSafeLeft = Math.max(insetLeft, cutoutLeft);
        cachedSafeRight = Math.max(insetRight, cutoutRight);
        cachedSafeBottom = navBarBottom;

        applySafeInsetsFromValues(cachedSafeTop, cachedSafeBottom, cachedSafeLeft, cachedSafeRight);
    }

    /**
     * Fallback method: directly detect display cutout via Display API.
     * Needed because immersive mode can suppress WindowInsets delivery
     * on some devices/API levels.
     */
    @android.annotation.SuppressLint("NewApi")
    private void tryDirectCutoutDetection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return;
        try {
            android.view.DisplayCutout cutout = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout();
            if (cutout != null) {
                int cutoutTop = cutout.getSafeInsetTop();
                if (cutoutTop > 0 && cachedSafeTop < cutoutTop) {
                    cachedSafeTop = cutoutTop;
                    cachedSafeLeft = Math.max(cachedSafeLeft, cutout.getSafeInsetLeft());
                    cachedSafeRight = Math.max(cachedSafeRight, cutout.getSafeInsetRight());
                    applySafeInsetsFromValues(cachedSafeTop, cachedSafeBottom, cachedSafeLeft, cachedSafeRight);
                }
            }
        } catch (Exception e) {
            // Fallback not available, use default padding
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
            // Re-apply insets after immersive mode is set (cutout may need re-detection)
            View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
            if (rootView != null) {
                rootView.post(() -> {
                    rootView.requestApplyInsets();
                    // Also try direct cutout detection as fallback
                    if (cachedSafeTop == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        rootView.postDelayed(() -> tryDirectCutoutDetection(), 200);
                    }
                });
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    private void initViews() {
        urlBar = findViewById(R.id.url_bar);
        clearUrlButton = findViewById(R.id.clear_url_button);
        tabsButton = findViewById(R.id.tabs_button);
        menuButton = findViewById(R.id.menu_button);
        sslIcon = findViewById(R.id.ssl_icon);
        progressBar = findViewById(R.id.progress_bar);
        webView = findViewById(R.id.webview);
        homePage = findViewById(R.id.home_page);
        webViewContainer = findViewById(R.id.webview_container);
        bottomBar = findViewById(R.id.bottom_bar);
        findBar = findViewById(R.id.find_bar);
        toolbarContainer = findViewById(R.id.toolbar_container);
        suggestionsContainer = findViewById(R.id.suggestions_container);
        suggestionsList = findViewById(R.id.suggestions_list);
        findInput = findViewById(R.id.find_input);
        findResultCount = findViewById(R.id.find_result_count);
        tabCountBadge = findViewById(R.id.tab_count_badge);
        greetingText = findViewById(R.id.greeting_text);
        homeSearchBar = findViewById(R.id.home_search_bar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnHome = findViewById(R.id.btn_home);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnBookmark = findViewById(R.id.btn_bookmark);
        fullscreenExitBar = findViewById(R.id.fullscreen_exit_bar);
        fullscreenTitle = findViewById(R.id.fullscreen_title);
        fullscreenExitBtn = findViewById(R.id.fullscreen_exit_btn);
        menuOverlay = findViewById(R.id.menu_overlay);
        menuSheet = findViewById(R.id.menu_sheet);
    }

    private void applyScandiTints() {
        // Toolbar icons: warm medium gray (#8A8A8A)
        int tintColor = Color.parseColor(COLOR_TEXT_SECONDARY);
        tabsButton.getDrawable().setTint(tintColor);
        menuButton.getDrawable().setTint(tintColor);
        clearUrlButton.getDrawable().setTint(Color.parseColor(COLOR_TEXT_HINT));

        // Bottom bar icons: warm medium gray
        btnBack.getDrawable().setTint(tintColor);
        btnForward.getDrawable().setTint(tintColor);
        btnHome.getDrawable().setTint(tintColor);
        btnRefresh.getDrawable().setTint(tintColor);
        btnBookmark.getDrawable().setTint(tintColor);

        // Tab count badge: accent background
        tabCountBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(COLOR_ACCENT)));
    }

    private void setupUrlBar() {
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String input = urlBar.getText().toString().trim();
                if (!input.isEmpty()) {
                    loadUrl(input);
                }
                hideKeyboard();
                hideSuggestions();
                return true;
            }
            return false;
        });

        urlBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                urlBar.selectAll();
                clearUrlButton.setVisibility(View.VISIBLE);
                urlBar.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
                urlBar.setHintTextColor(Color.parseColor(COLOR_TEXT_HINT));
            } else {
                clearUrlButton.setVisibility(View.GONE);
                hideSuggestions();
                urlBar.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
                urlBar.setHintTextColor(Color.parseColor(COLOR_TEXT_HINT));
            }
        });

        urlBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearUrlButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() > 0 && urlBar.hasFocus()) {
                    showSuggestions(s.toString());
                } else {
                    hideSuggestions();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        clearUrlButton.setOnClickListener(v -> {
            urlBar.setText("");
            urlBar.requestFocus();
        });
    }

    private void setupHomeSearchBar() {
        homeSearchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String input = homeSearchBar.getText().toString().trim();
                if (!input.isEmpty()) {
                    urlBar.setText(input);
                    loadUrl(input);
                }
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void setupSuggestions() {
        suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            urlBar.setText(selected);
            loadUrl(selected);
            hideSuggestions();
        });
    }

    private void showSuggestions(String query) {
        if (query.isEmpty()) {
            hideSuggestions();
            return;
        }

        List<String> matches = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        try {
            List<JSONObject> bookmarks = database.getAllBookmarks();
            for (JSONObject bm : bookmarks) {
                String title = bm.optString("title", "");
                String url = bm.optString("url", "");
                if (title.toLowerCase().contains(lowerQuery) || url.toLowerCase().contains(lowerQuery)) {
                    matches.add(url);
                }
                if (matches.size() >= 5) break;
            }
        } catch (Exception e) { /* ignore */ }

        try {
            List<JSONObject> history = database.getHistory(50);
            for (JSONObject h : history) {
                String title = h.optString("title", "");
                String url = h.optString("url", "");
                if (title.toLowerCase().contains(lowerQuery) || url.toLowerCase().contains(lowerQuery)) {
                    if (!matches.contains(url)) {
                        matches.add(url);
                    }
                }
                if (matches.size() >= 8) break;
            }
        } catch (Exception e) { /* ignore */ }

        if (matches.isEmpty()) {
            hideSuggestions();
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, matches);
        suggestionsList.setAdapter(adapter);
        suggestionsContainer.setVisibility(View.VISIBLE);
    }

    private void hideSuggestions() {
        suggestionsContainer.setVisibility(View.GONE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(settings.isJavascriptEnabled());
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setSaveFormData(!isIncognito);
        webSettings.setSavePassword(false);
        webSettings.setBlockNetworkImage(isDataSaver);

        int textSize = settings.getTextSize();
        webSettings.setTextZoom(textSize);

        String ua = settings.getUserAgentString(this);
        if (ua != null) {
            webSettings.setUserAgentString(ua);
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(settings.isCookiesEnabled());
        cookieManager.setAcceptThirdPartyCookies(webView, settings.isCookiesEnabled());

        webView.setWebViewClient(new ZBrowserWebViewClient());
        webView.setWebChromeClient(new ZBrowserWebChromeClient());

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (url == null || url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                Toast.makeText(this, "Небезопасная ссылка для загрузки", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent downloadIntent = new Intent(this, DownloadService.class);
            downloadIntent.putExtra(DownloadService.EXTRA_URL, url);
            String filename = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype);
            downloadIntent.putExtra(DownloadService.EXTRA_FILENAME, filename);
            startService(downloadIntent);
            Toast.makeText(this, "Загрузка начата: " + filename, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomBar() {
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            }
        });

        btnForward.setOnClickListener(v -> {
            if (webView.canGoForward()) {
                webView.goForward();
            }
        });

        btnHome.setOnClickListener(v -> showHomePage());

        btnRefresh.setOnClickListener(v -> {
            if (webView.getUrl() != null) {
                webView.reload();
            }
        });

        btnBookmark.setOnClickListener(v -> toggleBookmark());

        tabsButton.setOnClickListener(v -> openTabsActivity());
    }

    private void setupHomePage() {
        updateGreeting();

        // Quick links — Scandi style: white cards with accent letter
        android.widget.GridLayout grid = findViewById(R.id.quick_links_grid);
        String[][] quickLinks = {
                {"Google", "https://google.com", "G"},
                {"YouTube", "https://youtube.com", "Y"},
                {"VK", "https://vk.com", "V"},
                {"Yandex", "https://yandex.ru", "Я"},
                {"Telegram", "https://web.telegram.org", "T"},
                {"GitHub", "https://github.com", "G"},
                {"Wiki", "https://wikipedia.org", "W"},
                {"Mail.ru", "https://mail.ru", "M"}
        };

        for (String[] link : quickLinks) {
            View quickLinkView = getLayoutInflater().inflate(R.layout.item_quick_link, grid, false);

            // Letter in accent color
            TextView letterView = quickLinkView.findViewById(R.id.quick_link_letter);
            letterView.setText(link[2]);

            // Name
            TextView nameView = quickLinkView.findViewById(R.id.quick_link_name);
            nameView.setText(link[0]);

            quickLinkView.setOnClickListener(v -> loadUrl(link[1]));
            grid.addView(quickLinkView);
        }

        // Incognito button — clean text, no background
        findViewById(R.id.incognito_button).setOnClickListener(v -> {
            createNewTab(true);
            Toast.makeText(this, "Режим инкогнито — история не сохраняется", Toast.LENGTH_LONG).show();
        });

        updateTabCount();
    }

    private void updateGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = "Доброе утро";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Добрый день";
        } else if (hour >= 17 && hour < 22) {
            greeting = "Добрый вечер";
        } else {
            greeting = "Доброй ночи";
        }
        greetingText.setText(greeting);
    }

    private void setupFindBar() {
        findInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = findInput.getText().toString();
                if (!query.isEmpty()) {
                    webView.findAllAsync(query);
                }
                hideKeyboard();
                return true;
            }
            return false;
        });

        findViewById(R.id.find_prev).setOnClickListener(v -> webView.findNext(false));
        findViewById(R.id.find_next).setOnClickListener(v -> webView.findNext(true));
        findViewById(R.id.find_close).setOnClickListener(v -> {
            findBar.setVisibility(View.GONE);
            webView.clearMatches();
            findInput.setText("");
            hideKeyboard();
        });
    }

    private void setupMenuButton() {
        menuButton.setOnClickListener(v -> showMainMenu());
    }

    // ============ Menu Bottom Sheet (SCANDI) ============

    private void setupMenuOverlay() {
        menuOverlay.setOnClickListener(v -> hideMenu());
    }

    private void showMainMenu() {
        menuSheet.removeAllViews();

        // Menu items: icon + text, SCANDI style
        String[][] menuGroups = {
                {"Новая вкладка", "ic_tabs"},
                {"Инкогнито", "ic_incognito"},
                {"---", ""},
                {"Закладки", "ic_bookmark"},
                {"История", "ic_history"},
                {"Загрузки", "ic_download"},
                {"---", ""},
                {"Найти на странице", "ic_search"},
                {"Режим ПК", "ic_settings"},
                {"Ночной режим", "ic_settings"},
                {"Полный экран", "ic_settings"},
                {"Скриншот", "ic_settings"},
                {"Экономия данных", "ic_settings"},
                {"---", ""},
                {"Поделиться", "ic_share"},
                {"Настройки", "ic_settings"}
        };

        int[] drawableIds = {
                R.drawable.ic_tabs,
                R.drawable.ic_incognito,
                0,
                R.drawable.ic_bookmark,
                R.drawable.ic_history,
                R.drawable.ic_download,
                0,
                R.drawable.ic_search,
                R.drawable.ic_settings,
                R.drawable.ic_settings,
                R.drawable.ic_settings,
                R.drawable.ic_settings,
                R.drawable.ic_settings,
                0,
                R.drawable.ic_share,
                R.drawable.ic_settings
        };

        for (int i = 0; i < menuGroups.length; i++) {
            if (menuGroups[i][0].equals("---")) {
                // Divider
                View divider = new View(this);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                divParams.setMargins(dp(16), dp(4), dp(16), dp(4));
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(Color.parseColor(COLOR_DIVIDER));
                menuSheet.addView(divider);
            } else {
                LinearLayout item = createMenuItem(menuGroups[i][0], drawableIds[i]);
                final int index = i;
                item.setOnClickListener(v -> {
                    hideMenu();
                    handleMenuItem(index);
                });
                menuSheet.addView(item);
            }
        }

        menuOverlay.setVisibility(View.VISIBLE);
    }

    private LinearLayout createMenuItem(String text, int iconRes) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        int padV = dp(12);
        int padH = dp(20);
        item.setPadding(padH, padV, padH, padV);
        item.setMinimumHeight(dp(48));

        // Ripple
        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        android.content.res.TypedArray ta = obtainStyledAttributes(attrs);
        android.graphics.drawable.Drawable ripple = ta.getDrawable(0);
        ta.recycle();
        item.setBackground(ripple);

        // Icon
        if (iconRes != 0) {
            ImageView icon = new ImageView(this);
            icon.setImageResource(iconRes);
            icon.setColorFilter(Color.parseColor(COLOR_TEXT_SECONDARY));
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
            iconParams.setMarginEnd(dp(16));
            icon.setLayoutParams(iconParams);
            item.addView(icon);
        }

        // Text
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(Color.parseColor(COLOR_TEXT_PRIMARY));
        item.addView(label);

        return item;
    }

    private void handleMenuItem(int index) {
        // Map index to action (skipping dividers)
        // 0=New tab, 1=Incognito, 2=div, 3=Bookmarks, 4=History, 5=Downloads, 6=div,
        // 7=Find, 8=Desktop, 9=Night, 10=Fullscreen, 11=Screenshot, 12=DataSaver, 13=div,
        // 14=Share, 15=Settings
        switch (index) {
            case 0: createNewTab(false); break;
            case 1: createNewTab(true); Toast.makeText(this, "Режим инкогнито — история не сохраняется", Toast.LENGTH_LONG).show(); break;
            case 3: startActivity(new Intent(this, BookmarksActivity.class)); break;
            case 4: startActivity(new Intent(this, HistoryActivity.class)); break;
            case 5: openDownloads(); break;
            case 7: showFindBar(); break;
            case 8: toggleDesktopMode(); break;
            case 9: toggleNightMode(); break;
            case 10: toggleFullScreen(); break;
            case 11: takeScreenshot(); break;
            case 12: toggleDataSaver(); break;
            case 14: sharePage(); break;
            case 15: startActivity(new Intent(this, SettingsActivity.class)); break;
        }
    }

    private void hideMenu() {
        menuOverlay.setVisibility(View.GONE);
    }

    // ============ Full Screen Mode ============

    private void setupFullScreen() {
        fullscreenExitBtn.setOnClickListener(v -> toggleFullScreen());
        fullscreenExitBar.setOnClickListener(v -> toggleFullScreen());
    }

    private void toggleFullScreen() {
        isFullScreen = !isFullScreen;
        if (isFullScreen) {
            toolbarContainer.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
            fullscreenExitBar.setVisibility(View.VISIBLE);
            fullscreenTitle.setText(currentTitle != null ? currentTitle : currentUrl);
        } else {
            toolbarContainer.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            fullscreenExitBar.setVisibility(View.GONE);
        }
    }

    // ============ Gesture Detector ============

    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (webView.getVisibility() == View.VISIBLE) {
                    webView.scrollTo(0, 0);
                    return true;
                }
                return false;
            }
        });

        webView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    // ============ Volume Key Scrolling ============

    private void setupVolumeScroll() {
        isVolumeScrollEnabled = settings.isDarkMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isVolumeScrollEnabled) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                webView.scrollBy(0, -200);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                webView.scrollBy(0, 200);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    // ============ Night/Reading Mode ============

    private void toggleNightMode() {
        isNightMode = !isNightMode;
        if (isNightMode) {
            if (webView.getUrl() != null) {
                webView.loadUrl(NIGHT_MODE_CSS);
            }
            Toast.makeText(this, "Ночной режим включён", Toast.LENGTH_SHORT).show();
        } else {
            if (webView.getUrl() != null) {
                webView.loadUrl(NIGHT_MODE_REMOVE_CSS);
            }
            Toast.makeText(this, "Ночной режим выключен", Toast.LENGTH_SHORT).show();
        }
    }

    // ============ Data Saver ============

    private void toggleDataSaver() {
        isDataSaver = !isDataSaver;
        webView.getSettings().setBlockNetworkImage(isDataSaver);
        if (isDataSaver) {
            Toast.makeText(this, "Экономия данных: изображения заблокированы", Toast.LENGTH_SHORT).show();
            if (webView.getUrl() != null) webView.reload();
        } else {
            Toast.makeText(this, "Экономия данных выключена", Toast.LENGTH_SHORT).show();
            if (webView.getUrl() != null) webView.reload();
        }
    }

    // ============ Screenshot ============

    private void takeScreenshot() {
        try {
            Bitmap bitmap = Bitmap.createBitmap(webView.getWidth(), webView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            webView.draw(canvas);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "ZBrowser_" + System.currentTimeMillis() + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ZBrowser");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream out = getContentResolver().openOutputStream(uri);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                        out.close();
                        Toast.makeText(this, "Скриншот сохранён", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ZBrowser");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "ZBrowser_" + System.currentTimeMillis() + ".png");
                java.io.FileOutputStream out = new java.io.FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.close();
                android.media.MediaScannerConnection.scanFile(this,
                        new String[]{file.getAbsolutePath()}, new String[]{"image/png"}, null);
                Toast.makeText(this, "Скриншот сохранён", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось сохранить скриншот", Toast.LENGTH_SHORT).show();
        }
    }

    // ============ Navigation ============

    private void loadUrl(String input) {
        if (input.isEmpty()) return;

        if (input.startsWith("file://") || input.startsWith("content://") || input.startsWith("javascript:")) {
            Toast.makeText(this, "Схема URL заблокирована по соображениям безопасности", Toast.LENGTH_SHORT).show();
            return;
        }

        String url;
        if (isUrl(input)) {
            url = input;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
        } else {
            url = settings.getSearchUrl(input);
        }

        currentUrl = url;
        homePage.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        webView.loadUrl(url);
        urlBar.setText(url);
        hideKeyboard();
        hideSuggestions();

        updateCurrentTab(url, currentTitle);
    }

    private boolean isUrl(String input) {
        if (input == null || input.isEmpty()) return false;
        if (input.contains(" ") && !input.startsWith("http")) return false;
        if (input.startsWith("http://") || input.startsWith("https://")) return true;
        if (input.matches("^[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]*[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}.*")) return true;
        return input.contains(".") && !input.contains(" ");
    }

    private void showHomePage() {
        homePage.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        urlBar.setText("");
        currentUrl = "";
        currentTitle = "";
        updateSslIcon(false);
        updateBookmarkIcon();
        updateGreeting();
    }

    private void showFindBar() {
        findBar.setVisibility(View.VISIBLE);
        findInput.requestFocus();
        showKeyboard();
    }

    private void toggleDesktopMode() {
        WebSettings webSettings = webView.getSettings();
        boolean isDesktop = webSettings.getUserAgentString().contains("Windows");

        if (isDesktop) {
            String ua = settings.getUserAgentString(this);
            if (ua == null) {
                webSettings.setUserAgentString("");
            } else {
                webSettings.setUserAgentString(ua);
            }
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(true);
            Toast.makeText(this, "Мобильный режим", Toast.LENGTH_SHORT).show();
        } else {
            webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            webSettings.setUseWideViewPort(true);
            webSettings.setLoadWithOverviewMode(false);
            Toast.makeText(this, "Режим ПК", Toast.LENGTH_SHORT).show();
        }

        if (webView.getUrl() != null) {
            webView.reload();
        }
    }

    private void toggleBookmark() {
        if (currentUrl == null || currentUrl.isEmpty()) {
            Toast.makeText(this, "Нет страницы для закладки", Toast.LENGTH_SHORT).show();
            return;
        }

        if (database.isBookmarked(currentUrl)) {
            new AlertDialog.Builder(this)
                    .setTitle("Удалить закладку")
                    .setMessage("Удалить \"" + (currentTitle != null ? currentTitle : currentUrl) + "\"?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        database.removeBookmark(currentUrl);
                        updateBookmarkIcon();
                        Toast.makeText(this, "Закладка удалена", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            database.addBookmark(currentTitle != null ? currentTitle : currentUrl, currentUrl);
            updateBookmarkIcon();
            Toast.makeText(this, "Закладка добавлена", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBookmarkIcon() {
        if (currentUrl != null && !currentUrl.isEmpty() && database.isBookmarked(currentUrl)) {
            btnBookmark.getDrawable().setTint(Color.parseColor(COLOR_ACCENT));
        } else {
            btnBookmark.getDrawable().setTint(Color.parseColor(COLOR_TEXT_SECONDARY));
        }
    }

    private void sharePage() {
        if (currentUrl == null || currentUrl.isEmpty()) {
            Toast.makeText(this, "Нет страницы для отправки", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, currentUrl);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentTitle != null ? currentTitle : "");
        startActivity(Intent.createChooser(shareIntent, "Поделиться через"));
    }

    private void openDownloads() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent("android.app.action.MANAGE_DOWNLOADS");
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(this, "Не удалось открыть загрузки", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ============ Tab Management ============

    private void createNewTab(boolean incognito) {
        int tabId = nextTabId++;
        TabInfo tab = new TabInfo(tabId, "Новая вкладка", "", incognito);
        tabs.add(tab);
        currentTabId = tabId;
        isIncognito = incognito;
        updateTabCount();
        showHomePage();

        if (isFullScreen) {
            isFullScreen = false;
            toolbarContainer.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            fullscreenExitBar.setVisibility(View.GONE);
        }

        if (incognito) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.getSettings().setSaveFormData(false);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        } else {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.getSettings().setSaveFormData(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, settings.isCookiesEnabled());
        }
    }

    private TabInfo findTabById(int tabId) {
        for (TabInfo tab : tabs) {
            if (tab.getId() == tabId) return tab;
        }
        return null;
    }

    private int findTabIndexById(int tabId) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).getId() == tabId) return i;
        }
        return -1;
    }

    private void switchToTab(int tabId) {
        TabInfo tab = findTabById(tabId);
        if (tab == null) return;
        currentTabId = tabId;
        isIncognito = tab.isIncognito();

        if (tab.getUrl() != null && !tab.getUrl().isEmpty()) {
            homePage.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(tab.getUrl());
        } else {
            showHomePage();
        }

        if (isFullScreen) {
            isFullScreen = false;
            toolbarContainer.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
            fullscreenExitBar.setVisibility(View.GONE);
        }

        if (isIncognito) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            webView.getSettings().setSaveFormData(false);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        } else {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.getSettings().setSaveFormData(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, settings.isCookiesEnabled());
        }
    }

    private void closeTab(int tabId) {
        int idx = findTabIndexById(tabId);
        if (idx < 0) return;

        boolean wasIncognito = tabs.get(idx).isIncognito();
        tabs.remove(idx);

        if (tabs.isEmpty()) {
            createNewTab(false);
            return;
        }

        if (currentTabId == tabId) {
            int newIdx = Math.min(idx, tabs.size() - 1);
            currentTabId = tabs.get(newIdx).getId();
            switchToTab(currentTabId);
        }

        if (wasIncognito) {
            boolean hasOtherIncognito = false;
            for (TabInfo t : tabs) {
                if (t.isIncognito()) {
                    hasOtherIncognito = true;
                    break;
                }
            }
            if (!hasOtherIncognito) {
                clearIncognitoData();
            }
        }

        updateTabCount();
    }

    private void clearIncognitoData() {
        try {
            CookieManager.getInstance().removeAllCookies(null);
            webView.clearCache(true);
            webView.clearFormData();
            webView.clearHistory();
            webView.clearSslPreferences();
        } catch (Exception e) {
            // Non-critical
        }
    }

    private void updateCurrentTab(String url, String title) {
        TabInfo tab = findTabById(currentTabId);
        if (tab != null) {
            tab.setUrl(url);
            tab.setTitle(title);
        }
    }

    private void updateTabCount() {
        int count = tabs.size();
        tabCountBadge.setText(String.valueOf(count));
        tabsButton.setContentDescription(getString(R.string.tab_count, count));
    }

    private void openTabsActivity() {
        Intent intent = new Intent(this, TabsActivity.class);
        intent.putExtra("current_tab", currentTabId);
        intent.putParcelableArrayListExtra("tabs", new ArrayList<>(tabs));
        startActivityForResult(intent, 1001);
    }

    // ============ SSL / Security ============

    private void updateSslIcon(boolean isSecure) {
        if (isSecure) {
            sslIcon.setVisibility(View.VISIBLE);
            sslIcon.setImageResource(R.drawable.ic_check);
            sslIcon.getDrawable().setTint(Color.parseColor(COLOR_SUCCESS));
        } else {
            sslIcon.setVisibility(View.GONE);
        }
    }

    // ============ Keyboard ============

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(findInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ============ Ad blocking ============

    private boolean shouldBlockAd(String url) {
        if (!settings.isBlockAds()) return false;
        for (Pattern pattern : AD_PATTERNS) {
            if (pattern.matcher(url).matches()) {
                return true;
            }
        }
        return false;
    }

    // ============ WebView Client ============

    private class ZBrowserWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("sms:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Не удалось открыть: " + url, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            if (url.startsWith("intent://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                        return true;
                    }
                } catch (URISyntaxException e) {
                    return true;
                } catch (Exception e) {
                    // App not installed, try fallback
                    String fallback = request.getUrl().getQueryParameter("fallback");
                    if (fallback != null && !fallback.isEmpty()) {
                        view.loadUrl(fallback);
                        return true;
                    }
                    Toast.makeText(MainActivity.this, "Приложение не найдено", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if (shouldBlockAd(url)) {
                return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream("".getBytes()));
            }

            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            currentUrl = url;
            urlBar.setText(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            currentUrl = url;
            urlBar.setText(url);
            updateSslIcon(url.startsWith("https://"));
            updateBookmarkIcon();

            if (view.getTitle() != null && !view.getTitle().isEmpty()) {
                currentTitle = view.getTitle();
            } else {
                currentTitle = url;
            }
            updateCurrentTab(url, currentTitle);

            // Save to history (non-incognito only)
            if (!isIncognito && url != null && !url.isEmpty()) {
                database.addHistory(currentTitle != null ? currentTitle : url, url);
            }

            // Re-apply night mode if active
            if (isNightMode) {
                view.loadUrl(NIGHT_MODE_CSS);
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Небезопасное соединение")
                    .setMessage("Сертификат безопасности сайта недействителен. Продолжить?")
                    .setPositiveButton("Продолжить", (dialog, which) -> handler.proceed())
                    .setNegativeButton("Отмена", (dialog, which) -> handler.cancel())
                    .show();
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_http_auth, null);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Авторизация")
                    .setView(dialogView)
                    .setPositiveButton("Войти", (dialog, which) -> {
                        String username = ((EditText) dialogView.findViewById(R.id.auth_username)).getText().toString();
                        String password = ((EditText) dialogView.findViewById(R.id.auth_password)).getText().toString();
                        handler.proceed(username, password);
                    })
                    .setNegativeButton("Отмена", (dialog, which) -> handler.cancel())
                    .show();
        }
    }

    // ============ Chrome Client ============

    private class ZBrowserWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(newProgress);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (title != null && !title.isEmpty()) {
                currentTitle = title;
                updateCurrentTab(currentUrl, title);
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Доступ к местоположению")
                    .setMessage("Разрешить сайту доступ к вашему местоположению?")
                    .setPositiveButton("Разрешить", (dialog, which) -> callback.invoke(origin, true, false))
                    .setNegativeButton("Отклонить", (dialog, which) -> callback.invoke(origin, false, false))
                    .show();
        }
    }

    // ============ Activity Results ============

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            int action = data.getIntExtra("action", 0);
            switch (action) {
                case 1: // Switch to tab
                    int tabId = data.getIntExtra("tab_id", -1);
                    if (tabId >= 0) switchToTab(tabId);
                    break;
                case 2: // Close tab
                    int closeTabId = data.getIntExtra("tab_id", -1);
                    if (closeTabId >= 0) closeTab(closeTabId);
                    break;
                case 3: // New tab
                    createNewTab(data.getBooleanExtra("incognito", false));
                    break;
                case 4: // Close all
                    tabs.clear();
                    createNewTab(false);
                    break;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            loadUrl(intent.getData().toString());
        } else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(intent.getType())) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && !sharedText.isEmpty()) {
                loadUrl(sharedText);
            }
        } else if (intent.hasExtra("open_url")) {
            loadUrl(intent.getStringExtra("open_url"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableImmersiveMode();
        updateGreeting();
    }

    @Override
    protected void onDestroy() {
        if (database != null) {
            database.close();
        }
        super.onDestroy();
    }

    // ============ Utility ============

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
