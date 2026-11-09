package com.example.simpletvlauncher;

import android.app.Activity;
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
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(13, 16, 23);
    private static final int SURFACE = Color.rgb(29, 34, 45);
    private static final int SURFACE_HIGHLIGHT = Color.rgb(47, 56, 73);
    private static final int WALLPAPER_REQUEST = 41;
    private static final String PREFS = "launcher_preferences";
    private static final String PREF_ACCENT = "accent_color";
    private static final String PREF_ICON_SHAPE = "icon_shape";
    private static final String PREF_BADGE = "icon_badge";
    private static final String PREF_COLUMNS = "grid_columns";
    private static final String PREF_CATEGORY = "app_category";
    private static final String PREF_WALLPAPER = "wallpaper_uri";
    private static final String PREF_FAVORITES = "favorites";

    private FrameLayout root;
    private LinearLayout appGrid;
    private LinearLayout favoritesRow;
    private LinearLayout settingsDrawer;
    private View drawerScrim;
    private boolean drawerOpen;
    private String selectedCategory = "All";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        buildScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        if (appGrid != null) populateApps();
    }

    @Override public void onBackPressed() {
        if (drawerOpen) closeDrawer(); else super.onBackPressed();
    }

    private SharedPreferences prefs() { return getSharedPreferences(PREFS, MODE_PRIVATE); }
    private int accent() { return prefs().getInt(PREF_ACCENT, Color.rgb(83, 139, 255)); }

    private void buildScreen() {
        root = new FrameLayout(this);
        String wallpaper = prefs().getString(PREF_WALLPAPER, null);
        if (wallpaper != null) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try { image.setImageURI(Uri.parse(wallpaper)); } catch (RuntimeException ignored) { }
            root.addView(image, new FrameLayout.LayoutParams(-1, -1));
            View shade = new View(this);
            shade.setBackgroundColor(0xB80D1017);
            root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        } else root.setBackgroundColor(BACKGROUND);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(48), dp(22), dp(48), dp(22));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("NOVA", 16, accent(), true);
        brand.setLetterSpacing(.15f);
        top.addView(brand);
        Button apps = pill("Apps", true);
        LinearLayout.LayoutParams appsParams = new LinearLayout.LayoutParams(-2, dp(46));
        appsParams.leftMargin = dp(30);
        top.addView(apps, appsParams);
        apps.setFocusable(false);
        View gap = new View(this);
        top.addView(gap, new LinearLayout.LayoutParams(0, 1, 1));
        top.addView(text(new SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(new Date()),
                15, Color.WHITE, false));
        Button customize = pill("✦  Customize", false);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(46));
        cp.leftMargin = dp(14); top.addView(customize, cp);
        customize.setOnClickListener(v -> openDrawer(true));
        Button quick = pill("⚙  Quick settings", false);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-2, dp(46));
        qp.leftMargin = dp(9); top.addView(quick, qp);
        quick.setOnClickListener(v -> openDrawer(false));
        page.addView(top);

        TextView heading = text("Your apps", 32, Color.WHITE, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
        hp.topMargin = dp(30); page.addView(heading, hp);

        LinearLayout categoryBar = new LinearLayout(this);
        categoryBar.setGravity(Gravity.CENTER_VERTICAL);
        String[] categories = {"All", "Favorites", "Media", "Games", "Other"};
        for (String category : categories) {
            Button tab = pill(category, category.equals(selectedCategory));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, dp(42));
            tp.rightMargin = dp(8); categoryBar.addView(tab, tp);
            tab.setOnClickListener(v -> { selectedCategory = category; populateApps(); });
        }
        LinearLayout.LayoutParams cbp = new LinearLayout.LayoutParams(-1, -2);
        cbp.topMargin = dp(14); cbp.bottomMargin = dp(12); page.addView(categoryBar, cbp);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        favoritesRow = new LinearLayout(this);
        favoritesRow.setOrientation(LinearLayout.HORIZONTAL);
        content.addView(favoritesRow);
        appGrid = new LinearLayout(this);
        appGrid.setOrientation(LinearLayout.VERTICAL);
        content.addView(appGrid);
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(0x99000000);
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(v -> closeDrawer());
        root.addView(drawerScrim, new FrameLayout.LayoutParams(-1, -1));
        settingsDrawer = new LinearLayout(this);
        settingsDrawer.setOrientation(LinearLayout.VERTICAL);
        settingsDrawer.setBackground(roundRect(Color.rgb(25, 30, 40), dp(26)));
        settingsDrawer.setTranslationX(dp(430));
        root.addView(settingsDrawer, new FrameLayout.LayoutParams(dp(410), -1, Gravity.END));
        setContentView(root);
        populateApps();
    }

    private void populateApps() {
        if (appGrid == null || favoritesRow == null) return;
        appGrid.removeAllViews(); favoritesRow.removeAllViews();
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> unique = new LinkedHashMap<>();
        addApps(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique);
        addApps(pm, Intent.CATEGORY_LAUNCHER, unique);
        List<ResolveInfo> all = new ArrayList<>(unique.values());
        Collections.sort(all, Comparator.comparing(x -> x.loadLabel(pm).toString().toLowerCase(Locale.ROOT)));
        List<String> favorites = new ArrayList<>(prefs().getStringSet(PREF_FAVORITES,
                Collections.<String>emptySet()));
        List<ResolveInfo> favoriteApps = new ArrayList<>();
        List<ResolveInfo> filtered = new ArrayList<>();
        for (ResolveInfo app : all) {
            String pkg = app.activityInfo.packageName;
            if (favorites.contains(pkg)) favoriteApps.add(app);
            if (matchesCategory(app, selectedCategory, favorites)) filtered.add(app);
        }
        if (!"Favorites".equals(selectedCategory) && !favoriteApps.isEmpty()) {
            TextView label = text("FAVORITES", 13, accent(), true);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8); favoritesRow.addView(label, lp);
            for (ResolveInfo app : favoriteApps) favoritesRow.addView(makeTile(app, pm, true),
                    new LinearLayout.LayoutParams(dp(142), dp(132)));
            LinearLayout.LayoutParams fp = (LinearLayout.LayoutParams) favoritesRow.getLayoutParams();
            if (fp != null) fp.bottomMargin = dp(12);
        }
        int columns = Math.max(3, Math.min(8, prefs().getInt(PREF_COLUMNS, 5)));
        if (filtered.isEmpty()) {
            TextView empty = text("No apps in this category yet. Long-press an app tile to add it to Favorites.",
                    17, Color.LTGRAY, false);
            appGrid.addView(empty); return;
        }
        for (int start = 0; start < filtered.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.bottomMargin = dp(12); appGrid.addView(row, rp);
            for (int i = start; i < Math.min(start + columns, filtered.size()); i++) {
                ResolveInfo app = filtered.get(i);
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(136), 1);
                tileParams.setMargins(dp(5), dp(4), dp(5), dp(4));
                row.addView(makeTile(app, pm, false), tileParams);
            }
        }
    }

    private boolean matchesCategory(ResolveInfo app, String category, List<String> favorites) {
        if ("All".equals(category)) return true;
        if ("Favorites".equals(category)) return favorites.contains(app.activityInfo.packageName);
        String value = (app.activityInfo.packageName + " " + app.loadLabel(getPackageManager()))
                .toLowerCase(Locale.ROOT);
        if ("Media".equals(category)) return containsAny(value,
                "tv", "video", "movie", "music", "youtube", "netflix", "prime", "disney", "plex", "player");
        if ("Games".equals(category)) return containsAny(value,
                "game", "play", "arcade", "racing", "minecraft", "stadia");
        return !matchesCategory(app, "Media", favorites) && !matchesCategory(app, "Games", favorites);
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }

    private View makeTile(ResolveInfo info, PackageManager pm, boolean compact) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.setBackground(roundRect(SURFACE, dp(20)));
        tile.setFocusable(true);
        tile.setOnFocusChangeListener((v, focused) -> v.setBackground(roundRect(
                focused ? SURFACE_HIGHLIGHT : SURFACE, dp(20))));
        FrameLayout iconFrame = new FrameLayout(this);
        int size = dp(compact ? 48 : 56);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(info.loadIcon(pm));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        String shape = prefs().getString(PREF_ICON_SHAPE, "original");
        if (!"original".equals(shape)) {
            GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE);
            bg.setCornerRadius("circle".equals(shape) ? size / 2f : dp(13));
            icon.setBackground(bg); icon.setClipToOutline(true);
            icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        }
        iconFrame.addView(icon, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        String badge = prefs().getString(PREF_BADGE, "");
        if (!badge.isEmpty()) {
            TextView b = text(badge, 14, Color.WHITE, false); b.setGravity(Gravity.CENTER);
            GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(35, 40, 52));
            bg.setShape(GradientDrawable.OVAL); b.setBackground(bg);
            iconFrame.addView(b, new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.END | Gravity.BOTTOM));
        }
        tile.addView(iconFrame, new LinearLayout.LayoutParams(size, size));
        TextView name = text(info.loadLabel(pm).toString(), compact ? 12 : 14, Color.WHITE, true);
        name.setGravity(Gravity.CENTER); name.setMaxLines(2);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(8); tile.addView(name, np);
        tile.setOnClickListener(v -> launchApp(info));
        tile.setOnLongClickListener(v -> { toggleFavorite(info.activityInfo.packageName); return true; });
        return tile;
    }

    private void toggleFavorite(String packageName) {
        java.util.Set<String> current = new java.util.HashSet<>(prefs().getStringSet(PREF_FAVORITES,
                Collections.<String>emptySet()));
        if (!current.add(packageName)) current.remove(packageName);
        prefs().edit().putStringSet(PREF_FAVORITES, current).apply();
        Toast.makeText(this, current.contains(packageName) ? "Added to Favorites" : "Removed from Favorites",
                Toast.LENGTH_SHORT).show();
        populateApps();
    }

    private void addApps(PackageManager pm, String category, Map<String, ResolveInfo> out) {
        Intent query = new Intent(Intent.ACTION_MAIN); query.addCategory(category);
        for (ResolveInfo app : pm.queryIntentActivities(query, 0))
            out.put(app.activityInfo.packageName + "/" + app.activityInfo.name, app);
    }

    private void openDrawer(boolean customize) {
        if (drawerOpen) return;
        drawerOpen = true; drawerScrim.setVisibility(View.VISIBLE); settingsDrawer.removeAllViews();
        settingsDrawer.setPadding(dp(26), dp(28), dp(26), dp(24));
        if (customize) buildCustomizePanel(); else buildQuickPanel();
        settingsDrawer.animate().translationX(0).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void buildCustomizePanel() {
        settingsDrawer.addView(text("Personalize", 25, Color.WHITE, true));
        addLabel("Accent color");
        addColor("Royal Blue", Color.rgb(83,139,255)); addColor("Magenta", Color.rgb(232,72,174));
        addColor("Purple", Color.rgb(165,105,255)); addColor("Emerald", Color.rgb(46,190,132));
        addColor("Sunset Orange", Color.rgb(255,145,75));
        addLabel("Icon shape"); addStringChoice("Original", PREF_ICON_SHAPE, "original");
        addStringChoice("Circle", PREF_ICON_SHAPE, "circle");
        addStringChoice("Rounded square", PREF_ICON_SHAPE, "rounded");
        addLabel("Icon badge"); addStringChoice("None", PREF_BADGE, "");
        addStringChoice("Purple heart 💜", PREF_BADGE, "💜");
        addStringChoice("Blue heart 💙", PREF_BADGE, "💙");
        addStringChoice("Star ⭐", PREF_BADGE, "⭐");
        addLabel("Apps per row");
        for (int count : new int[]{4,5,6,7,8}) addStringChoice(String.valueOf(count) + " columns",
                PREF_COLUMNS, String.valueOf(count));
        Button wallpaper = pill("Choose wallpaper", false); addDrawerButton(wallpaper);
        wallpaper.setOnClickListener(v -> chooseWallpaper());
        addCloseButton();
    }

    private void buildQuickPanel() {
        settingsDrawer.addView(text("Quick settings", 25, Color.WHITE, true));
        addLabel("Device settings shortcuts");
        addSetting("Network & Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        addSetting("Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        addSetting("Display", Settings.ACTION_DISPLAY_SETTINGS);
        addSetting("Sound", Settings.ACTION_SOUND_SETTINGS);
        addSetting("All device settings", Settings.ACTION_SETTINGS);
        addCloseButton();
    }

    private void addLabel(String label) {
        TextView t = text(label.toUpperCase(Locale.ROOT), 11, accent(), true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(10); p.bottomMargin = dp(4); settingsDrawer.addView(t, p);
    }

    private void addColor(String label, int color) {
        Button b = pill("●   " + label, false); b.setTextColor(color); addDrawerButton(b);
        b.setOnClickListener(v -> { prefs().edit().putInt(PREF_ACCENT, color).apply(); refresh(); });
    }

    private void addStringChoice(String label, String key, String value) {
        Button b = pill(label, false); addDrawerButton(b);
        b.setOnClickListener(v -> {
            if (PREF_COLUMNS.equals(key)) prefs().edit().putInt(key, Integer.parseInt(value)).apply();
            else prefs().edit().putString(key, value).apply();
            refresh();
        });
    }

    private void addSetting(String label, String action) {
        Button b = pill(label, false); addDrawerButton(b);
        b.setOnClickListener(v -> { try { startActivity(new Intent(action)); }
            catch (ActivityNotFoundException | SecurityException e) {
                Toast.makeText(this, "Not available on this device", Toast.LENGTH_SHORT).show(); }});
    }

    private void addDrawerButton(Button b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(38));
        p.bottomMargin = dp(3); settingsDrawer.addView(b, p);
    }

    private void addCloseButton() {
        View spacer = new View(this); settingsDrawer.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        Button close = pill("Close", false); settingsDrawer.addView(close, new LinearLayout.LayoutParams(-1, dp(44)));
        close.setOnClickListener(v -> closeDrawer());
    }

    private void chooseWallpaper() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE); pick.setType("image/*");
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
            refresh();
        }
    }

    private void refresh() { drawerOpen = false; buildScreen(); }
    private void closeDrawer() {
        if (!drawerOpen) return; drawerOpen = false;
        settingsDrawer.animate().translationX(dp(430)).setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> drawerScrim.setVisibility(View.GONE)).start();
    }
    private void launchApp(ResolveInfo app) {
        Intent launch = new Intent(Intent.ACTION_MAIN); launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setComponent(new ComponentName(app.activityInfo.packageName, app.activityInfo.name));
        try { startActivity(launch); } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show(); }
    }
    private Button pill(String label, boolean selected) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(selected ? Color.rgb(16,22,33) : Color.WHITE);
        b.setBackground(roundRect(selected ? accent() : SURFACE, dp(22)));
        b.setFocusable(true); b.setOnFocusChangeListener((v, f) -> {
            if (!selected) v.setBackground(roundRect(f ? SURFACE_HIGHLIGHT : SURFACE, dp(22))); });
        return b;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius);
        d.setStroke(dp(1), Color.rgb(55,64,81)); return d;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
