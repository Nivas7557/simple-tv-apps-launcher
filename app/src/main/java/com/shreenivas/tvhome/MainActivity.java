package com.shreenivas.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
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
    private final List<View> drawerFocusables = new ArrayList<>();
    private FrameLayout root;
    private LinearLayout favoritesRow, appGrid, settingsDrawer;
    private View drawerScrim, restoreFocus;
    private ScrollView drawerScroll;
    private boolean drawerOpen, scanInProgress;
    private long lastScan;
    private String selectedCategory = "All";

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
        if (drawerOpen) closeDrawer(); else super.onBackPressed();
    }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }
    private int accent() { return prefs().getInt(PREF_ACCENT, Color.rgb(45, 190, 220)); }
    private String userName() { return prefs().getString(PREF_USER_NAME, "Shree"); }
    private String theme() { return prefs().getString("launcher_theme", "ocean"); }

    private void buildScreen() {
        root = new FrameLayout(this);
        String wallpaper = prefs().getString(PREF_WALLPAPER, null);
        if (wallpaper != null) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try { image.setImageURI(Uri.parse(wallpaper)); } catch (RuntimeException ignored) { }
            root.addView(image, new FrameLayout.LayoutParams(-1, -1));
            View shade = new View(this); shade.setBackgroundColor(0xB8091725);
            root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        } else root.setBackgroundColor(Color.rgb(7, 16, 27));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(48), dp(20), dp(48), dp(20));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(18), dp(7), dp(12), dp(7));
        top.setBackground(roundRect(Color.argb(150, 12, 31, 49), dp(24), Color.argb(90, 88, 174, 207)));
        TextView brand = text(userName(), 17, accent(), true); top.addView(brand);
        Button customize = pill("✦  Customize", false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(44)); cp.leftMargin = dp(24);
        top.addView(customize, cp); customize.setOnClickListener(v -> openDrawer(true));
        View gap = new View(this); top.addView(gap, new LinearLayout.LayoutParams(0, 1, 1));
        top.addView(text(new SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(new java.util.Date()),
                15, Color.WHITE, false));
        Button quick = pill("⚙  Quick settings", false);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-2, dp(44)); qp.leftMargin = dp(14);
        top.addView(quick, qp); quick.setOnClickListener(v -> openDrawer(false));
        top.setId(View.generateViewId()); page.addView(top);

        page.addView(text("Your apps", 31, Color.WHITE, true), new LinearLayout.LayoutParams(-1, -2));
        LinearLayout tabs = new LinearLayout(this); tabs.setGravity(Gravity.CENTER_VERTICAL);
        String[] categories = {"All", "Favorites", "Media", "Games", "Other"};
        List<Button> tabButtons = new ArrayList<>();
        for (String category : categories) {
            Button tab = pill(category, category.equals(selectedCategory)); tab.setId(View.generateViewId());
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, dp(42)); tp.rightMargin = dp(8);
            tabs.addView(tab, tp); tabButtons.add(tab);
            tab.setOnFocusChangeListener((v, focused) -> {
                v.setBackground(tabBackground(category.equals(selectedCategory), focused));
                if (focused && !category.equals(selectedCategory)) {
                    selectedCategory = category;
                    updateTabs(tabButtons, categories);
                    renderApps();
                }
            });
            tab.setOnClickListener(v -> { selectedCategory = category; updateTabs(tabButtons, categories); renderApps(); });
        }
        linkHorizontal(tabButtons);
        LinearLayout.LayoutParams tabsParams = new LinearLayout.LayoutParams(-1, -2);
        tabsParams.topMargin = dp(16); tabsParams.bottomMargin = dp(10); page.addView(tabs, tabsParams);

        ScrollView appScroll = new ScrollView(this); appScroll.setClipToPadding(false); appScroll.setFillViewport(false);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        favoritesRow = new LinearLayout(this); favoritesRow.setOrientation(LinearLayout.HORIZONTAL);
        favoritesRow.setGravity(Gravity.CENTER_VERTICAL); content.addView(favoritesRow);
        appGrid = new LinearLayout(this); appGrid.setOrientation(LinearLayout.VERTICAL); content.addView(appGrid);
        appScroll.addView(content); page.addView(appScroll, new LinearLayout.LayoutParams(-1, 0, 1));

        drawerScrim = new View(this); drawerScrim.setBackgroundColor(0x99030A12); drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(v -> closeDrawer()); root.addView(drawerScrim, new FrameLayout.LayoutParams(-1, -1));
        settingsDrawer = new LinearLayout(this); settingsDrawer.setOrientation(LinearLayout.VERTICAL);
        settingsDrawer.setPadding(dp(20), dp(20), dp(20), dp(16));
        settingsDrawer.setBackground(roundRect(Color.rgb(10, 28, 45), dp(24), Color.rgb(37, 131, 165)));
        settingsDrawer.setElevation(dp(14)); settingsDrawer.setTranslationX(dp(500));
        int screenDp = Math.round(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        int drawerWidth = Math.max(dp(300), Math.min(dp(440), dp(Math.round(screenDp * .24f))));
        root.addView(settingsDrawer, new FrameLayout.LayoutParams(drawerWidth, -1, Gravity.END));
        setContentView(root);
        updateTabs(tabButtons, categories);
        renderApps();
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
            PackageManager pm = getPackageManager(); Map<String, ResolveInfo> unique = new LinkedHashMap<>();
            addApps(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique); addApps(pm, Intent.CATEGORY_LAUNCHER, unique);
            List<ResolveInfo> result = new ArrayList<>(unique.values());
            Collections.sort(result, Comparator.comparing(x -> x.loadLabel(pm).toString().toLowerCase(Locale.ROOT)));
            runOnUiThread(() -> {
                if (isFinishing()) return;
                appCache.clear(); appCache.addAll(result); scanInProgress = false; lastScan = System.currentTimeMillis(); renderApps();
            });
        }).start();
    }

    private void renderApps() {
        if (appGrid == null || favoritesRow == null) return;
        appGrid.removeAllViews(); favoritesRow.removeAllViews();
        PackageManager pm = getPackageManager();
        Set<String> favorites = new HashSet<>(prefs().getStringSet(PREF_FAVORITES, Collections.<String>emptySet()));
        List<ResolveInfo> favoriteApps = new ArrayList<>(), filtered = new ArrayList<>();
        for (ResolveInfo app : appCache) {
            if (favorites.contains(app.activityInfo.packageName)) favoriteApps.add(app);
            if (matchesCategory(app, selectedCategory, favorites)) filtered.add(app);
        }
        if (appCache.isEmpty()) { appGrid.addView(text(scanInProgress ? "Loading apps…" : "No apps found", 17, Color.LTGRAY, false)); return; }
        if (!"Favorites".equals(selectedCategory) && !favoriteApps.isEmpty()) {
            TextView label = text("FAVORITES", 13, accent(), true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.bottomMargin = dp(7); favoritesRow.addView(label, lp);
            for (ResolveInfo app : favoriteApps) favoritesRow.addView(makeTile(app, pm, true), new LinearLayout.LayoutParams(dp(140), dp(128)));
        }
        if (filtered.isEmpty()) { appGrid.addView(text("No apps in this category yet. Long-press an app tile to add it to Favorites.", 17, Color.LTGRAY, false)); return; }
        int columns = Math.max(3, Math.min(8, prefs().getInt(PREF_COLUMNS, 5)));
        List<List<View>> rows = new ArrayList<>();
        for (int start = 0; start < filtered.size(); start += columns) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.bottomMargin = dp(10); appGrid.addView(row, rp);
            List<View> rowViews = new ArrayList<>();
            for (int i = start; i < Math.min(start + columns, filtered.size()); i++) {
                View tile = makeTile(filtered.get(i), pm, false);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(136), 1); p.setMargins(dp(5), dp(4), dp(5), dp(4));
                row.addView(tile, p); rowViews.add(tile);
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
    private boolean containsAny(String value, String... words) { for (String w : words) if (value.contains(w)) return true; return false; }
    private void addApps(PackageManager pm, String category, Map<String, ResolveInfo> out) {
        Intent query = new Intent(Intent.ACTION_MAIN); query.addCategory(category);
        for (ResolveInfo app : pm.queryIntentActivities(query, 0)) out.put(app.activityInfo.packageName + "/" + app.activityInfo.name, app);
    }

    private View makeTile(ResolveInfo info, PackageManager pm, boolean compact) {
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8)); tile.setFocusable(true); tile.setId(View.generateViewId());
        tile.setBackground(appTileBackground(false));
        tile.setOnFocusChangeListener((v, focused) -> {
            v.animate().cancel(); v.animate().scaleX(focused ? 1.055f : 1f).scaleY(focused ? 1.055f : 1f).setDuration(85).start();
            v.setElevation(focused ? dp(9) : dp(1)); v.setBackground(appTileBackground(focused));
        });
        int size = dp(compact ? 48 : 56); ImageView icon = new ImageView(this); icon.setImageDrawable(info.loadIcon(pm));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        String shape = prefs().getString(PREF_ICON_SHAPE, "original");
        if (!"original".equals(shape)) {
            GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE);
            bg.setCornerRadius("circle".equals(shape) ? size / 2f : dp(13)); icon.setBackground(bg); icon.setClipToOutline(true);
            icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        }
        tile.addView(icon, new LinearLayout.LayoutParams(size, size));
        TextView name = text(info.loadLabel(pm).toString(), compact ? 12 : 14, Color.WHITE, true); name.setGravity(Gravity.CENTER); name.setMaxLines(2);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2); np.topMargin = dp(8); tile.addView(name, np);
        tile.setOnClickListener(v -> launchApp(info)); tile.setOnLongClickListener(v -> { toggleFavorite(info.activityInfo.packageName); return true; });
        return tile;
    }

    private GradientDrawable appTileBackground(boolean focused) {
        int top = focused ? focusSurfaceColor() : surfaceColor();
        int bottom = focused ? Color.rgb(20, 85, 112) : ("glossy".equals(theme()) ? Color.rgb(11, 25, 43) : top);
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{
                focused ? Color.rgb(53, 142, 171) : top, bottom});
        d.setCornerRadius(dp(20)); d.setStroke(dp(focused ? 3 : 1), focused ? accent() : Color.rgb(32, 67, 87)); return d;
    }
    private int surfaceColor() {
        switch (theme()) {
            case "neon": return Color.rgb(27, 21, 47); case "ocean": return Color.rgb(15, 39, 59);
            case "sunset": return Color.rgb(57, 34, 43); case "forest": return Color.rgb(27, 48, 39);
            case "glossy": return Color.rgb(24, 42, 61); default: return Color.rgb(20, 34, 48);
        }
    }
    private int focusSurfaceColor() {
        switch (theme()) {
            case "neon": return Color.rgb(76, 43, 112); case "ocean": return Color.rgb(24, 91, 119);
            case "sunset": return Color.rgb(112, 59, 66); case "forest": return Color.rgb(47, 91, 67);
            default: return Color.rgb(36, 76, 99);
        }
    }

    private void toggleFavorite(String packageName) {
        Set<String> current = new HashSet<>(prefs().getStringSet(PREF_FAVORITES, Collections.<String>emptySet()));
        if (!current.add(packageName)) current.remove(packageName);
        prefs().edit().putStringSet(PREF_FAVORITES, current).apply();
        Toast.makeText(this, current.contains(packageName) ? "Added to Favorites" : "Removed from Favorites", Toast.LENGTH_SHORT).show(); renderApps();
    }

    private void openDrawer(boolean customize) {
        if (drawerOpen) return; drawerOpen = true; restoreFocus = getCurrentFocus(); drawerFocusables.clear();
        drawerScrim.setVisibility(View.VISIBLE); settingsDrawer.removeAllViews();
        int screenDp = Math.round(getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density);
        int width = Math.max(dp(300), Math.min(dp(440), dp(Math.round(screenDp * .24f))));
        FrameLayout.LayoutParams drawerParams = (FrameLayout.LayoutParams) settingsDrawer.getLayoutParams(); drawerParams.width = width; settingsDrawer.setLayoutParams(drawerParams);
        drawerScroll = new ScrollView(this); drawerScroll.setFillViewport(true); drawerScroll.setClipToPadding(false);
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); drawerScroll.addView(panel);
        settingsDrawer.addView(drawerScroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (customize) buildCustomizePanel(panel); else buildQuickPanel(panel);
        Button close = drawerButton("Close  ·  Back", false); drawerFocusables.add(close);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(46)); closeParams.topMargin = dp(9);
        settingsDrawer.addView(close, closeParams); close.setOnClickListener(v -> closeDrawer());
        close.setOnKeyListener((v, key, e) -> e.getAction() == KeyEvent.ACTION_DOWN && (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_BACK) && closeDrawerHandled());
        linkDrawerFocus(); settingsDrawer.animate().cancel(); settingsDrawer.animate().translationX(0).setDuration(125).setInterpolator(new DecelerateInterpolator()).start();
        if (!drawerFocusables.isEmpty()) drawerFocusables.get(0).post(() -> { if (drawerOpen) drawerFocusables.get(0).requestFocus(); });
    }
    private boolean closeDrawerHandled() { closeDrawer(); return true; }

    private void buildQuickPanel(LinearLayout panel) {
        panel.addView(text("Quick settings", 24, Color.WHITE, true));
        panel.addView(text("Device controls & shortcuts", 13, Color.LTGRAY, false));
        addLabel(panel, "CONNECTIVITY"); addQuickPair(panel, "Internet & Wi-Fi", this::openInternetPanel, "Wi-Fi settings", () -> launchSettings(Settings.ACTION_WIFI_SETTINGS));
        addLabel(panel, "DEVICE"); addQuickPair(panel, "Bluetooth", () -> launchSettings(Settings.ACTION_BLUETOOTH_SETTINGS), "Display", () -> launchSettings(Settings.ACTION_DISPLAY_SETTINGS));
        addLabel(panel, "SOUND & SYSTEM"); addQuickPair(panel, "Sound", () -> launchSettings(Settings.ACTION_SOUND_SETTINGS), "All settings", () -> launchSettings(Settings.ACTION_SETTINGS));
    }
    private void addQuickPair(LinearLayout panel, String firstLabel, Runnable firstAction, String secondLabel, Runnable secondAction) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button first = drawerButton(firstLabel, false), second = drawerButton(secondLabel, false);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, dp(58), 1); a.rightMargin = dp(5);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, dp(58), 1); b.leftMargin = dp(5);
        row.addView(first, a); row.addView(second, b); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(58)); rp.bottomMargin = dp(5); panel.addView(row, rp);
        drawerFocusables.add(first); drawerFocusables.add(second); first.setOnClickListener(v -> firstAction.run()); second.setOnClickListener(v -> secondAction.run());
    }
    private Button drawerButton(String label, boolean selected) {
        Button b = pill(label, selected); b.setId(View.generateViewId()); b.setGravity(Gravity.CENTER_VERTICAL | Gravity.START); b.setPadding(dp(12), 0, dp(8), 0);
        b.setOnFocusChangeListener((v, focus) -> { v.animate().cancel(); v.animate().scaleX(focus ? 1.025f : 1f).scaleY(focus ? 1.025f : 1f).setDuration(75).start(); v.setElevation(focus ? dp(7) : dp(1)); v.setBackground(roundRect(focus ? focusSurfaceColor() : surfaceColor(), dp(14), focus ? accent() : Color.rgb(40, 78, 96))); }); return b;
    }
    private void linkDrawerFocus() {
        int closeIndex = drawerFocusables.size() - 1;
        for (int i = 0; i < closeIndex; i++) {
            View v = drawerFocusables.get(i);
            if (i % 2 == 0 && i + 1 < closeIndex) v.setNextFocusRightId(drawerFocusables.get(i + 1).getId());
            else if (i % 2 == 1) v.setNextFocusLeftId(drawerFocusables.get(i - 1).getId());
            int up = i - 2, down = i + 2;
            v.setNextFocusUpId(up >= 0 ? drawerFocusables.get(up).getId() : v.getId());
            v.setNextFocusDownId(down < closeIndex ? drawerFocusables.get(down).getId() : drawerFocusables.get(closeIndex).getId());
        }
        if (closeIndex >= 0) { View close = drawerFocusables.get(closeIndex); close.setNextFocusDownId(close.getId()); if (closeIndex > 0) close.setNextFocusUpId(drawerFocusables.get(closeIndex - 1).getId()); }
    }

    private void buildCustomizePanel(LinearLayout panel) {
        panel.addView(text("Customize launcher", 23, Color.WHITE, true));
        addLabel(panel, "USER NAME"); Button name = drawerButton("✎  " + userName(), false); addDrawerButton(panel, name); name.setOnClickListener(v -> showUserNameDialog());
        addLabel(panel, "ACCENT"); addChoiceGrid(panel, new String[]{"Blue", "Magenta", "Purple", "Emerald", "Orange"}, new String[]{"royal", "magenta", "purple", "emerald", "sunset"}, "accent");
        addLabel(panel, "ICON SHAPE"); addChoiceGrid(panel, new String[]{"Original", "Circle", "Rounded"}, new String[]{"original", "circle", "rounded"}, "shape");
        addLabel(panel, "APPS PER ROW"); addChoiceGrid(panel, new String[]{"4 columns", "5 columns", "6 columns", "7 columns", "8 columns"}, new String[]{"4", "5", "6", "7", "8"}, "columns");
        addLabel(panel, "THEME"); addChoiceGrid(panel, new String[]{"Default", "Neon", "Ocean", "Sunset", "Forest", "Glossy"}, new String[]{"default", "neon", "ocean", "sunset", "forest", "glossy"}, "theme");
        addLabel(panel, "WALLPAPER"); Button wallpaper = drawerButton("Choose wallpaper…", false); addDrawerButton(panel, wallpaper); wallpaper.setOnClickListener(v -> chooseWallpaper());
    }

    private void addChoiceGrid(LinearLayout panel, String[] labels, String[] values, String type) {
        List<List<View>> rows = new ArrayList<>();
        for (int start = 0; start < labels.length; start += 2) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.bottomMargin = dp(5); panel.addView(row, rp);
            List<View> views = new ArrayList<>();
            for (int i = start; i < Math.min(start + 2, labels.length); i++) {
                String value = values[i]; LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER); card.setFocusable(true); card.setId(View.generateViewId());
                boolean selected = isChoiceSelected(type, value); card.setBackground(choiceBackground(selected, false));
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(70), 1); cp.setMargins(dp(3), dp(2), dp(3), dp(2)); row.addView(card, cp);
                TextView sample = text("●", 20, previewColor(type, value), true); sample.setGravity(Gravity.CENTER); card.addView(sample, new LinearLayout.LayoutParams(-1, 0, 1));
                TextView caption = text(labels[i], 11, Color.WHITE, selected); caption.setGravity(Gravity.CENTER); card.addView(caption);
                card.setOnFocusChangeListener((v, focus) -> { v.animate().cancel(); v.animate().scaleX(focus ? 1.025f : 1f).scaleY(focus ? 1.025f : 1f).setDuration(75).start(); v.setBackground(choiceBackground(isChoiceSelected(type, value), focus)); });
                card.setOnClickListener(v -> { applyChoice(type, value); refreshCustomize(); }); views.add(card); drawerFocusables.add(card);
            }
            linkHorizontal(views); rows.add(views);
        }
        for (int r = 0; r < rows.size(); r++) for (int c = 0; c < rows.get(r).size(); c++) {
            View v = rows.get(r).get(c); if (r > 0) v.setNextFocusUpId(rows.get(r-1).get(Math.min(c, rows.get(r-1).size()-1)).getId());
            if (r+1 < rows.size()) v.setNextFocusDownId(rows.get(r+1).get(Math.min(c, rows.get(r+1).size()-1)).getId());
        }
    }
    private int previewColor(String type, String value) { if ("accent".equals(type)) return accentColor(value); if ("theme".equals(type)) return "ocean".equals(value) ? Color.rgb(41,170,198) : themeColor(value); return accent(); }
    private GradientDrawable choiceBackground(boolean selected, boolean focused) { return roundRect(focused ? focusSurfaceColor() : (selected ? Color.rgb(26,79,99) : surfaceColor()), dp(12), focused || selected ? accent() : Color.rgb(39,77,93)); }
    private boolean isChoiceSelected(String type, String value) {
        switch (type) { case "accent": return prefs().getInt(PREF_ACCENT, Color.rgb(45,190,220)) == accentColor(value); case "shape": return prefs().getString(PREF_ICON_SHAPE,"original").equals(value); case "columns": return prefs().getInt(PREF_COLUMNS,5) == Integer.parseInt(value); case "theme": return theme().equals(value); default: return false; }
    }
    private void applyChoice(String type, String value) {
        SharedPreferences.Editor e = prefs().edit(); switch (type) { case "accent": e.putInt(PREF_ACCENT,accentColor(value)); break; case "shape": e.putString(PREF_ICON_SHAPE,value); break; case "columns": e.putInt(PREF_COLUMNS,Integer.parseInt(value)); break; case "theme": e.putString("launcher_theme",value); break; } e.apply();
    }
    private int accentColor(String name) { switch(name) { case "magenta": return Color.rgb(232,72,174); case "purple": return Color.rgb(165,105,255); case "emerald": return Color.rgb(46,190,132); case "sunset": return Color.rgb(255,145,75); default: return Color.rgb(45,190,220); } }
    private int themeColor(String name) { switch(name) { case "neon": return Color.rgb(113,48,191); case "sunset": return Color.rgb(194,80,75); case "forest": return Color.rgb(46,126,78); case "glossy": return Color.rgb(100,160,225); default: return Color.rgb(70,80,96); } }
    private void refreshCustomize() { drawerOpen = false; buildScreen(); renderApps(); openDrawer(true); }
    private void showUserNameDialog() {
        EditText input = new EditText(this); input.setSingleLine(true); input.setText(userName()); input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this).setTitle("Launcher user name").setView(input).setNegativeButton("Cancel", null).setPositiveButton("Save", (d,w) -> { String s=input.getText().toString().trim(); prefs().edit().putString(PREF_USER_NAME,s.isEmpty()?"Shree":s).apply(); refreshCustomize(); }).show();
    }
    private void addLabel(LinearLayout panel, String label) { TextView t=text(label,11,accent(),true); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(10); p.bottomMargin=dp(4); panel.addView(t,p); }
    private void addDrawerButton(LinearLayout panel, Button b) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(42)); p.bottomMargin=dp(3); panel.addView(b,p); drawerFocusables.add(b); }
    private void openInternetPanel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { try { startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)); return; } catch (ActivityNotFoundException | SecurityException ignored) { } } launchSettings(Settings.ACTION_WIFI_SETTINGS); }
    private void launchSettings(String action) { try { startActivity(new Intent(action)); } catch (ActivityNotFoundException | SecurityException e) { Toast.makeText(this,"Not available on this device",Toast.LENGTH_SHORT).show(); } }
    private void chooseWallpaper() { Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT); pick.addCategory(Intent.CATEGORY_OPENABLE); pick.setType("image/*"); try { startActivityForResult(pick,WALLPAPER_REQUEST); } catch(ActivityNotFoundException e) { Toast.makeText(this,"Image picker unavailable",Toast.LENGTH_SHORT).show(); } }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==WALLPAPER_REQUEST && resultCode==RESULT_OK && data!=null && data.getData()!=null) { Uri uri=data.getData(); try { getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch(SecurityException ignored) { } prefs().edit().putString(PREF_WALLPAPER,uri.toString()).apply(); buildScreen(); renderApps(); } }

    private void closeDrawer() {
        if (!drawerOpen) return; drawerOpen = false; settingsDrawer.animate().cancel(); settingsDrawer.animate().translationX(settingsDrawer.getWidth()).setDuration(100).setInterpolator(new DecelerateInterpolator()).withEndAction(() -> { drawerScrim.setVisibility(View.GONE); if (restoreFocus != null && restoreFocus.isShown()) restoreFocus.requestFocus(); restoreFocus=null; }).start();
    }
    private void launchApp(ResolveInfo app) { Intent launch=new Intent(Intent.ACTION_MAIN); launch.addCategory(Intent.CATEGORY_LAUNCHER); launch.setComponent(new ComponentName(app.activityInfo.packageName,app.activityInfo.name)); try { startActivity(launch); } catch(ActivityNotFoundException e) { Toast.makeText(this,"Unable to open app",Toast.LENGTH_SHORT).show(); } }
    private Button pill(String label, boolean selected) { Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setTextColor(selected?Color.rgb(5,23,35):Color.WHITE); b.setBackground(tabBackground(selected,false)); b.setFocusable(true); b.setOnFocusChangeListener((v,f) -> { if(!selected) v.setBackground(tabBackground(false,f)); }); return b; }
    private void linkHorizontal(List<? extends View> views) { for(int i=0;i<views.size();i++) { if(i>0) views.get(i).setNextFocusLeftId(views.get(i-1).getId()); if(i+1<views.size()) views.get(i).setNextFocusRightId(views.get(i+1).getId()); } }
    private TextView text(String value,int size,int color,boolean bold) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private GradientDrawable roundRect(int color,int radius,int strokeColor) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); d.setStroke(dp(1),strokeColor); return d; }
    private GradientDrawable roundRect(int color,int radius) { return roundRect(color,radius,Color.rgb(39,77,93)); }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
}
