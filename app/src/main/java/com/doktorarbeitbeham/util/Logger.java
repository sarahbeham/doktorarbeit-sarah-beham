package com.doktorarbeitbeham.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    private static final DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss:");

    public static void i(Context context, String msg) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        String existing = prefs.getString("logs", "");
        if (existing.length() - existing.replaceAll("\n", "").length() > 18) {
            int index = existing.indexOf("\n");
            existing = existing.substring(index + 1);
        }
        editor.putString("logs", existing + "\ni" + sdf.format(new Date()) + msg);
        editor.apply();
    }
}
