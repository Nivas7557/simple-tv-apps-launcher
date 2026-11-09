package com.example.simpletvlauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(16, 18, 24);
    private static final int SURFACE = Color.rgb(34, 38, 48);
    private static final int ACCENT = Color.rgb(131, 167, 255);
    private LinearLayout appGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appGrid != null) populateApps();
    }

    private void buildScreen() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(48), dp(28), dp(48), dp(24));
        page.setBackgroundColor(BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("Your apps");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button search = new Button(this);
        search.setText("Search / Assistant");
        search.setTextColor(Color.WHITE);
        search.setBackground(roundRect(SURFACE, dp(18)));
        search.setOnClickListener(v -> openAssistant());
        header.addView(search);
        page.addView(header);

        TextView subtitle = new TextView(this);
        subtitle.setText("Apps only • Use your remote to navigate");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(15);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(8);
        subtitleParams.bottomMargin = dp(22);
        page.addView(subtitle, subtitleParams);

        ScrollView scroll = new ScrollView(this);
        appGrid = new LinearLayout(this);
        appGrid.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(appGrid);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);
        populateApps();
    }

    private void populateApps() {
        appGrid.removeAllViews();
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        query.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(query, 0));
        Collections.sort(apps, Comparator.comparing(
                info -> info.loadLabel(pm).toString().toLowerCase()));

        if (apps.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No launchable apps found.");
            empty.setTextColor(Color.WHITE);
            empty.setTextSize(18);
            appGrid.addView(empty);
            return;
        }

        final int columns = 5;
        for (int start = 0; start < apps.size(); start += columns) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(14);
            appGrid.addView(row, rowParams);

            for (int i = start; i < Math.min(start + columns, apps.size()); i++) {
                ResolveInfo info = apps.get(i);
                Button tile = new Button(this);
                tile.setText(info.loadLabel(pm));
                tile.setAllCaps(false);
                tile.setTextColor(Color.WHITE);
                tile.setTextSize(15);
                tile.setGravity(Gravity.CENTER);
                tile.setCompoundDrawablesWithIntrinsicBounds(null,
                        info.loadIcon(pm), null, null);
                tile.setCompoundDrawablePadding(dp(10));
                tile.setBackground(roundRect(SURFACE, dp(20)));
                tile.setFocusable(true);
                tile.setOnClickListener(v -> {
                    Intent launch = new Intent(Intent.ACTION_MAIN);
                    launch.addCategory(Intent.CATEGORY_LAUNCHER);
                    launch.setComponent(new android.content.ComponentName(
                            info.activityInfo.packageName, info.activityInfo.name));
                    try {
                        startActivity(launch);
                    } catch (ActivityNotFoundException ex) {
                        Toast.makeText(this, "Unable to open app", Toast.LENGTH_SHORT).show();
                    }
                });
                LinearLayout.LayoutParams tileParams = new LinearLayout.LayoutParams(0, dp(132), 1);
                tileParams.setMargins(dp(6), dp(4), dp(6), dp(4));
                row.addView(tile, tileParams);
            }
        }
        if (appGrid.getChildCount() > 0) appGrid.getChildAt(0).requestFocus();
    }

    private void openAssistant() {
        try {
            startActivity(new Intent(Intent.ACTION_ASSIST));
        } catch (ActivityNotFoundException | SecurityException ex) {
            Toast.makeText(this, "Assistant isn't available on this device", Toast.LENGTH_LONG).show();
        }
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), Color.rgb(62, 69, 84));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
