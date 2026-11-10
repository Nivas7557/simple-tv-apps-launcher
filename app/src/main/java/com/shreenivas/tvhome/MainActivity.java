package com.shreenivas.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int WALLPAPER_REQUEST = 41;
    private static final String PREFS = "launcher_preferences";
    private static final String PREF_ACCENT = "accent_color";
    private static final String PREF_ICON_SHAPE = "icon_shape";
    private static final String PREF_COLUMNS = "grid_columns";
    private static final String PREF_WALLPAPER = "wallpaper_uri";
    private static final String PREF_FAVORITES = "favorites";
    private static final String PREF_USER_NAME = "launcher_user_name";

    private final List<ResolveInfo> appCache = new ArrayList<>();
    private final List<View> panelFocusables = new ArrayList<>();
    private FrameLayout root;
    private LinearLayout body, appArea, favoritesRow, appGrid, settingsDrawer;
    private View drawerScrim, restoreFocus;
    private boolean drawerOpen, scanInProgress;
    private long lastScan;
    private String selectedCategory = "All";
    private String currentPanel = "";
    private ValueAnimator panelAnimator;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        buildScreen();
        refreshApps();
    }

    @Override protected void onResume() {
        super.onResume();
        if (appGrid != null && !scanInProgress && System.currentTimeMillis() - lastScan > 120000) refreshApps();
    }

    @Override public void onBackPressed() {
        if (drawerOpen) closePanel(); else super.onBackPressed();
    }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }
    private int accent() { return prefs().getInt(PREF_ACCENT, Color.rgb(45, 190, 220)); }
    private String theme() { return prefs().getString("launcher_theme", "ocean"); }
    private String userName() { return prefs().getString(PREF_USER_NAME, "Shree"); }

    private int screenWidthDp() {
        return Math.round(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
    }

    private void buildScreen() {
        root = new FrameLayout(this);
        String wallpaper = prefs().getString(PREF_WALLPAPER, null);
        if (wallpaper != null) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try { image.setImageURI(Uri.parse(wallpaper)); } catch (RuntimeException ignored) { }
            root.addView(image, new FrameLayout.LayoutParams(-1, -1));
            View shade = new View(this);
            shade.setBackgroundColor(0x9E071522);
            root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        } else root.setBackgroundColor(Color.rgb(7, 16, 27));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int sideInset = Math.round(screenWidthDp() * .05f);
        page.setPadding(dp(sideInset), dp(18), dp(sideInset), dp(14));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        page.addView(buildRibbon(), new LinearLayout.LayoutParams(-1, dp(58)));

        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(0x35020A12);
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(v -> closePanel());

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(-1, 0, 1);
        bodyParams.topMargin = dp(15);
        page.addView(body, bodyParams);

        appArea = new LinearLayout(this);
        appArea.setOrientation(LinearLayout.VERTICAL);
        appArea.setFocusable(false);
        body.addView(appArea, new LinearLayout.LayoutParams(0, -1, 1));
        buildAppArea();

        settingsDrawer = new LinearLayout(this);
        settingsDrawer.setOrientation(LinearLayout.VERTICAL);
        settingsDrawer.setPadding(dp(20), dp(18), dp(20), dp(14));
        settingsDrawer.setBackground(glassPanelBackground());
        settingsDrawer.setElevation(dp(18));
        settingsDrawer.setVisibility(View.GONE);
        settingsDrawer.setFocusable(false);
        body.addView(settingsDrawer, new LinearLayout.LayoutParams(0, -1));

        root.addView(drawerScrim, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        renderApps();
    }

    private View buildRibbon() {
        LinearLayout ribbon = new LinearLayout(this);
        ribbon.setGravity(Gravity.CENTER_VERTICAL);
        ribbon.setPadding(dp(18), dp(5), dp(10), dp(5));
        ribbon.setBackground(ribbonBackground());
        TextView brand = text(userName(), 16, accent(), true);
        brand.setLetterSpacing(.12f);
        ribbon.addView(brand);
        Button apps = pill("Apps", true);
        LinearLayout.LayoutParams appsParams = new LinearLayout.LayoutParams(-2, dp(42));
        appsParams.leftMargin = dp(18);
        ribbon.addView(apps, appsParams);
        apps.setFocusable(false);
        View gap = new View(this);
        ribbon.addView(gap, new LinearLayout.LayoutParams(0, 1, 1));
        ribbon.addView(text(new SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(new java.util.Date()), 14, Color.WHITE, false));

        Button wifi = pill("◉  Wi-Fi", false);
        LinearLayout.LayoutParams wifiParams = new LinearLayout.LayoutParams(-2, dp(42));
        wifiParams.leftMargin = dp(12);
        ribbon.addView(wifi, wifiParams);
        wifi.setContentDescription("Wi-Fi networks and controls");
        wifi.setOnClickListener(v -> openPanel("wifi"));

        Button customize = pill("✦  Customize", false);
        LinearLayout.LayoutParams customizeParams = new LinearLayout.LayoutParams(-2, dp(42));
        customizeParams.leftMargin = dp(7);
        ribbon.addView(customize, customizeParams);
        customize.setOnClickListener(v -> openPanel("customize"));

        Button quick = pill("⚙  Quick settings", false);
        LinearLayout.LayoutParams quickParams = new LinearLayout.LayoutParams(-2, dp(42));
        quickParams.leftMargin = dp(7);
        ribbon.addView(quick, quickParams);
        quick.setOnClickListener(v -> openPanel("quick"));
        return ribbon;
    }

    private void buildAppArea() {
        appArea.removeAllViews();
        TextView heading = text("Your apps", 30, Color.WHITE, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(4);
        appArea.addView(heading, headingParams);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        String[] categories = {"All", "Favorites", "Media", "Games", "Other"};
        List<Button> tabButtons = new ArrayList<>();
        for (String category : categories) {
            Button tab = pill(category, category.equals(selectedCategory));
            tab.setId(View.generateViewId());
            LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-2, dp(42));
            tabParams.rightMargin = dp(8);
            tabs.addView(tab, tabParams);
            tabButtons.add(tab);
            tab.setOnFocusChangeListener((v, focused) -> {
                v.setBackground(tabBackground(category.equals(selectedCategory), focused));
                if (focused && !category.equals(selectedCategory)) {
                    selectedCategory = category;
                    updateTabs(tabButtons, categories);
                    renderApps();
                }
            });
            tab.setOnClickListener(v -> {
                selectedCategory = category;
                updateTabs(tabButtons, categories);
                renderApps();
            });
        }
        linkHorizontal(tabButtons);
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, -2);
        tabsParams.topMargin = dp(12);
        tabsParams.bottomMargin = dp(8);
        appArea.addView(tabs, tabsParams);

        ScrollView appScroll = new ScrollView(this);
        appScroll.setClipToPadding(false);
        appScroll.setFillViewport(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        favoritesRow = new LinearLayout(this);
        favoritesRow.setOrientation(LinearLayout.HORIZONTAL);
        favoritesRow.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(favoritesRow);
        appGrid = new LinearLayout(this);
        appGrid.setOrientation(LinearLayout.VERTICAL);
        content.addView(appGrid);
        appScroll.addView(content);
        appArea.addView(appScroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private GradientDrawable ribbonBackground() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(230, 23, 48, 69), Color.argb(205, 8, 24, 40)});
        d.setCornerRadius(dp(28));
        d.setStroke(dp(1), Color.argb(210, 108, 187, 217));
        return d;
    }

    private GradientDrawable glassPanelBackground() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(242, 38, 63, 78), Color.argb(238, 13, 30, 45)});
        d.setCornerRadius(dp(26));
        d.setStroke(dp(2), Color.argb(215, 164, 213, 230));
        return d;
    }

    private void updateTabs(List<Button> tabs, String[] categories) {
        for (int i = 0; i < tabs.size(); i++) {
            boolean active = categories[i].equals(selectedCategory);
            tabs.get(i).setTextColor(active ? Color.rgb(5, 23, 35) : Color.WHITE);
            tabs.get(i).setBackground(tabBackground(active, tabs.get(i).hasFocus()));
        }
    }

    private GradientDrawable tabBackground(boolean active, boolean focused) {
        int color = active ? accent() : (focused ? Color.rgb(24, 76, 100) : Color.rgb(15, 35, 54));
        return roundRect(color, dp(22), focused ? accent() : Color.rgb(35, 75, 96));
    }

    private void refreshApps() {
        if (scanInProgress) return;
        scanInProgress = true;
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Map<String, ResolveInfo> unique = new LinkedHashMap<>();
            addApps(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique);
            addApps(pm, Intent.CATEGORY_LAUNCHER, unique);
            List<ResolveInfo> result = new ArrayList<>(unique.values());
            Collections.sort(result, Comparator.comparing(x -> x.loadLabel(pm).toString().toLowerCase(Locale.ROOT)));
            runOnUiThread(() -> {
                if (isFinishing()) return;
                appCache.clear();
                appCache.addAll(result);
                scanInProgress = false;
                lastScan = System.currentTimeMillis();
                renderApps();
            });
        }).start();
    }

    private void renderApps() {
        if (appGrid == null || favoritesRow == null) return;
        appGrid.removeAllViews();
        favoritesRow.removeAllViews();
        PackageManager pm = getPackageManager();
        Set<String> favorites = new HashSet<>(prefs().getStringSet(PREF_FAVORITES, Collections.<String>emptySet()));
        List<ResolveInfo> favoriteApps = new ArrayList<>();
        List<ResolveInfo> filtered = new ArrayList<>();
        for (ResolveInfo app : appCache) {
            if (favorites.contains(app.activityInfo.packageName)) favoriteApps.add(app);
            if (matchesCategory(app, selectedCategory, favorites)) filtered.add(app);
        }
        if (appCache.isEmpty()) {
            appGrid.addView(text(scanInProgress ? "Loading apps…" : "No apps found", 17, Color.LTGRAY, false));
            return;
        }
        if (!"Favorites".equals(selectedCategory) && !favoriteApps.isEmpty()) {
            TextView label = text("FAVORITES", 13, accent(), true);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
            p.bottomMargin = dp(7);
            favoritesRow.addView(label, p);
            for (ResolveInfo app : favoriteApps)
                favoritesRow.addView(makeTile(app, pm, true), new LinearLayout.LayoutParams(dp(140), dp(128)));
        }
        if (filtered.isEmpty()) {
            appGrid.addView(text("No apps in this category yet. Long-press an app tile to add it to Favorites.", 17, Color.LTGRAY, false));
            return;
        }
        int columns = Math.max(3, Math.min(8, prefs().getInt(PREF_COLUMNS, 5)));
        List<List<View>> rows = new ArrayList<>();
        for (int start = 0; start < filtered.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = dp(9);
            appGrid.addView(row, rp);
            List<View> rowViews = new ArrayList<>();
            for (int i = start; i < Math.min(start + columns, filtered.size()); i++) {
                View tile = makeTile(filtered.get(i), pm, false);
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(136), 1);
                tp.setMargins(dp(5), dp(4), dp(5), dp(4));
                row.addView(tile, tp);
                rowViews.add(tile);
            }
            rows.add(rowViews);
        }
        for (int r = 0; r < rows.size(); r++) for (int c = 0; c < rows.get(r).size(); c++) {
            View v = rows.get(r).get(c);
            if (c > 0) v.setNextFocusLeftId(rows.get(r).get(c - 1).getId());
            if (c + 1 < rows.get(r).size()) v.setNextFocusRightId(rows.get(r).get(c + 1).getId());
            if (r > 0) v.setNextFocusUpId(rows.get(r - 1).get(Math.min(c, rows.get(r - 1).size() - 1)).getId());
            if (r + 1 < rows.size()) v.setNextFocusDownId(rows.get(r + 1).get(Math.min(c, rows.get(r + 1).size() - 1)).getId());
        }
    }

    private boolean matchesCategory(ResolveInfo app, String category, Set<String> favorites) {
        if ("All".equals(category)) return true;
        if ("Favorites".equals(category)) return favorites.contains(app.activityInfo.packageName);
        String value = (app.activityInfo.packageName + " " + app.loadLabel(getPackageManager())).toLowerCase(Locale.ROOT);
        if ("Media".equals(category)) return containsAny(value, "tv", "video", "movie", "music", "youtube", "netflix", "prime", "disney", "plex", "player");
        if ("Games".equals(category)) return containsAny(value, "game", "play", "arcade", "racing", "minecraft", "stadia");
        return !matchesCategory(app, "Media", favorites) && !matchesCategory(app, "Games", favorites);
    }

    private boolean containsAny(String value, String... words) {
        for (String word : words) if (value.contains(word)) return true;
        return false;
    }

    private void addApps(PackageManager pm, String category, Map<String, ResolveInfo> out) {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(category);
        for (ResolveInfo app : pm.queryIntentActivities(query, 0))
            out.put(app.activityInfo.packageName + "/" + app.activityInfo.name, app);
    }

    private View makeTile(ResolveInfo info, PackageManager pm, boolean compact) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.setFocusable(true);
        tile.setId(View.generateViewId());
        tile.setBackground(appTileBackground(false));
        tile.setOnFocusChangeListener((v, focused) -> {
            v.animate().cancel();
            v.setBackground(appTileBackground(focused));
            v.animate().scaleX(focused ? 1.055f : 1f).scaleY(focused ? 1.055f : 1f)
                    .setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
            v.setElevation(focused ? dp(11) : dp(1));
            if (focused) startGlowPulse(v); else v.setAlpha(1f);
        });
        int size = dp(compact ? 48 : 56);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(info.loadIcon(pm));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        String shape = prefs().getString(PREF_ICON_SHAPE, "original");
        if (!"original".equals(shape)) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius("circle".equals(shape) ? size / 2f : dp(13));
            icon.setBackground(bg);
            icon.setClipToOutline(true);
            icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        }
        tile.addView(icon, new LinearLayout.LayoutParams(size, size));
        TextView name = text(info.loadLabel(pm).toString(), compact ? 12 : 14, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8);
        tile.addView(name, np);
        tile.setOnClickListener(v -> launchApp(info));
        tile.setOnLongClickListener(v -> { toggleFavorite(info.activityInfo.packageName); return true; });
        return tile;
    }

    private void startGlowPulse(View view) {
        view.animate().cancel();
        view.animate().alpha(.88f).setDuration(500).withEndAction(() -> {
            if (view.hasFocus()) view.animate().alpha(1f).setDuration(500).withEndAction(() -> {
                if (view.hasFocus()) startGlowPulse(view);
            }).start();
        }).start();
    }

    private GradientDrawable appTileBackground(boolean focused) {
        int start = focused ? Color.rgb(35, 133, 143) : surfaceColor();
        int end = focused ? Color.rgb(13, 72, 92) : ("glossy".equals(theme()) ? Color.rgb(11, 25, 43) : surfaceColor());
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{start, end});
        d.setCornerRadius(dp(20));
        d.setStroke(dp(focused ? 3 : 1), focused ? accent() : Color.rgb(74, 126, 145));
        return d;
    }

    private int surfaceColor() {
        switch (theme()) {
            case "neon": return Color.rgb(27, 21, 47);
            case "ocean": return Color.rgb(19, 75, 62);
            case "sunset": return Color.rgb(57, 34, 43);
            case "forest": return Color.rgb(27, 67, 47);
            case "glossy": return Color.rgb(35, 57, 63);
            default: return Color.rgb(20, 42, 44);
        }
    }

    private int focusSurfaceColor() {
        switch (theme()) {
            case "neon": return Color.rgb(76, 43, 112);
            case "ocean": return Color.rgb(36, 132, 98);
            case "sunset": return Color.rgb(112, 59, 66);
            case "forest": return Color.rgb(47, 105, 67);
            default: return Color.rgb(36, 93, 82);
        }
    }

    private void toggleFavorite(String packageName) {
        Set<String> current = new HashSet<>(prefs().getStringSet(PREF_FAVORITES, Collections.<String>emptySet()));
        if (!current.add(packageName)) current.remove(packageName);
        prefs().edit().putStringSet(PREF_FAVORITES, current).apply();
        Toast.makeText(this, current.contains(packageName) ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show();
        renderApps();
    }

    private void openPanel(String type) {
        if (drawerOpen && currentPanel.equals(type)) return;
        boolean wasOpen = drawerOpen;
        currentPanel = type;
        drawerOpen = true;
        restoreFocus = getCurrentFocus();
        panelFocusables.clear();
        settingsDrawer.removeAllViews();
        settingsDrawer.setPadding(dp(18), dp(17), dp(18), dp(14));
        buildPanelContents(type);
        buildPanelFooter();
        settingsDrawer.setVisibility(View.VISIBLE);
        drawerScrim.setVisibility(View.VISIBLE);

        int targetWidth = Math.max(dp(270), Math.min(dp(430), dp(Math.round(screenWidthDp() * 25f / 100f))));
        animatePanelWidth(targetWidth, true, wasOpen ? 0 : targetWidth);
    }

    private void animatePanelWidth(int targetWidth, boolean opening, int startWidth) {
        if (panelAnimator != null) panelAnimator.cancel();
        int from = settingsDrawer.getLayoutParams().width;
        if (from < 0) from = 0;
        final int destination = opening ? targetWidth : 0;
        final int start = (opening && from == 0) ? 0 : from;
        if (opening && start == 0) settingsDrawer.setTranslationX(targetWidth);
        panelAnimator = ValueAnimator.ofInt(start, destination);
        panelAnimator.setDuration(300);
        panelAnimator.setInterpolator(new DecelerateInterpolator());
        panelAnimator.addUpdateListener(animation -> {
            int width = (Integer) animation.getAnimatedValue();
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) settingsDrawer.getLayoutParams();
            params.width = width;
            settingsDrawer.setLayoutParams(params);
            float fraction = targetWidth == 0 ? 1f : Math.min(1f, width / (float) targetWidth);
            settingsDrawer.setTranslationX(opening ? targetWidth * (1f - fraction) : targetWidth * fraction);
            settingsDrawer.setAlpha(.72f + .28f * fraction);
        });
        panelAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (!opening && !drawerOpen) {
                    settingsDrawer.setVisibility(View.GONE);
                    drawerScrim.setVisibility(View.GONE);
                    settingsDrawer.setTranslationX(0);
                    if (restoreFocus != null && restoreFocus.isShown()) restoreFocus.requestFocus();
                    restoreFocus = null;
                    currentPanel = "";
                }
            }
        });
        panelAnimator.start();
        if (opening && !panelFocusables.isEmpty()) panelFocusables.get(0).post(() -> {
            if (drawerOpen && !panelFocusables.isEmpty()) panelFocusables.get(0).requestFocus();
        });
    }

    private void buildPanelContents(String type) {
        if ("wifi".equals(type)) buildWifiPanel();
        else if ("customize".equals(type)) buildCustomizePanel();
        else buildQuickPanel();
    }

    private void buildWifiPanel() {
        settingsDrawer.addView(text("Wi-Fi", 27, Color.WHITE, true));
        settingsDrawer.addView(text("Internet connection", 14, Color.LTGRAY, false));
        addPanelLabel("NETWORK CONTROLS");
        TextView explanation = text("Open the TV's secure Internet panel to turn Wi-Fi on or off, scan nearby networks, and choose a network.", 15, Color.WHITE, false);
        explanation.setLineSpacing(dp(4), 1f);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(-1, -2);
        ep.topMargin = dp(8);
        ep.bottomMargin = dp(14);
        settingsDrawer.addView(explanation, ep);
        Button open = panelButton("◉  Open Wi-Fi and network list");
        addPanelButton(open, dp(56));
        open.setOnClickListener(v -> openInternetPanel());
        TextView note = text("Android/TV firmware owns Wi-Fi scanning and connection security. This launcher cannot safely replace its protected controls.", 12, Color.LTGRAY, false);
        note.setLineSpacing(dp(3), 1f);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2);
        noteParams.topMargin = dp(14);
        settingsDrawer.addView(note, noteParams);
    }

    private void buildQuickPanel() {
        settingsDrawer.addView(text("Quick settings", 24, Color.WHITE, true));
        settingsDrawer.addView(text("Device controls and shortcuts", 13, Color.LTGRAY, false));
        addPanelLabel("CONNECTIVITY");
        addPanelButton(makeActionButton("◉  Internet / Wi-Fi", this::openInternetPanel), dp(54));
        addPanelLabel("DEVICE");
        addPanelButton(makeActionButton("◈  Bluetooth", () -> launchSettings(Settings.ACTION_BLUETOOTH_SETTINGS)), dp(54));
        addPanelButton(makeActionButton("▣  Display", () -> launchSettings(Settings.ACTION_DISPLAY_SETTINGS)), dp(54));
        addPanelLabel("SOUND & SYSTEM");
        addPanelButton(makeActionButton("♫  Sound", () -> launchSettings(Settings.ACTION_SOUND_SETTINGS)), dp(54));
        addPanelButton(makeActionButton("⚙  All settings", () -> launchSettings(Settings.ACTION_SETTINGS)), dp(54));
    }

    private void buildCustomizePanel() {
        settingsDrawer.addView(text("Customize", 24, Color.WHITE, true));
        addPanelLabel("USER NAME");
        Button name = panelButton("✎  " + userName());
        addPanelButton(name, dp(44));
        name.setOnClickListener(v -> showUserNameDialog());
        addPanelLabel("ACCENT");
        addChoiceGrid(new String[]{"Blue", "Magenta", "Purple", "Emerald", "Orange"}, new String[]{"royal", "magenta", "purple", "emerald", "sunset"}, "accent");
        addPanelLabel("ICON SHAPE");
        addChoiceGrid(new String[]{"Original", "Circle", "Rounded"}, new String[]{"original", "circle", "rounded"}, "shape");
        addPanelLabel("APPS PER ROW");
        addChoiceGrid(new String[]{"4", "5", "6", "7", "8"}, new String[]{"4", "5", "6", "7", "8"}, "columns");
        addPanelLabel("THEME");
        addChoiceGrid(new String[]{"Default", "Neon", "Ocean", "Sunset", "Forest", "Glossy"}, new String[]{"default", "neon", "ocean", "sunset", "forest", "glossy"}, "theme");
        addPanelLabel("WALLPAPER");
        Button wallpaper = panelButton("Choose wallpaper…");
        addPanelButton(wallpaper, dp(44));
        wallpaper.setOnClickListener(v -> chooseWallpaper());
    }

    private void buildPanelFooter() {
        Button close = panelButton("Close  ·  Back");
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(44));
        p.topMargin = dp(8);
        settingsDrawer.addView(close, p);
        close.setOnClickListener(v -> closePanel());
        close.setOnKeyListener((v, key, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && key == KeyEvent.KEYCODE_DPAD_LEFT) {
                closePanel(); return true;
            }
            return false;
        });
        panelFocusables.add(close);
        linkPanelFocus();
    }

    private void addPanelLabel(String label) {
        TextView t = text(label, 11, accent(), true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(12);
        p.bottomMargin = dp(5);
        settingsDrawer.addView(t, p);
    }

    private void addPanelButton(Button button, int height) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, height);
        p.bottomMargin = dp(6);
        settingsDrawer.addView(button, p);
        panelFocusables.add(button);
    }

    private Button makeActionButton(String label, Runnable action) {
        Button b = panelButton(label);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private Button panelButton(String label) {
        Button b = pill(label, false);
        b.setId(View.generateViewId());
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        b.setPadding(dp(12), 0, dp(8), 0);
        b.setBackground(glassButtonBackground(false));
        b.setOnFocusChangeListener((v, focused) -> {
            v.animate().cancel();
            v.animate().scaleX(focused ? 1.035f : 1f).scaleY(focused ? 1.035f : 1f)
                    .setDuration(160).setInterpolator(new DecelerateInterpolator()).start();
            v.setElevation(focused ? dp(9) : dp(1));
            v.setBackground(glassButtonBackground(focused));
        });
        return b;
    }

    private GradientDrawable glassButtonBackground(boolean focused) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{focused ? Color.rgb(76, 130, 143) : Color.argb(182, 87, 104, 103),
                        focused ? Color.rgb(24, 81, 80) : Color.argb(160, 45, 64, 65)});
        d.setCornerRadius(dp(18));
        d.setStroke(dp(focused ? 2 : 1), focused ? Color.rgb(153, 235, 231) : Color.argb(190, 194, 214, 209));
        return d;
    }

    private void linkPanelFocus() {
        for (int i = 0; i < panelFocusables.size(); i++) {
            View v = panelFocusables.get(i);
            v.setNextFocusLeftId(v.getId());
            v.setNextFocusRightId(v.getId());
            if (i > 0) v.setNextFocusUpId(panelFocusables.get(i - 1).getId());
            else v.setNextFocusUpId(v.getId());
            if (i + 1 < panelFocusables.size()) v.setNextFocusDownId(panelFocusables.get(i + 1).getId());
            else v.setNextFocusDownId(v.getId());
        }
    }

    private void addChoiceGrid(String[] labels, String[] values, String type) {
        LinearLayout row = null;
        for (int i = 0; i < labels.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(58));
                rp.bottomMargin = dp(4);
                settingsDrawer.addView(row, rp);
            }
            String value = values[i];
            Button choice = panelButton(labels[i]);
            choice.setGravity(Gravity.CENTER);
            boolean selected = isChoiceSelected(type, value);
            if (selected) choice.setBackground(glassChoiceSelected());
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, -1, 1);
            cp.setMargins(dp(3), dp(2), dp(3), dp(2));
            row.addView(choice, cp);
            panelFocusables.add(choice);
            choice.setOnClickListener(v -> {
                applyChoice(type, value);
                openPanel("customize");
            });
        }
    }

    private GradientDrawable glassChoiceSelected() {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(63, 143, 125), Color.rgb(26, 91, 77)});
        d.setCornerRadius(dp(18));
        d.setStroke(dp(2), accent());
        return d;
    }

    private boolean isChoiceSelected(String type, String value) {
        switch (type) {
            case "accent": return prefs().getInt(PREF_ACCENT, Color.rgb(45, 190, 220)) == accentColor(value);
            case "shape": return prefs().getString(PREF_ICON_SHAPE, "original").equals(value);
            case "columns": return prefs().getInt(PREF_COLUMNS, 5) == Integer.parseInt(value);
            case "theme": return theme().equals(value);
            default: return false;
        }
    }

    private void applyChoice(String type, String value) {
        SharedPreferences.Editor e = prefs().edit();
        switch (type) {
            case "accent": e.putInt(PREF_ACCENT, accentColor(value)); break;
            case "shape": e.putString(PREF_ICON_SHAPE, value); break;
            case "columns": e.putInt(PREF_COLUMNS, Integer.parseInt(value)); break;
            case "theme": e.putString("launcher_theme", value); break;
        }
        e.apply();
    }

    private int accentColor(String name) {
        switch (name) {
            case "magenta": return Color.rgb(232, 72, 174);
            case "purple": return Color.rgb(165, 105, 255);
            case "emerald": return Color.rgb(46, 190, 132);
            case "sunset": return Color.rgb(255, 145, 75);
            default: return Color.rgb(45, 190, 220);
        }
    }

    private int themeColor(String name) {
        switch (name) {
            case "neon": return Color.rgb(113, 48, 191);
            case "ocean": return Color.rgb(41, 170, 198);
            case "sunset": return Color.rgb(194, 80, 75);
            case "forest": return Color.rgb(46, 126, 78);
            case "glossy": return Color.rgb(100, 160, 225);
            default: return Color.rgb(72, 80, 96);
        }
    }

    private void showUserNameDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(userName());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this).setTitle("Launcher user name").setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    prefs().edit().putString(PREF_USER_NAME, value.isEmpty() ? "Shree" : value).apply();
                    openPanel("customize");
                }).show();
    }

    private void openInternetPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try { startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)); return; }
            catch (ActivityNotFoundException | SecurityException ignored) { }
        }
        try { startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Wi-Fi controls are not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchSettings(String action) {
        try { startActivity(new Intent(action)); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void chooseWallpaper() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        try { startActivityForResult(pick, WALLPAPER_REQUEST); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Image picker unavailable", Toast.LENGTH_SHORT).show(); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WALLPAPER_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (SecurityException ignored) { }
            prefs().edit().putString(PREF_WALLPAPER, uri.toString()).apply();
            buildScreen();
            renderApps();
        }
    }

    private void closePanel() {
        if (!drawerOpen) return;
        drawerOpen = false;
        int width = settingsDrawer.getWidth();
        if (width <= 0) width = Math.max(dp(270), dp(Math.round(screenWidthDp() * .23f)));
        animatePanelWidth(width, false, width);
    }

    private void launchApp(ResolveInfo app) {
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setComponent(new ComponentName(app.activityInfo.packageName, app.activityInfo.name));
        try { startActivity(launch); }
        catch (ActivityNotFoundException e) { Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show(); }
    }

    private Button pill(String label, boolean selected) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(selected ? Color.rgb(5, 23, 35) : Color.WHITE);
        b.setBackground(roundRect(selected ? accent() : surfaceColor(), dp(22)));
        b.setFocusable(true);
        b.setOnFocusChangeListener((v, focused) -> {
            if (!selected) v.setBackground(roundRect(focused ? focusSurfaceColor() : surfaceColor(), dp(22), focused ? accent() : Color.rgb(39, 77, 93)));
        });
        return b;
    }

    private void linkHorizontal(List<? extends View> views) {
        for (int i = 0; i < views.size(); i++) {
            if (i > 0) views.get(i).setNextFocusLeftId(views.get(i - 1).getId());
            if (i + 1 < views.size()) views.get(i).setNextFocusRightId(views.get(i + 1).getId());
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable roundRect(int color, int radius, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), strokeColor);
        return d;
    }

    private GradientDrawable roundRect(int color, int radius) {
        return roundRect(color, radius, Color.rgb(39, 77, 93));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
