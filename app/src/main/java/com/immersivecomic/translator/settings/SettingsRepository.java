package com.immersivecomic.translator.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.immersivecomic.translator.model.Models;

public final class SettingsRepository {
    private static final String PREF = "app_settings";
    private final SharedPreferences preferences;

    public SettingsRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public Models.AppSettings load() {
        Models.AppSettings settings = new Models.AppSettings();
        settings.onboardingCompleted = preferences.getBoolean("onboarding_completed", false);
        settings.ocrTestPassed = preferences.getBoolean("ocr_test_passed", false);
        settings.translationTestPassed = preferences.getBoolean("translation_test_passed", false);
        settings.language.sourceLanguage = enumValue("source_language", Models.SourceLanguage.AUTO);
        settings.language.targetLanguage = enumValue("target_language", Models.TargetLanguage.SIMPLIFIED_CHINESE);
        settings.language.lockDetectedLanguage = preferences.getBoolean("lock_detected_language", true);
        settings.language.allowMixedLanguage = preferences.getBoolean("allow_mixed_language", true);
        settings.translation.style = enumValue("translation_style", Models.TranslationStyle.NATURAL);
        settings.translation.usePageContext = preferences.getBoolean("use_page_context", true);
        settings.translation.correctOcrErrors = preferences.getBoolean("correct_ocr_errors", true);
        settings.translation.translateSoundEffects = preferences.getBoolean("translate_sound_effects", true);
        settings.translation.preserveHonorifics = preferences.getBoolean("preserve_honorifics", false);
        settings.translation.preserveNames = preferences.getBoolean("preserve_names", true);
        settings.translation.showOriginalText = preferences.getBoolean("show_original_text", true);
        settings.translation.usePreviousPageContext = preferences.getBoolean("use_previous_page_context", true);
        settings.translation.maxPreviousContextBlocks = clamp(
                preferences.getInt("max_previous_context_blocks", settings.translation.maxPreviousContextBlocks),
                0,
                30
        );
        settings.overlay.panelSide = enumValue("panel_side", Models.ScreenSide.RIGHT);
        settings.overlay.bubbleSide = enumValue("bubble_side", Models.ScreenSide.RIGHT);
        settings.overlay.panelWidthPercent = clampFloat(preferences.getFloat("panel_width_percent", 0.72f), 0.55f, 0.86f);
        settings.overlay.panelMaxHeightPercent = clampFloat(preferences.getFloat("panel_max_height_percent", 0.82f), 0.45f, 0.92f);
        settings.overlay.fontScale = clampFloat(preferences.getFloat("font_scale", 1.0f), 0.85f, 1.3f);
        settings.overlay.panelOpacity = clampFloat(preferences.getFloat("panel_opacity", 0.94f), 0.78f, 1.0f);
        settings.overlay.showOriginalText = preferences.getBoolean("overlay_show_original_text", true);
        settings.overlay.autoExpandAfterTranslation = preferences.getBoolean("auto_expand_after_translation", true);
        settings.overlay.collapseBubbleDuringTranslation = preferences.getBoolean("collapse_bubble_during_translation", true);
        settings.overlay.rememberBubblePosition = preferences.getBoolean("remember_bubble_position", true);
        settings.overlay.rememberPanelState = preferences.getBoolean("remember_panel_state", true);
        settings.processingMode = enumValue("processing_mode", Models.ProcessingMode.TWO_STAGE);
        settings.imageSettings.maxLongSide = clamp(preferences.getInt("image_max_long_side", 1920), 720, 2560);
        settings.imageSettings.jpegQuality = clamp(preferences.getInt("image_jpeg_quality", 85), 60, 95);
        settings.imageSettings.removeSystemBars = preferences.getBoolean("image_remove_system_bars", true);
        settings.imageSettings.detectBlackScreen = preferences.getBoolean("image_detect_black_screen", true);
        settings.privacySettings.saveScreenshots = preferences.getBoolean("privacy_save_screenshots", false);
        settings.privacySettings.saveTranslationHistory = preferences.getBoolean("privacy_save_translation_history", true);
        settings.privacySettings.historyLimit = clamp(preferences.getInt("privacy_history_limit", 50), 0, 300);
        settings.privacySettings.clearImageAfterRequest = preferences.getBoolean("privacy_clear_image_after_request", true);
        readEndpoint(settings.ocrEndpoint, "ocr");
        readEndpoint(settings.translationEndpoint, "translation");
        return settings;
    }

    public void save(Models.AppSettings settings) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("onboarding_completed", settings.onboardingCompleted);
        editor.putBoolean("ocr_test_passed", settings.ocrTestPassed);
        editor.putBoolean("translation_test_passed", settings.translationTestPassed);
        editor.putString("source_language", settings.language.sourceLanguage.name());
        editor.putString("target_language", settings.language.targetLanguage.name());
        editor.putBoolean("lock_detected_language", settings.language.lockDetectedLanguage);
        editor.putBoolean("allow_mixed_language", settings.language.allowMixedLanguage);
        editor.putString("translation_style", settings.translation.style.name());
        editor.putBoolean("use_page_context", settings.translation.usePageContext);
        editor.putBoolean("correct_ocr_errors", settings.translation.correctOcrErrors);
        editor.putBoolean("translate_sound_effects", settings.translation.translateSoundEffects);
        editor.putBoolean("preserve_honorifics", settings.translation.preserveHonorifics);
        editor.putBoolean("preserve_names", settings.translation.preserveNames);
        editor.putBoolean("show_original_text", settings.translation.showOriginalText);
        editor.putBoolean("use_previous_page_context", settings.translation.usePreviousPageContext);
        editor.putInt("max_previous_context_blocks", settings.translation.maxPreviousContextBlocks);
        editor.putString("panel_side", settings.overlay.panelSide.name());
        editor.putString("bubble_side", settings.overlay.bubbleSide.name());
        editor.putFloat("panel_width_percent", settings.overlay.panelWidthPercent);
        editor.putFloat("panel_max_height_percent", settings.overlay.panelMaxHeightPercent);
        editor.putFloat("font_scale", settings.overlay.fontScale);
        editor.putFloat("panel_opacity", settings.overlay.panelOpacity);
        editor.putBoolean("overlay_show_original_text", settings.overlay.showOriginalText);
        editor.putBoolean("auto_expand_after_translation", settings.overlay.autoExpandAfterTranslation);
        editor.putBoolean("collapse_bubble_during_translation", settings.overlay.collapseBubbleDuringTranslation);
        editor.putBoolean("remember_bubble_position", settings.overlay.rememberBubblePosition);
        editor.putBoolean("remember_panel_state", settings.overlay.rememberPanelState);
        editor.putString("processing_mode", settings.processingMode.name());
        editor.putInt("image_max_long_side", settings.imageSettings.maxLongSide);
        editor.putInt("image_jpeg_quality", settings.imageSettings.jpegQuality);
        editor.putBoolean("image_remove_system_bars", settings.imageSettings.removeSystemBars);
        editor.putBoolean("image_detect_black_screen", settings.imageSettings.detectBlackScreen);
        editor.putBoolean("privacy_save_screenshots", settings.privacySettings.saveScreenshots);
        editor.putBoolean("privacy_save_translation_history", settings.privacySettings.saveTranslationHistory);
        editor.putInt("privacy_history_limit", settings.privacySettings.historyLimit);
        editor.putBoolean("privacy_clear_image_after_request", settings.privacySettings.clearImageAfterRequest);
        writeEndpoint(editor, settings.ocrEndpoint, "ocr");
        writeEndpoint(editor, settings.translationEndpoint, "translation");
        editor.apply();
    }

    public void markTestResult(boolean ocr, boolean passed) {
        preferences.edit().putBoolean(ocr ? "ocr_test_passed" : "translation_test_passed", passed).apply();
    }

    private void readEndpoint(Models.AiEndpointConfig endpoint, String prefix) {
        endpoint.provider = enumValue(prefix + "_provider", endpoint.provider);
        endpoint.name = clean(preferences.getString(prefix + "_name", endpoint.name), endpoint.name);
        endpoint.baseUrl = clean(preferences.getString(prefix + "_base_url", endpoint.baseUrl), endpoint.baseUrl);
        endpoint.endpointPath = clean(preferences.getString(prefix + "_endpoint_path", endpoint.endpointPath), endpoint.endpointPath);
        endpoint.modelId = clean(preferences.getString(prefix + "_model_id", endpoint.modelId), "");
        endpoint.capability = enumValue(prefix + "_capability", endpoint.capability);
        endpoint.authentication = enumValue(prefix + "_authentication", endpoint.authentication);
        endpoint.customAuthHeader = clean(preferences.getString(prefix + "_custom_auth_header", endpoint.customAuthHeader), "");
        endpoint.requestTimeoutSeconds = clampLong(preferences.getLong(prefix + "_timeout", endpoint.requestTimeoutSeconds), 10, 240);
        endpoint.maxOutputTokens = clamp(preferences.getInt(prefix + "_max_output_tokens", endpoint.maxOutputTokens), 512, 16384);
        endpoint.imageDetail = enumValue(prefix + "_image_detail", endpoint.imageDetail);
        endpoint.enableJsonMode = preferences.getBoolean(prefix + "_json_mode", endpoint.enableJsonMode);
        endpoint.enableThinking = preferences.getBoolean(prefix + "_thinking", endpoint.enableThinking);
        endpoint.extraBodyJson = clean(preferences.getString(prefix + "_extra_body_json", endpoint.extraBodyJson), "");
        if (endpoint.endpointPath.isEmpty()) {
            endpoint.endpointPath = "/chat/completions";
        }
        if (!endpoint.endpointPath.startsWith("/")) {
            endpoint.endpointPath = "/" + endpoint.endpointPath;
        }
    }

    private void writeEndpoint(SharedPreferences.Editor editor, Models.AiEndpointConfig endpoint, String prefix) {
        editor.putString(prefix + "_provider", endpoint.provider.name());
        editor.putString(prefix + "_name", endpoint.name);
        editor.putString(prefix + "_base_url", endpoint.baseUrl);
        editor.putString(prefix + "_endpoint_path", endpoint.endpointPath);
        editor.putString(prefix + "_model_id", endpoint.modelId);
        editor.putString(prefix + "_capability", endpoint.capability.name());
        editor.putString(prefix + "_authentication", endpoint.authentication.name());
        editor.putString(prefix + "_custom_auth_header", endpoint.customAuthHeader);
        editor.putLong(prefix + "_timeout", endpoint.requestTimeoutSeconds);
        editor.putInt(prefix + "_max_output_tokens", endpoint.maxOutputTokens);
        editor.putString(prefix + "_image_detail", endpoint.imageDetail.name());
        editor.putBoolean(prefix + "_json_mode", endpoint.enableJsonMode);
        editor.putBoolean(prefix + "_thinking", endpoint.enableThinking);
        editor.putString(prefix + "_extra_body_json", endpoint.extraBodyJson);
    }

    private <T extends Enum<T>> T enumValue(String key, T fallback) {
        String value = preferences.getString(key, fallback.name());
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String clean(String value, String fallback) {
        if (value == null) return fallback;
        return value.trim();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
