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
    private static final String PREFS = "launcher_preferences";
    private static final String PREF_ACCENT = "accent_color";
    private static final String PREF_ICON_SHAPE = "icon_shape";
    private static final String PREF_BADGE = "icon_badge";

    private FrameLayout root;
    private LinearLayout appGrid;
    private View drawerScrim;
    private LinearLayout settingsDrawer;
    private boolean drawerOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appGrid != null) populateApps();
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) closeQuickSettings();
        else super.onBackPressed();
    }

    private SharedPreferences preferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private int accentColor() {
        return preferences().getInt(PREF_ACCENT, Color.rgb(151, 185, 255));
    }

    private void buildScreen() {
        root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(56), dp(28), dp(56), dp(28));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand = text("NOVA", 16, accentColor(), true);
        brand.setLetterSpacing(0.16f);
        topBar.addView(brand);

        Button appsTab = pill("Apps", true);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-2, dp(48));
        tabParams.leftMargin = dp(38);
        topBar.addView(appsTab, tabParams);
        appsTab.setFocusable(false);

        View spacer = new View(this);
        topBar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        TextView time = text(new SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(new Date()),
                16, Color.rgb(220, 225, 235), false);
        topBar.addView(time);
        Button customize = pill("✦  Customize", false);
        LinearLayout.LayoutParams customizeParams = new LinearLayout.LayoutParams(-2, dp(48));
        customizeParams.leftMargin = dp(14);
        topBar.addView(customize, customizeParams);
        customize.setOnClickListener(v -> openDrawer(true));

        Button quickSettings = pill("⚙  Quick settings", false);
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(-2, dp(48));
        settingsParams.leftMargin = dp(10);
        topBar.addView(quickSettings, settingsParams);
        quickSettings.setOnClickListener(v -> openDrawer(false));
        page.addView(topBar);

        TextView heading = text("Your apps", 34, Color.WHITE, true);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.topMargin = dp(42);
        page.addView(heading, headingParams);
        TextView subtitle = text("Everything you want to watch, all in one place.", 16,
                Color.rgb(164, 173, 190), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(8);
        subtitleParams.bottomMargin = dp(24);
        page.addView(subtitle, subtitleParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        appGrid = new LinearLayout(this);
        appGrid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appGrid);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        drawerScrim = new View(this);
        drawerScrim.setBackgroundColor(0x99000000);
        drawerScrim.setVisibility(View.GONE);
        drawerScrim.setOnClickListener(v -> closeQuickSettings());
        root.addView(drawerScrim, new FrameLayout.LayoutParams(-1, -1));

        settingsDrawer = new LinearLayout(this);
        settingsDrawer.setOrientation(LinearLayout.VERTICAL);
        settingsDrawer.setBackground(roundRect(Color.rgb(25, 30, 40), dp(28)));
        FrameLayout.LayoutParams drawerParams = new FrameLayout.LayoutParams(dp(410), -1, Gravity.END);
        settingsDrawer.setTranslationX(dp(430));
        root.addView(settingsDrawer, drawerParams);

        setContentView(root);
        populateApps();
    }

    private void openDrawer(boolean customize) {
        if (drawerOpen) return;
        drawerOpen = true;
        drawerScrim.setVisibility(View.VISIBLE);
        settingsDrawer.removeAllViews();
        if (customize) buildCustomizationPanel(settingsDrawer);
        else buildQuickSettingsPanel(settingsDrawer);
        settingsDrawer.animate().translationX(0).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();
        settingsDrawer.requestFocus();
    }

    private void buildQuickSettingsPanel(LinearLayout panel) {
        panel.setPadding(dp(28), dp(34), dp(28), dp(28));
        panel.addView(text("Quick settings", 26, Color.WHITE, true));
        TextView note = text("Shortcuts to your device settings", 14,
                Color.rgb(166, 175, 191), false);
        addTopSpace(panel, note, 8, 24);
        addSettingShortcut(panel, "◉   Network & Wi-Fi", Settings.ACTION_WIFI_SETTINGS);
        addSettingShortcut(panel, "ᛒ   Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        addSettingShortcut(panel, "▣   Display", Settings.ACTION_DISPLAY_SETTINGS);
        addSettingShortcut(panel, "♫   Sound", Settings.ACTION_SOUND_SETTINGS);
        addSettingShortcut(panel, "⚙   All device settings", Settings.ACTION_SETTINGS);
        addDrawerClose(panel);
    }

    private void buildCustomizationPanel(LinearLayout panel) {
        panel.setPadding(dp(26), dp(28), dp(26), dp(24));
        panel.addView(text("Personalize", 26, Color.WHITE, true));
        TextView note = text("Choose your launcher colors and icon style", 14,
                Color.rgb(166, 175, 191), false);
        addTopSpace(panel, note, 6, 14);

        panel.addView(text("ACCENT COLOR", 12, accentColor(), true));
        addChoice(panel, "Royal Blue", Color.rgb(83, 139, 255), PREF_ACCENT);
        addChoice(panel, "Magenta", Color.rgb(232, 72, 174), PREF_ACCENT);
        addChoice(panel, "Purple", Color.rgb(165, 105, 255), PREF_ACCENT);
        addChoice(panel, "Emerald", Color.rgb(46, 190, 132), PREF_ACCENT);
        addChoice(panel, "Sunset Orange", Color.rgb(255, 145, 75), PREF_ACCENT);

        TextView shapeTitle = text("APP ICON SHAPE", 12, Color.rgb(180, 190, 208), true);
        LinearLayout.LayoutParams shapeTitleParams = new LinearLayout.LayoutParams(-1, -2);
        shapeTitleParams.topMargin = dp(14);
        shapeTitleParams.bottomMargin = dp(6);
        panel.addView(shapeTitle, shapeTitleParams);
        addTextChoice(panel, "Original", "original", PREF_ICON_SHAPE);
        addTextChoice(panel, "Circle", "circle", PREF_ICON_SHAPE);
        addTextChoice(panel, "Rounded square", "rounded", PREF_ICON_SHAPE);

        TextView badgeTitle = text("ICON EMOJI BADGE", 12, Color.rgb(180, 190, 208), true);
        LinearLayout.LayoutParams badgeTitleParams = new LinearLayout.LayoutParams(-1, -2);
        badgeTitleParams.topMargin = dp(14);
        badgeTitleParams.bottomMargin = dp(6);
        panel.addView(badgeTitle, badgeTitleParams);
        addTextChoice(panel, "None", "", PREF_BADGE);
        addTextChoice(panel, "Purple heart  💜", "💜", PREF_BADGE);
        addTextChoice(panel, "Blue heart  💙", "💙", PREF_BADGE);
        addTextChoice(panel, "Star  ⭐", "⭐", PREF_BADGE);

        addDrawerClose(panel);
    }

    private void addChoice(LinearLayout panel, String label, int color, String key) {
        Button choice = pill("●   " + label, false);
        choice.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        choice.setTextColor(color);
        addPanelButton(panel, choice);
        choice.setOnClickListener(v -> {
            preferences().edit().putInt(key, color).apply();
            refreshLauncher();
        });
    }

    private void addTextChoice(LinearLayout panel, String label, String value, String key) {
        Button choice = pill(label, false);
        choice.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        addPanelButton(panel, choice);
        choice.setOnClickListener(v -> {
            preferences().edit().putString(key, value).apply();
            refreshLauncher();
        });
    }

    private void addPanelButton(LinearLayout panel, Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(42));
        params.bottomMargin = dp(5);
        panel.addView(button, params);
    }

    private void addSettingShortcut(LinearLayout panel, String label, String action) {
        Button item = pill(label, false);
        item.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        item.setPadding(dp(18), 0, dp(18), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58));
        params.bottomMargin = dp(10);
        panel.addView(item, params);
        item.setOnClickListener(v -> launchSettings(action));
    }

    private void addTopSpace(LinearLayout panel, View view, int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(top);
        params.bottomMargin = dp(bottom);
        panel.addView(view, params);
    }

    private void addDrawerClose(LinearLayout panel) {
        View spacer = new View(this);
        panel.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1));
        Button close = pill("Close", false);
        panel.addView(close, new LinearLayout.LayoutParams(-1, dp(48)));
        close.setOnClickListener(v -> closeQuickSettings());
    }

    private void refreshLauncher() {
        drawerOpen = false;
        buildScreen();
    }

    private void closeQuickSettings() {
        if (!drawerOpen) return;
        drawerOpen = false;
        settingsDrawer.animate().translationX(dp(430)).setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> drawerScrim.setVisibility(View.GONE)).start();
    }

    private void launchSettings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, "This settings page is not available on this device", Toast.LENGTH_LONG).show();
        }
    }

    private void populateApps() {
        appGrid.removeAllViews();
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> unique = new LinkedHashMap<>();
        addAppsForCategory(pm, Intent.CATEGORY_LEANBACK_LAUNCHER, unique);
        addAppsForCategory(pm, Intent.CATEGORY_LAUNCHER, unique);
        List<ResolveInfo> apps = new ArrayList<>(unique.values());
        Collections.sort(apps, Comparator.comparing(info -> info.loadLabel(pm).toString().toLowerCase()));

        if (apps.isEmpty()) {
            appGrid.addView(text("No launchable apps found.", 18, Color.WHITE, false));
            return;
        }

        final int columns = 5;
        for (int start = 0; start < apps.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(16);
            appGrid.addView(row, rowParams);
            for (int i = start; i < Math.min(start + columns, apps.size()); i++) {
                ResolveInfo info = apps.get(i);
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(12), dp(12), dp(12), dp(12));
                tile.setBackground(roundRect(SURFACE, dp(22)));
                tile.setFocusable(true);
                tile.setClickable(true);
                tile.setOnFocusChangeListener((v, focused) ->
                        v.setBackground(roundRect(focused ? SURFACE_HIGHLIGHT : SURFACE, dp(22))));

                FrameLayout iconFrame = new FrameLayout(this);
                int iconSize = dp(58);
                ImageView icon = new ImageView(this);
                icon.setImageDrawable(info.loadIcon(pm));
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                String shape = preferences().getString(PREF_ICON_SHAPE, "original");
                if (!"original".equals(shape)) {
                    GradientDrawable iconBackground = new GradientDrawable();
                    iconBackground.setColor(Color.WHITE);
                    iconBackground.setCornerRadius("circle".equals(shape) ? iconSize / 2f : dp(14));
                    icon.setBackground(iconBackground);
                    icon.setClipToOutline(true);
                    icon.setPadding(dp(4), dp(4), dp(4), dp(4));
                }
                iconFrame.addView(icon, new FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER));
                String badge = preferences().getString(PREF_BADGE, "");
                if (!badge.isEmpty()) {
                    TextView badgeView = text(badge, 15, Color.WHITE, false);
                    badgeView.setGravity(Gravity.CENTER);
                    GradientDrawable badgeBg = new GradientDrawable();
                    badgeBg.setColor(Color.rgb(35, 40, 52));
                    badgeBg.setShape(GradientDrawable.OVAL);
                    badgeView.setBackground(badgeBg);
                    FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(25), dp(25), Gravity.END | Gravity.BOTTOM);
                    iconFrame.addView(badgeView, badgeParams);
                }
                tile.addView(iconFrame, new LinearLayout.LayoutParams(iconSize, iconSize));

                TextView label = text(info.loadLabel(pm).toString(), 14, Color.WHITE, true);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
                labelParams.topMargin = dp(10);
                tile.addView(label, labelParams);
                tile.setOnClickListener(v -> launchApp(info));
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(142), 1);
                tileParams.setMargins(dp(7), dp(4), dp(7), dp(4));
                row.addView(tile, tileParams);
            }
        }
        if (appGrid.getChildCount() > 0) {
            LinearLayout firstRow = (LinearLayout) appGrid.getChildAt(0);
            if (firstRow.getChildCount() > 0) firstRow.getChildAt(0).requestFocus();
        }
    }

    private void addAppsForCategory(PackageManager pm, String category, Map<String, ResolveInfo> destination) {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(category);
        for (ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            String key = info.activityInfo.packageName + "/" + info.activityInfo.name;
            destination.put(key, info);
        }
    }

    private void launchApp(ResolveInfo info) {
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
        try {
            startActivity(launch);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAssistant() {
        try {
            startActivity(new Intent(Intent.ACTION_ASSIST));
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, "Assistant isn't available on this device", Toast.LENGTH_LONG).show();
        }
    }

    private Button pill(String label, boolean selected) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(selected ? Color.rgb(16, 22, 33) : Color.WHITE);
        button.setBackground(roundRect(selected ? accentColor() : SURFACE, dp(24)));
        button.setFocusable(true);
        button.setOnFocusChangeListener((v, focused) -> {
            if (!selected) v.setBackground(roundRect(focused ? SURFACE_HIGHLIGHT : SURFACE, dp(24)));
        });
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.rgb(55, 64, 81));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
