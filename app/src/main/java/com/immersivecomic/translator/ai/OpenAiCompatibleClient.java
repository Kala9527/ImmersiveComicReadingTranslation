package com.immersivecomic.translator.ai;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Base64;

import com.immersivecomic.translator.model.Models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class OpenAiCompatibleClient {
    public Models.TranslatedPage translateScreenshot(
            byte[] jpegBytes,
            Models.AppSettings settings,
            String ocrKey,
            String translationKey
    ) throws Models.TranslateFailure {
        return translateScreenshot(jpegBytes, settings, ocrKey, translationKey, null);
    }

    public Models.TranslatedPage translateScreenshot(
            byte[] jpegBytes,
            Models.AppSettings settings,
            String ocrKey,
            String translationKey,
            Models.TranslatedPage previousPage
    ) throws Models.TranslateFailure {
        Models.OcrPage ocrPage = recognize(jpegBytes, settings, ocrKey);
        if (ocrPage.blocks.isEmpty()) {
            throw new Models.TranslateFailure(
                    Models.FailureStage.OCR_PARSING,
                    "OCR 已返回结果，但没有识别到漫画文字块。",
                    true
            );
        }
        return translate(ocrPage, settings, translationKey, previousPage);
    }

    public Models.OcrPage recognize(byte[] jpegBytes, Models.AppSettings settings, String apiKey)
            throws Models.TranslateFailure {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new Models.TranslateFailure(
                    Models.FailureStage.IMAGE_PROCESSING,
                    "截图为空，无法发送给 OCR 模型。",
                    true
            );
        }
        try {
            JSONObject request = baseRequest(settings.ocrEndpoint);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", ocrPrompt(settings)));
            JSONArray content = new JSONArray();
            content.put(new JSONObject()
                    .put("type", "text")
                    .put("text", "请识别这张漫画截图中的所有漫画相关文字，只返回合法 JSON。"));
            content.put(new JSONObject()
                    .put("type", "image_url")
                    .put("image_url", new JSONObject()
                            .put("url", "data:image/jpeg;base64," + Base64.encodeToString(jpegBytes, Base64.NO_WRAP))
                            .put("detail", settings.ocrEndpoint.imageDetail.wireName)));
            messages.put(new JSONObject().put("role", "user").put("content", content));
            request.put("messages", messages);
            request.put("max_tokens", settings.ocrEndpoint.maxOutputTokens);
            maybeJsonMode(request, settings.ocrEndpoint);
            String contentText = postChat(settings.ocrEndpoint, apiKey, request);
            try {
                return Models.OcrPage.fromJson(contentText);
            } catch (Exception exception) {
                throw new Models.TranslateFailure(
                        Models.FailureStage.OCR_PARSING,
                        "无法解析 OCR JSON：" + exception.getMessage() + "；原始响应：" + excerpt(contentText),
                        true,
                        exception
                );
            }
        } catch (Models.TranslateFailure failure) {
            throw failure;
        } catch (Exception exception) {
            throw new Models.TranslateFailure(
                    Models.FailureStage.OCR_PARSING,
                    "无法解析 OCR JSON：" + exception.getMessage(),
                    true,
                    exception
            );
        }
    }

    public Models.TranslatedPage translate(
            Models.OcrPage ocrPage,
            Models.AppSettings settings,
            String apiKey
    ) throws Models.TranslateFailure {
        return translate(ocrPage, settings, apiKey, null);
    }

    public Models.TranslatedPage translate(
            Models.OcrPage ocrPage,
            Models.AppSettings settings,
            String apiKey,
            Models.TranslatedPage previousPage
    ) throws Models.TranslateFailure {
        try {
            JSONObject request = baseRequest(settings.translationEndpoint);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", translationPrompt(settings)));
            JSONObject payload = new JSONObject();
            payload.put("detected_source_language", ocrPage.pageLanguage);
            payload.put("target_language", settings.language.targetLanguage.code);
            payload.put("reading_direction", ocrPage.readingDirection);
            JSONArray blocks = new JSONArray();
            for (Models.OcrBlock block : ocrPage.blocks) {
                blocks.put(block.toJson());
            }
            payload.put("blocks", blocks);
            if (settings.translation.usePreviousPageContext && previousPage != null) {
                payload.put("previous_page_context", previousContext(previousPage, settings.translation.maxPreviousContextBlocks));
            }
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", payload.toString()));
            request.put("messages", messages);
            request.put("max_tokens", settings.translationEndpoint.maxOutputTokens);
            maybeJsonMode(request, settings.translationEndpoint);
            String contentText = postChat(settings.translationEndpoint, apiKey, request);
            return Models.TranslatedPage.fromJson(contentText, ocrPage, settings.language.targetLanguage);
        } catch (Models.TranslateFailure failure) {
            throw failure;
        } catch (Exception exception) {
            throw new Models.TranslateFailure(
                    Models.FailureStage.TRANSLATION_PARSING,
                    "无法解析翻译 JSON：" + exception.getMessage(),
                    true,
                    exception
            );
        }
    }

    public String testOcr(Models.AppSettings settings, String apiKey) throws Models.TranslateFailure {
        Bitmap bitmap = Bitmap.createBitmap(600, 360, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.rgb(250, 246, 236));
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(23, 33, 31));
        paint.setTextSize(42);
        canvas.drawText("もう帰るの？", 80, 150, paint);
        canvas.drawText("ドン！", 80, 230, paint);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
        Models.OcrPage page = recognize(out.toByteArray(), settings, apiKey);
        if (page.blocks.isEmpty()) {
            throw new Models.TranslateFailure(
                    Models.FailureStage.OCR_PARSING,
                    "测试 OCR 返回成功，但没有识别到任何文字块。请确认所选模型支持图片输入。",
                    false
            );
        }
        return String.format(Locale.US, "连接成功。识别到 %d 个文字块，语言：%s",
                page.blocks.size(), page.pageLanguage);
    }

    public String testTranslation(Models.AppSettings settings, String apiKey) throws Models.TranslateFailure {
        try {
            JSONArray blocks = new JSONArray();
            blocks.put(new JSONObject()
                    .put("id", "b1")
                    .put("order", 1)
                    .put("language", "ja")
                    .put("original_text", "もう帰るの？")
                    .put("type", "dialogue")
                    .put("confidence", 0.98));
            Models.OcrPage page = Models.OcrPage.fromJson(new JSONObject()
                    .put("page_language", "ja")
                    .put("language_confidence", 0.98)
                    .put("is_mixed_language", false)
                    .put("reading_direction", "rtl")
                    .put("blocks", blocks)
                    .toString());
            Models.TranslatedPage translatedPage = translate(page, settings, apiKey);
            if (!settings.language.targetLanguage.code.equals(translatedPage.targetLanguage)) {
                throw new Models.TranslateFailure(
                        Models.FailureStage.TRANSLATION_PARSING,
                        "翻译测试返回的目标语言不一致：" + translatedPage.targetLanguage,
                        false
                );
            }
            return "连接成功。示例译文：" +
                    (translatedPage.blocks.isEmpty() ? "未返回译文" : translatedPage.blocks.get(0).translation);
        } catch (JSONException exception) {
            throw new Models.TranslateFailure(
                    Models.FailureStage.TRANSLATION_PARSING,
                    exception.getMessage(),
                    true,
                    exception
            );
        }
    }

    private JSONObject baseRequest(Models.AiEndpointConfig endpoint) throws JSONException {
        JSONObject request = new JSONObject();
        request.put("model", endpoint.modelId);
        if (endpoint.enableThinking) {
            request.put("enable_thinking", true);
        }
        if (endpoint.extraBodyJson != null && !endpoint.extraBodyJson.trim().isEmpty()) {
            JSONObject extras = new JSONObject(Models.cleanJsonContent(endpoint.extraBodyJson));
            JSONArray names = extras.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String name = names.getString(i);
                    request.put(name, extras.get(name));
                }
            }
        }
        return request;
    }

    private JSONArray previousContext(Models.TranslatedPage page, int maxBlocks) throws JSONException {
        JSONArray context = new JSONArray();
        int count = 0;
        for (Models.TranslationBlock block : page.blocks) {
            if (count >= maxBlocks) break;
            JSONObject item = new JSONObject();
            item.put("source", block.source == null ? block.correctedText : block.source.originalText);
            item.put("translation", block.translation);
            context.put(item);
            count++;
        }
        return context;
    }

    private void maybeJsonMode(JSONObject request, Models.AiEndpointConfig endpoint) throws JSONException {
        if (endpoint.enableJsonMode) {
            request.put("response_format", new JSONObject().put("type", "json_object"));
        }
    }

    private String postChat(Models.AiEndpointConfig endpoint, String apiKey, JSONObject request)
            throws Models.TranslateFailure {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(joinUrl(endpoint.baseUrl, endpoint.endpointPath));
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout((int) endpoint.requestTimeoutSeconds * 1000);
            connection.setReadTimeout((int) endpoint.requestTimeoutSeconds * 1000);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            if (endpoint.authentication == Models.AuthenticationType.API_KEY_HEADER) {
                connection.setRequestProperty("X-API-Key", apiKey);
            } else if (endpoint.authentication == Models.AuthenticationType.CUSTOM_HEADER) {
                if (endpoint.customAuthHeader == null || endpoint.customAuthHeader.trim().isEmpty()) {
                    Models.FailureStage stage = endpoint.capability == Models.ModelCapability.TEXT
                            ? Models.FailureStage.TRANSLATION_NETWORK
                            : Models.FailureStage.OCR_NETWORK;
                    throw new Models.TranslateFailure(stage, "自定义认证 Header 为空，请检查模型高级配置。", false);
                }
                connection.setRequestProperty(endpoint.customAuthHeader, apiKey);
            } else {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(request.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = connection.getResponseCode();
            String body = readAll(code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream());
            if (code < 200 || code >= 300) {
                Models.FailureStage stage = endpoint.capability == Models.ModelCapability.TEXT
                        ? Models.FailureStage.TRANSLATION_NETWORK
                        : Models.FailureStage.OCR_NETWORK;
                throw new Models.TranslateFailure(stage, friendlyHttpError(code, body), canRetryHttp(code));
            }
            return extractMessageContent(body, endpoint);
        } catch (Models.TranslateFailure failure) {
            throw failure;
        } catch (Exception exception) {
            Models.FailureStage stage = endpoint.capability == Models.ModelCapability.TEXT
                    ? Models.FailureStage.TRANSLATION_NETWORK
                    : Models.FailureStage.OCR_NETWORK;
            throw new Models.TranslateFailure(stage, "模型服务连接失败：" + exception.getMessage(), true, exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readAll(InputStream inputStream) throws Exception {
        if (inputStream == null) return "";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }

    private String joinUrl(String baseUrl, String path) {
        String safeBase = baseUrl == null ? "" : baseUrl.trim();
        String safePath = path == null || path.trim().isEmpty() ? "/chat/completions" : path.trim();
        if (safeBase.endsWith(safePath) || safeBase.endsWith(safePath.substring(1))) {
            return safeBase;
        }
        safeBase = safeBase.endsWith("/") ? safeBase.substring(0, safeBase.length() - 1) : safeBase;
        safePath = safePath.startsWith("/") ? safePath : "/" + safePath;
        return safeBase + safePath;
    }

    private String extractMessageContent(String body, Models.AiEndpointConfig endpoint) throws Models.TranslateFailure {
        try {
            JSONObject response = new JSONObject(body);
            JSONObject message = response
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message");
            Object content = message.opt("content");
            if (content instanceof String && !((String) content).trim().isEmpty()) {
                return (String) content;
            }
            if (content instanceof JSONArray) {
                StringBuilder builder = new StringBuilder();
                JSONArray parts = (JSONArray) content;
                for (int i = 0; i < parts.length(); i++) {
                    JSONObject part = parts.optJSONObject(i);
                    if (part != null) {
                        builder.append(part.optString("text"));
                    }
                }
                if (builder.length() > 0) {
                    return builder.toString();
                }
            }
            String refusal = message.optString("refusal", "");
            if (!refusal.isEmpty()) {
                throw new JSONException("模型拒绝响应：" + refusal);
            }
            throw new JSONException("choices[0].message.content 为空");
        } catch (JSONException exception) {
            Models.FailureStage stage = endpoint.capability == Models.ModelCapability.TEXT
                    ? Models.FailureStage.TRANSLATION_PARSING
                    : Models.FailureStage.OCR_PARSING;
            throw new Models.TranslateFailure(
                    stage,
                    "模型响应格式不兼容：" + exception.getMessage(),
                    false,
                    exception
            );
        }
    }

    private boolean canRetryHttp(int code) {
        return code == 408 || code == 409 || code == 425 || code == 429 || code >= 500;
    }

    private String excerpt(String text) {
        if (text == null) return "";
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() <= 180 ? compact : compact.substring(0, 180) + "...";
    }

    private String friendlyHttpError(int code, String body) {
        if (code == 401 || code == 403) {
            return "HTTP " + code + "：鉴权失败，请检查 API Key 或认证方式。";
        }
        if (code == 400 || code == 404) {
            return "HTTP " + code + "：请求配置可能不兼容，请检查 Base URL、接口路径和模型 ID。"
                    + (body == null || body.isEmpty() ? "" : "\n" + body);
        }
        if (code == 429) {
            return "HTTP 429：模型服务限流，请稍后重试。";
        }
        return "HTTP " + code + "：" + body;
    }

    private String ocrPrompt(Models.AppSettings settings) {
        if (settings.ocrEndpoint.modelId != null
                && settings.ocrEndpoint.modelId.toLowerCase(Locale.US).contains("deepseek-ocr")) {
            return "Free OCR. Output only the visible manga text, one item per line. Include speech bubbles, narration, notes, and sound effects.";
        }
        return "你是漫画图片 OCR 引擎。\n"
                + "用户设置的预计原文语言：" + settings.language.sourceLanguage.code + "\n"
                + "需要支持中文、英文、日文、韩文。识别所有漫画相关文字，自动判断页面主要语言。"
                + "如果页面包含多种语言，为每个文字块单独标记 language。"
                + "排除状态栏、导航栏、按钮、页码和阅读器界面。"
                + "识别对白、旁白、注释和拟声词。日文漫画默认考虑从右向左阅读。"
                + "无法确定的字符使用 [?]，不得虚构文字，不执行翻译。"
                + "只返回合法 JSON，坐标范围统一为 0 到 1000。"
                + "page_language 只能是 zh、en、ja、ko、mixed、unknown 之一；reading_direction 只能是 rtl、ltr、vertical 之一。"
                + "格式示例：{\"page_language\":\"ja\","
                + "\"language_confidence\":0.0,\"is_mixed_language\":false,"
                + "\"reading_direction\":\"rtl\",\"blocks\":[{\"id\":\"b1\","
                + "\"order\":1,\"language\":\"ja\",\"original_text\":\"原文\","
                + "\"type\":\"dialogue|narration|sound_effect|note\","
                + "\"bbox\":{\"left\":0,\"top\":0,\"right\":0,\"bottom\":0},"
                + "\"confidence\":0.0}]}";
    }

    private String translationPrompt(Models.AppSettings settings) {
        if (settings.translationEndpoint.modelId != null
                && settings.translationEndpoint.modelId.toLowerCase(Locale.US).contains("qwen/qwen2.5-7b")) {
            return "You are a manga translation engine. Translate each input block into Simplified Chinese. "
                    + "Correct obvious OCR errors, preserve tone and manga sound effects. "
                    + "Return JSON only with this exact shape: "
                    + "{\"detected_source_language\":\"ja\",\"target_language\":\"zh-CN\",\"page_summary\":\"short\","
                    + "\"blocks\":[{\"id\":\"b1\",\"order\":1,\"corrected_text\":\"source\",\"translation\":\"中文译文\",\"translation_confidence\":0.9}]}. "
                    + "Keep every input id and order unchanged. Do not explain.";
        }
        return "你是专业漫画 OCR 纠错和翻译引擎。\n"
                + "目标语言：" + settings.language.targetLanguage.code + "\n"
                + "翻译风格：" + settings.translation.style.name() + "\n"
                + "用户偏好：纠正 OCR：" + settings.translation.correctOcrErrors
                + "，翻译拟声词：" + settings.translation.translateSoundEffects
                + "，保留敬称：" + settings.translation.preserveHonorifics
                + "，保留角色名：" + settings.translation.preserveNames + "。\n"
                + "根据整页上下文修正明显 OCR 错误。不确定时保留原文，不得虚构。"
                + "保留角色语气、停顿、情绪和称呼关系。翻译应自然。"
                + "每个输入 block 必须返回对应结果，不得改变 id 和 order。"
                + "不添加解释、注释或翻译说明，只输出合法 JSON。"
                + "格式：{\"detected_source_language\":\"ja\",\"target_language\":\"zh-CN\","
                + "\"page_summary\":\"供下一页使用的简短上下文\","
                + "\"blocks\":[{\"id\":\"b1\",\"order\":1,"
                + "\"corrected_text\":\"纠错后的原文\",\"translation\":\"译文\","
                + "\"translation_confidence\":0.0}]}";
    }
}
