package com.immersivecomic.translator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.immersivecomic.translator.model.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class TranslationHistoryRepository {
    private static final String PREF = "translation_history";
    private static final String KEY_ITEMS = "items";

    private final SharedPreferences preferences;

    public TranslationHistoryRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void savePage(Models.TranslatedPage page, Models.AppSettings settings) {
        if (!settings.privacySettings.saveTranslationHistory || page == null || page.blocks.isEmpty()) {
            return;
        }
        try {
            JSONArray items = loadArray();
            JSONObject item = new JSONObject();
            item.put("time", System.currentTimeMillis());
            item.put("source", page.detectedSourceLanguage);
            item.put("target", page.targetLanguage);
            item.put("count", page.blocks.size());
            JSONArray lines = new JSONArray();
            for (Models.TranslationBlock block : page.blocks) {
                lines.put(block.translation);
            }
            item.put("lines", lines);
            JSONArray next = new JSONArray();
            next.put(item);
            int limit = Math.max(0, settings.privacySettings.historyLimit);
            for (int i = 0; i < items.length() && next.length() < limit; i++) {
                next.put(items.getJSONObject(i));
            }
            preferences.edit().putString(KEY_ITEMS, next.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    public List<String> loadSummaries() {
        List<String> summaries = new ArrayList<>();
        JSONArray items = loadArray();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            if (item == null) continue;
            StringBuilder builder = new StringBuilder();
            builder.append(format.format(new Date(item.optLong("time"))))
                    .append("  ")
                    .append(item.optString("source", "unknown"))
                    .append(" → ")
                    .append(item.optString("target", ""))
                    .append("  ")
                    .append(item.optInt("count"))
                    .append(" 段");
            JSONArray lines = item.optJSONArray("lines");
            if (lines != null && lines.length() > 0) {
                builder.append("\n").append(lines.optString(0));
            }
            summaries.add(builder.toString());
        }
        return summaries;
    }

    public void clear() {
        preferences.edit().remove(KEY_ITEMS).apply();
    }

    private JSONArray loadArray() {
        try {
            return new JSONArray(preferences.getString(KEY_ITEMS, "[]"));
        } catch (JSONException exception) {
            return new JSONArray();
        }
    }
}
