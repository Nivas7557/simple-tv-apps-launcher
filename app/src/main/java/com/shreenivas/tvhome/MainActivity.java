package com.shreenivas.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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
    private static final int WALLPAPER_REQUEST = 41;
    private static final String PREFS = "launcher_preferences";
    private static final String PREF_ACCENT = "accent_color";
    private static final String PREF_ICON_SHAPE = "icon_shape";
    private static final String PREF_BADGE = "icon_badge";
    private static final String PREF_COLUMNS = "grid_columns";
    private static final String PREF_WALLPAPER = "wallpaper_uri";
    private static final String PREF_WALLPAPER_X = "wallpaper_offset_x";
    private static final String PREF_WALLPAPER_Y = "wallpaper_offset_y";
    private static final String PREF_FAVORITES = "favorites";
    private static final String PREF_USER_NAME = "launcher_user_name";
    private static final String PREF_THEME = "launcher_theme";

    private FrameLayout root;
    private LinearLayout appGrid;
    private LinearLayout favoritesRow;
    private LinearLayout settingsDrawer;
    private View drawerScrim;
    private boolean drawerOpen;

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
    private String userName() { return prefs().getString(PREF_USER_NAME, "Shree"); }
    private String theme() { return prefs().getString(PREF_THEME, "default"); }

    private int pageBackground() {
        switch (theme()) {
            case "neon": return Color.rgb(8, 8, 18);
            case "ocean": return Color.rgb(8, 20, 32);
            case "sunset": return Color.rgb(29, 17, 25);
            case "forest": return Color.rgb(12, 25, 20);
            case "glossy": return Color.rgb(12, 16, 25);
            default: return Color.rgb(13, 16, 23);
        }
    }

    private int surfaceColor() {
        switch (theme()) {
            case "neon": return Color.rgb(27, 21, 47);
            case "ocean": return Color.rgb(20, 42, 59);
            case "sunset": return Color.rgb(57, 34, 43);
            case "forest": return Color.rgb(27, 48, 39);
            case "glossy": return Color.rgb(35, 44, 61);
            default: return Color.rgb(29, 34, 45);
        }
    }

    private int focusSurfaceColor() {
        switch (theme()) {
            case "neon": return Color.rgb(76, 43, 112);
            case "ocean": return Color.rgb(32, 82, 105);
            case "sunset": return Color.rgb(112, 59, 66);
            case "forest": return Color.rgb(47, 91, 67);
            default: return Color.rgb(47, 56, 73);
        }
    }

    private void buildScreen() {
        root = new FrameLayout(this);
        String wallpaper = prefs().getString(PREF_WALLPAPER, null);
        if (wallpaper != null) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try { image.setImageURI(Uri.parse(wallpaper)); } catch (RuntimeException ignored) { }
            root.addView(image, new FrameLayout.LayoutParams(-1, -1));
            image.post(() -> {
                image.setTranslationX(prefs().getFloat(PREF_WALLPAPER_X, 0f) * image.getWidth());
                image.setTranslationY(prefs().getFloat(PREF_WALLPAPER_Y, 0f) * image.getHeight());
            });
            View shade = new View(this);
            shade.setBackgroundColor(0xB80D1017);
            root.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        } else {
            root.setBackgroundColor(pageBackground());
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(48), dp(20), dp(48), dp(22));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brandBlock = new LinearLayout(this);
        brandBlock.setOrientation(LinearLayout.VERTICAL);
        TextView brand = text(userName(), 18, accent(), true);
        brand.setLetterSpacing(.12f);
        brandBlock.addView(brand);
        View brandAccent = new View(this);
        brandAccent.setBackground(roundRect(accent(), dp(2)));
        LinearLayout.LayoutParams accentLine = new LinearLayout.LayoutParams(-1, dp(3));
        accentLine.topMargin = dp(4);
        brandBlock.addView(brandAccent, accentLine);
        top.addView(brandBlock, new LinearLayout.LayoutParams(-2, -2));

        Button customize = pill("✦  Customize", false);
        View gap = new View(this);
        top.addView(gap, new LinearLayout.LayoutParams(0, 1, 1));
        top.addView(text(new SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(new Date()),
                15, Color.WHITE, false));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, dp(46));
        cp.leftMargin = dp(18);
        top.addView(customize, cp);
        customize.setOnClickListener(v -> openDrawer(true));
        Button quick = pill("⚙  Quick settings", false);
        LinearLayout.LayoutParams qp = new LinearLayout.LayoutParams(-2, dp(46));
        qp.leftMargin = dp(9);
        top.addView(quick, qp);
        quick.setOnClickListener(v -> openDrawer(false));
        page.addView(top);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        TextView favoritesTitle = text("Favorites", 25, Color.WHITE, true);
        LinearLayout.LayoutParams favoritesTitleParams = new LinearLayout.LayoutParams(-1, -2);
        favoritesTitleParams.topMargin = dp(28);
        favoritesTitleParams.bottomMargin = dp(9);
        content.addView(favoritesTitle, favoritesTitleParams);
        HorizontalScrollView favoritesScroll = new HorizontalScrollView(this);
        favoritesScroll.setHorizontalScrollBarEnabled(false);
        favoritesRow = new LinearLayout(this);
        favoritesRow.setOrientation(LinearLayout.HORIZONTAL);
        favoritesScroll.addView(favoritesRow);
        content.addView(favoritesScroll, new LinearLayout.LayoutParams(-1, dp(142)));

        TextView allTitle = text("All Apps", 25, Color.WHITE, true);
        LinearLayout.LayoutParams allTitleParams = new LinearLayout.LayoutParams(-1, -2);
        allTitleParams.topMargin = dp(18);
        allTitleParams.bottomMargin = dp(9);
        content.addView(allTitle, allTitleParams);
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
        root.addView(settingsDrawer, new FrameLayout.LayoutParams(dp(430), -1, Gravity.END));
        setContentView(root);
        populateApps();
    }

    private void populateApps() {
        if (appGrid == null || favoritesRow == null) return;
        appGrid.removeAllViews();
        favoritesRow.removeAllViews();
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> unique = new LinkedHashMap<>();
        addApps(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique);
        addApps(pm, Intent.CATEGORY_LAUNCHER, unique);
        List<ResolveInfo> all = new ArrayList<>(unique.values());
        Collections.sort(all, Comparator.comparing(x -> x.loadLabel(pm).toString().toLowerCase(Locale.ROOT)));
        List<String> favorites = new ArrayList<>(prefs().getStringSet(PREF_FAVORITES,
                Collections.<String>emptySet()));
        List<ResolveInfo> favoriteApps = new ArrayList<>();
        for (ResolveInfo app : all) {
            if (favorites.contains(app.activityInfo.packageName)) favoriteApps.add(app);
        }
        if (favoriteApps.isEmpty()) {
            TextView emptyFavorites = text("Long-press any app below to add it here.", 15,
                    Color.rgb(176, 184, 198), false);
            emptyFavorites.setGravity(Gravity.CENTER_VERTICAL);
            favoritesRow.addView(emptyFavorites, new LinearLayout.LayoutParams(-1, dp(100)));
        } else {
            for (ResolveInfo app : favoriteApps) {
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(dp(142), dp(132));
                tileParams.setMargins(0, 0, dp(10), 0);
                favoritesRow.addView(makeTile(app, pm, true), tileParams);
            }
        }

        int columns = Math.max(3, Math.min(8, prefs().getInt(PREF_COLUMNS, 5)));
        if (all.isEmpty()) {
            appGrid.addView(text("No launchable apps found.", 17, Color.LTGRAY, false));
            return;
        }
        for (int start = 0; start < all.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(12);
            appGrid.addView(row, rowParams);
            for (int i = start; i < Math.min(start + columns, all.size()); i++) {
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(136), 1);
                tileParams.setMargins(dp(5), dp(4), dp(5), dp(4));
                row.addView(makeTile(all.get(i), pm, false), tileParams);
            }
        }
    }

    private View makeTile(ResolveInfo info, PackageManager pm, boolean compact) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(8), dp(8), dp(8), dp(8));
        tile.setBackground(appTileBackground(false));
        tile.setFocusable(true);
        tile.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.045f : 1f).scaleY(focused ? 1.045f : 1f)
                    .setDuration(130).start();
            v.setElevation(focused ? dp(14) : dp(2));
            v.setBackground(appTileBackground(focused));
        });
        FrameLayout iconFrame = new FrameLayout(this);
        int size = dp(compact ? 48 : 56);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(info.loadIcon(pm));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        String shape = prefs().getString(PREF_ICON_SHAPE, "original");
        if (!"original".equals(shape)) {
            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setColor(Color.WHITE);
            iconBg.setCornerRadius("circle".equals(shape) ? size / 2f : dp(13));
            icon.setBackground(iconBg);
            icon.setClipToOutline(true);
            icon.setPadding(dp(4), dp(4), dp(4), dp(4));
        }
        iconFrame.addView(icon, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        String badge = prefs().getString(PREF_BADGE, "");
        if (!badge.isEmpty()) {
            TextView badgeView = text(badge, 14, Color.WHITE, false);
            badgeView.setGravity(Gravity.CENTER);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.rgb(35, 40, 52));
            badgeBg.setShape(GradientDrawable.OVAL);
            badgeView.setBackground(badgeBg);
            iconFrame.addView(badgeView,
                    new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.END | Gravity.BOTTOM));
        }
        tile.addView(iconFrame, new LinearLayout.LayoutParams(size, size));
        TextView name = text(info.loadLabel(pm).toString(), compact ? 12 : 14, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(8);
        tile.addView(name, nameParams);
        tile.setOnClickListener(v -> launchApp(info));
        tile.setOnLongClickListener(v -> {
            toggleFavorite(info.activityInfo.packageName);
            return true;
        });
        return tile;
    }

    private GradientDrawable appTileBackground(boolean focused) {
        int top = focused ? focusSurfaceColor() : surfaceColor();
        int bottom = theme().equals("glossy")
                ? (focused ? Color.rgb(63, 77, 103) : Color.rgb(21, 27, 39)) : top;
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, bottom});
        drawable.setCornerRadius(dp(20));
        drawable.setStroke(dp(focused ? 3 : 1), focused ? accent() : Color.rgb(55, 64, 81));
        return drawable;
    }

    private void toggleFavorite(String packageName) {
        java.util.Set<String> current = new java.util.HashSet<>(prefs().getStringSet(
                PREF_FAVORITES, Collections.<String>emptySet()));
        if (!current.add(packageName)) current.remove(packageName);
        prefs().edit().putStringSet(PREF_FAVORITES, current).apply();
        Toast.makeText(this, current.contains(packageName) ? "Added to Favorites" : "Removed from Favorites",
                Toast.LENGTH_SHORT).show();
        populateApps();
    }

    private void addApps(PackageManager pm, String category, Map<String, ResolveInfo> out) {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(category);
        for (ResolveInfo app : pm.queryIntentActivities(query, 0)) {
            out.put(app.activityInfo.packageName + "/" + app.activityInfo.name, app);
        }
    }

    private void openDrawer(boolean customize) {
        if (drawerOpen) return;
        drawerOpen = true;
        drawerScrim.setVisibility(View.VISIBLE);
        settingsDrawer.removeAllViews();
        settingsDrawer.setPadding(dp(24), dp(26), dp(24), dp(22));
        ScrollView scroll = new ScrollView(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        settingsDrawer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        scroll.addView(panel);
        if (customize) buildCustomizePanel(panel); else buildQuickPanel(panel);
        Button close = pill("Close", false);
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(-1, dp(44));
        closeParams.topMargin = dp(8);
        settingsDrawer.addView(close, closeParams);
        close.setOnClickListener(v -> closeDrawer());
        settingsDrawer.animate().translationX(0).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void buildCustomizePanel(LinearLayout panel) {
        panel.addView(text("Customize Home Launcher", 24, Color.WHITE, true));
        addLabel(panel, "User Name");
        Button name = pill("✎  " + userName(), false);
        addDrawerButton(panel, name);
        name.setOnClickListener(v -> showUserNameDialog());
        addLabel(panel, "Accent");
        addChoiceGrid(panel, new String[]{"Royal Blue", "Magenta", "Purple", "Emerald", "Sunset Orange"},
                new String[]{"royal", "magenta", "purple", "emerald", "sunset"}, "accent");
        addLabel(panel, "Icon Shape");
        addChoiceGrid(panel, new String[]{"Original", "Circle", "Rounded Square"},
                new String[]{"original", "circle", "rounded"}, "shape");
        addLabel(panel, "Apps Per Row");
        addChoiceGrid(panel, new String[]{"4 columns", "5 columns", "6 columns", "7 columns", "8 columns"},
                new String[]{"4", "5", "6", "7", "8"}, "columns");
        addLabel(panel, "Icon Badge");
        addChoiceGrid(panel, new String[]{"None", "Purple Heart", "Blue Heart", "Star"},
                new String[]{"", "💜", "💙", "⭐"}, "badge");
        addLabel(panel, "Themes");
        addChoiceGrid(panel, new String[]{"Default", "Neon", "Ocean", "Sunset", "Forest", "Glossy"},
                new String[]{"default", "neon", "ocean", "sunset", "forest", "glossy"}, "theme");
        addLabel(panel, "Wallpaper");
        Button wallpaper = pill("Choose wallpaper…", false);
        addDrawerButton(panel, wallpaper);
        wallpaper.setOnClickListener(v -> chooseWallpaper());
    }

    private void addChoiceGrid(LinearLayout panel, String[] labels, String[] values, String type) {
        for (int start = 0; start < labels.length; start += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(7);
            panel.addView(row, rowParams);
            for (int index = start; index < Math.min(start + 2, labels.length); index++) {
                String value = values[index];
                boolean selected = isChoiceSelected(type, value);
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setGravity(Gravity.CENTER);
                card.setPadding(dp(7), dp(7), dp(7), dp(7));
                card.setFocusable(true);
                card.setBackground(choiceCardBackground(selected, false));
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(0, dp(88), 1);
                cardParams.setMargins(dp(4), dp(3), dp(4), dp(3));
                row.addView(card, cardParams);
                View sample = makeChoicePreview(type, value);
                LinearLayout.LayoutParams sampleParams = new LinearLayout.LayoutParams(-1, dp(42));
                sampleParams.bottomMargin = dp(5);
                card.addView(sample, sampleParams);
                TextView caption = text(labels[index], 12, Color.WHITE, selected);
                caption.setGravity(Gravity.CENTER);
                card.addView(caption, new LinearLayout.LayoutParams(-1, -2));
                card.setOnFocusChangeListener((v, focused) -> {
                    v.animate().scaleX(focused ? 1.04f : 1f).scaleY(focused ? 1.04f : 1f)
                            .setDuration(120).start();
                    v.setElevation(focused ? dp(12) : dp(1));
                    v.setBackground(choiceCardBackground(isChoiceSelected(type, value), focused));
                });
                card.setOnClickListener(v -> {
                    applyChoice(type, value);
                    refreshAndReopenCustomize();
                });
            }
        }
    }

    private GradientDrawable choiceCardBackground(boolean selected, boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(focused ? focusSurfaceColor() : (selected ? blend(surfaceColor(), accent()) : surfaceColor()));
        drawable.setCornerRadius(dp(14));
        drawable.setStroke(dp(focused ? 3 : (selected ? 2 : 1)),
                focused || selected ? accent() : Color.rgb(55, 64, 81));
        return drawable;
    }

    private int blend(int base, int tint) {
        return Color.rgb((Color.red(base) + Color.red(tint)) / 2,
                (Color.green(base) + Color.green(tint)) / 2,
                (Color.blue(base) + Color.blue(tint)) / 2);
    }

    private View makeChoicePreview(String type, String value) {
        FrameLayout preview = new FrameLayout(this);
        int color;
        if ("accent".equals(type)) color = accentColor(value);
        else if ("theme".equals(type)) color = themePreviewColor(value);
        else color = accent();
        if ("columns".equals(type)) {
            TextView number = text(value, 22, Color.WHITE, true);
            number.setGravity(Gravity.CENTER);
            preview.addView(number, new FrameLayout.LayoutParams(-1, -1));
            return preview;
        }
        View sample = new View(this);
        GradientDrawable sampleBg;
        if ("theme".equals(type) && "glossy".equals(value)) {
            sampleBg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{Color.rgb(125, 165, 230), Color.rgb(34, 48, 76)});
        } else {
            sampleBg = new GradientDrawable();
            sampleBg.setColor("badge".equals(type) && value.isEmpty() ? Color.rgb(75, 82, 96) : color);
        }
        if ("shape".equals(type) && "circle".equals(value)) sampleBg.setShape(GradientDrawable.OVAL);
        else sampleBg.setCornerRadius("shape".equals(type) && "original".equals(value) ? dp(4) : dp(11));
        sample.setBackground(sampleBg);
        FrameLayout.LayoutParams sampleParams = new FrameLayout.LayoutParams(
                "shape".equals(type) ? dp(36) : -1, dp(34), Gravity.CENTER);
        if (!"shape".equals(type)) {
            sampleParams.leftMargin = dp(12);
            sampleParams.rightMargin = dp(12);
        }
        preview.addView(sample, sampleParams);
        if ("badge".equals(type) && !value.isEmpty()) {
            TextView badge = text(value, 18, Color.WHITE, true);
            badge.setGravity(Gravity.CENTER);
            preview.addView(badge, new FrameLayout.LayoutParams(-1, -1));
        }
        return preview;
    }

    private boolean isChoiceSelected(String type, String value) {
        switch (type) {
            case "accent": return prefs().getInt(PREF_ACCENT, Color.rgb(83, 139, 255)) == accentColor(value);
            case "shape": return prefs().getString(PREF_ICON_SHAPE, "original").equals(value);
            case "columns": return prefs().getInt(PREF_COLUMNS, 5) == Integer.parseInt(value);
            case "badge": return prefs().getString(PREF_BADGE, "").equals(value);
            case "theme": return theme().equals(value);
            default: return false;
        }
    }

    private void applyChoice(String type, String value) {
        SharedPreferences.Editor editor = prefs().edit();
        switch (type) {
            case "accent": editor.putInt(PREF_ACCENT, accentColor(value)); break;
            case "shape": editor.putString(PREF_ICON_SHAPE, value); break;
            case "columns": editor.putInt(PREF_COLUMNS, Integer.parseInt(value)); break;
            case "badge": editor.putString(PREF_BADGE, value); break;
            case "theme": editor.putString(PREF_THEME, value); break;
        }
        editor.apply();
    }

    private int accentColor(String name) {
        switch (name) {
            case "magenta": return Color.rgb(232, 72, 174);
            case "purple": return Color.rgb(165, 105, 255);
            case "emerald": return Color.rgb(46, 190, 132);
            case "sunset": return Color.rgb(255, 145, 75);
            default: return Color.rgb(83, 139, 255);
        }
    }

    private int themePreviewColor(String name) {
        switch (name) {
            case "neon": return Color.rgb(113, 48, 191);
            case "ocean": return Color.rgb(22, 111, 153);
            case "sunset": return Color.rgb(194, 80, 75);
            case "forest": return Color.rgb(46, 126, 78);
            case "glossy": return Color.rgb(83, 139, 255);
            default: return Color.rgb(72, 80, 96);
        }
    }

    private void refreshAndReopenCustomize() {
        drawerOpen = false;
        buildScreen();
        openDrawer(true);
    }

    private void showUserNameDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(userName());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        new AlertDialog.Builder(this).setTitle("Launcher user name")
                .setMessage("This name appears in the top-left of your launcher.")
                .setView(input).setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) value = "Shree";
                    prefs().edit().putString(PREF_USER_NAME, value).apply();
                    refreshAndReopenCustomize();
                }).show();
    }

    private void buildQuickPanel(LinearLayout panel) {
        panel.addView(text("Quick settings", 25, Color.WHITE, true));
        addLabel(panel, "Wi-Fi");
        addQuickPair(panel, "Wi-Fi toggle & networks", this::openInternetPanel,
                "Wi-Fi settings", () -> launchSettings(Settings.ACTION_WIFI_SETTINGS));
        addLabel(panel, "Device settings");
        addQuickPair(panel, "Bluetooth settings", () -> launchSettings(Settings.ACTION_BLUETOOTH_SETTINGS),
                "Display settings", () -> launchSettings(Settings.ACTION_DISPLAY_SETTINGS));
        addQuickPair(panel, "Sound settings", () -> launchSettings(Settings.ACTION_SOUND_SETTINGS),
                "All settings", () -> launchSettings(Settings.ACTION_SETTINGS));
    }

    private void addQuickPair(LinearLayout panel, String firstLabel, Runnable firstAction,
                              String secondLabel, Runnable secondAction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button first = pill(firstLabel, false);
        Button second = pill(secondLabel, false);
        LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        firstParams.rightMargin = dp(6);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, dp(58), 1);
        secondParams.leftMargin = dp(6);
        row.addView(first, firstParams);
        row.addView(second, secondParams);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(58));
        rowParams.bottomMargin = dp(8);
        panel.addView(row, rowParams);
        first.setOnClickListener(v -> firstAction.run());
        second.setOnClickListener(v -> secondAction.run());
    }

    private void openInternetPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startActivity(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY));
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) { }
        }
        launchSettings(Settings.ACTION_WIFI_SETTINGS);
    }

    private void launchSettings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Not available on this device", Toast.LENGTH_SHORT).show();
        }
    }

    private void addLabel(LinearLayout panel, String label) {
        TextView t = text(label, 13, accent(), true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(13);
        p.bottomMargin = dp(5);
        panel.addView(t, p);
    }

    private void addDrawerButton(LinearLayout panel, Button button) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(42));
        p.bottomMargin = dp(4);
        panel.addView(button, p);
    }

    private void chooseWallpaper() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        try {
            startActivityForResult(pick, WALLPAPER_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Image picker unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WALLPAPER_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { }
            showWallpaperPreview(uri);
        }
    }

    private void showWallpaperPreview(Uri uri) {
        Dialog dialog = new Dialog(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(14), dp(18), dp(14));
        layout.setBackground(roundRect(Color.rgb(22, 27, 38), dp(20)));
        TextView title = text("Wallpaper preview — position with arrows", 18, Color.WHITE, true);
        layout.addView(title, new LinearLayout.LayoutParams(-1, dp(36)));
        FrameLayout viewport = new FrameLayout(this);
        viewport.setClipToOutline(true);
        viewport.setBackground(roundRect(Color.BLACK, dp(14)));
        LinearLayout.LayoutParams viewportParams = new LinearLayout.LayoutParams(-1, 0, 1);
        viewportParams.topMargin = dp(8);
        viewportParams.bottomMargin = dp(8);
        layout.addView(viewport, viewportParams);
        ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setImageURI(uri);
        viewport.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        final float[] dragLast = new float[2];
        preview.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dragLast[0] = event.getRawX(); dragLast[1] = event.getRawY(); return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                movePreview(preview, viewport, event.getRawX() - dragLast[0], event.getRawY() - dragLast[1]);
                dragLast[0] = event.getRawX(); dragLast[1] = event.getRawY(); return true;
            }
            return event.getAction() == MotionEvent.ACTION_UP;
        });

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        Button left = pill("◀", false);
        Button up = pill("▲", false);
        Button down = pill("▼", false);
        Button right = pill("▶", false);
        Button center = pill("Center", false);
        Button[] controlButtons = {left, up, down, right, center};
        for (Button b : controlButtons) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(42), 1);
            p.setMargins(dp(3), 0, dp(3), 0);
            controls.addView(b, p);
        }
        layout.addView(controls, new LinearLayout.LayoutParams(-1, dp(42)));
        LinearLayout buttons = new LinearLayout(this);
        Button cancel = pill("Cancel", false);
        Button apply = pill("Apply wallpaper", true);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        cancelParams.rightMargin = dp(6);
        buttons.addView(cancel, cancelParams);
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(0, dp(46), 1);
        applyParams.leftMargin = dp(6);
        buttons.addView(apply, applyParams);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, dp(46));
        buttonsParams.topMargin = dp(8);
        layout.addView(buttons, buttonsParams);
        dialog.setContentView(layout);

        int move = dp(28);
        left.setOnClickListener(v -> movePreview(preview, viewport, -move, 0));
        right.setOnClickListener(v -> movePreview(preview, viewport, move, 0));
        up.setOnClickListener(v -> movePreview(preview, viewport, 0, -move));
        down.setOnClickListener(v -> movePreview(preview, viewport, 0, move));
        center.setOnClickListener(v -> { preview.setTranslationX(0); preview.setTranslationY(0); });
        cancel.setOnClickListener(v -> dialog.dismiss());
        apply.setOnClickListener(v -> {
            float width = Math.max(1, viewport.getWidth());
            float height = Math.max(1, viewport.getHeight());
            prefs().edit().putString(PREF_WALLPAPER, uri.toString())
                    .putFloat(PREF_WALLPAPER_X, preview.getTranslationX() / width)
                    .putFloat(PREF_WALLPAPER_Y, preview.getTranslationY() / height).apply();
            dialog.dismiss();
            drawerOpen = false;
            buildScreen();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            int width = Math.min(dp(850), getResources().getDisplayMetrics().widthPixels - dp(48));
            int height = Math.min(dp(620), getResources().getDisplayMetrics().heightPixels - dp(48));
            dialog.getWindow().setLayout(Math.max(dp(420), width), Math.max(dp(380), height));
        }
    }

    private void movePreview(ImageView preview, View viewport, float dx, float dy) {
        float maxX = viewport.getWidth() * .22f;
        float maxY = viewport.getHeight() * .22f;
        preview.setTranslationX(Math.max(-maxX, Math.min(maxX, preview.getTranslationX() + dx)));
        preview.setTranslationY(Math.max(-maxY, Math.min(maxY, preview.getTranslationY() + dy)));
    }

    private void closeDrawer() {
        if (!drawerOpen) return;
        drawerOpen = false;
        settingsDrawer.animate().translationX(dp(430)).setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> drawerScrim.setVisibility(View.GONE)).start();
    }

    private void launchApp(ResolveInfo app) {
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setComponent(new ComponentName(app.activityInfo.packageName, app.activityInfo.name));
        try {
            startActivity(launch);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show();
        }
    }

    private Button pill(String label, boolean selected) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(selected ? Color.rgb(16, 22, 33) : Color.WHITE);
        b.setBackground(roundRect(selected ? accent() : surfaceColor(), dp(22)));
        b.setFocusable(true);
        b.setOnFocusChangeListener((v, focused) -> {
            if (focused) {
                v.animate().scaleX(1.035f).scaleY(1.035f).setDuration(120).start();
                v.setElevation(dp(8));
                if (!selected) v.setBackground(roundRect(focusSurfaceColor(), dp(22)));
            } else {
                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                v.setElevation(dp(1));
                if (!selected) v.setBackground(roundRect(surfaceColor(), dp(22)));
            }
        });
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), Color.rgb(55, 64, 81));
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
