package com.immersivecomic.translator.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Models {
    private Models() {
    }

    public enum SourceLanguage {
        AUTO("auto", "自动识别"),
        CHINESE("zh", "中文"),
        ENGLISH("en", "英文"),
        JAPANESE("ja", "日文"),
        KOREAN("ko", "韩文");

        public final String code;
        public final String label;

        SourceLanguage(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    public enum TargetLanguage {
        SIMPLIFIED_CHINESE("zh-CN", "简体中文"),
        TRADITIONAL_CHINESE("zh-TW", "繁体中文"),
        ENGLISH("en", "英文"),
        JAPANESE("ja", "日文"),
        KOREAN("ko", "韩文");

        public final String code;
        public final String label;

        TargetLanguage(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    public enum SameLanguageAction {
        SKIP, CORRECT_ONLY, SHOW_ORIGINAL
    }

    public enum TranslationStyle {
        LITERAL("直译"),
        NATURAL("自然"),
        CONCISE("简洁"),
        COMIC("漫画化");

        public final String label;

        TranslationStyle(String label) {
            this.label = label;
        }
    }

    public enum ProviderType {
        DEEPSEEK("DeepSeek"),
        SILICON_FLOW("硅基流动"),
        ALIBABA_QWEN("阿里云百炼"),
        OPENAI_COMPATIBLE("OpenAI 兼容");

        public final String label;

        ProviderType(String label) {
            this.label = label;
        }
    }

    public enum ModelCapability {
        TEXT, VISION, OCR, VISION_AND_TEXT
    }

    public enum AuthenticationType {
        BEARER, API_KEY_HEADER, CUSTOM_HEADER
    }

    public enum ImageDetail {
        LOW("low"), AUTO("auto"), HIGH("high");

        public final String wireName;

        ImageDetail(String wireName) {
            this.wireName = wireName;
        }
    }

    public enum ProcessingMode {
        TWO_STAGE, SINGLE_VISION_MODEL
    }

    public enum ScreenSide {
        LEFT, RIGHT
    }

    public enum BlockType {
        DIALOGUE, NARRATION, SOUND_EFFECT, NOTE, UNKNOWN
    }

    public enum FailureStage {
        PERMISSION,
        SCREEN_CAPTURE,
        IMAGE_PROCESSING,
        OCR_NETWORK,
        OCR_PARSING,
        TRANSLATION_NETWORK,
        TRANSLATION_PARSING,
        OVERLAY
    }

    public static final class LanguageSettings {
        public SourceLanguage sourceLanguage = SourceLanguage.AUTO;
        public TargetLanguage targetLanguage = TargetLanguage.SIMPLIFIED_CHINESE;
        public boolean lockDetectedLanguage = true;
        public int languageLockPageCount = 2;
        public boolean allowMixedLanguage = true;
        public SameLanguageAction sameLanguageAction = SameLanguageAction.CORRECT_ONLY;
    }

    public static final class TranslationSettings {
        public TranslationStyle style = TranslationStyle.NATURAL;
        public boolean usePageContext = true;
        public boolean correctOcrErrors = true;
        public boolean translateSoundEffects = true;
        public boolean preserveHonorifics = false;
        public boolean preserveNames = true;
        public boolean showOriginalText = true;
        public boolean usePreviousPageContext = true;
        public int maxPreviousContextBlocks = 8;
    }

    public static final class OverlaySettings {
        public ScreenSide bubbleSide = ScreenSide.RIGHT;
        public ScreenSide panelSide = ScreenSide.RIGHT;
        public float panelWidthPercent = 0.72f;
        public float panelMaxHeightPercent = 0.82f;
        public float fontScale = 1.0f;
        public float panelOpacity = 0.94f;
        public boolean showOriginalText = true;
        public boolean autoExpandAfterTranslation = true;
        public boolean collapseBubbleDuringTranslation = true;
        public boolean rememberBubblePosition = true;
        public boolean rememberPanelState = true;
    }

    public static final class ImageSettings {
        public int maxLongSide = 1920;
        public int jpegQuality = 85;
        public boolean removeSystemBars = true;
        public boolean detectBlackScreen = true;
    }

    public static final class PrivacySettings {
        public boolean saveScreenshots = false;
        public boolean saveTranslationHistory = true;
        public int historyLimit = 50;
        public boolean clearImageAfterRequest = true;
    }

    public static final class AiEndpointConfig {
        public String id;
        public String name;
        public ProviderType provider;
        public String baseUrl;
        public String endpointPath = "/chat/completions";
        public String modelId = "";
        public String secretReference;
        public ModelCapability capability;
        public AuthenticationType authentication = AuthenticationType.BEARER;
        public String customAuthHeader = "";
        public long requestTimeoutSeconds = 90;
        public int maxOutputTokens = 4096;
        public ImageDetail imageDetail = ImageDetail.HIGH;
        public boolean enableJsonMode = false;
        public boolean enableThinking = false;
        public String extraBodyJson = "";

        public AiEndpointConfig(String id, String name, ProviderType provider, String baseUrl,
                                ModelCapability capability, String secretReference) {
            this.id = id;
            this.name = name;
            this.provider = provider;
            this.baseUrl = baseUrl;
            this.capability = capability;
            this.secretReference = secretReference;
        }

        public boolean isConfigured() {
            String url = baseUrl == null ? "" : baseUrl.trim();
            boolean secure = url.startsWith("https://");
            boolean local = url.startsWith("http://127.0.0.1")
                    || url.startsWith("http://localhost")
                    || url.startsWith("http://10.0.2.2");
            return (secure || local)
                    && modelId != null && !modelId.trim().isEmpty();
        }
    }

    public static final class AppSettings {
        public int version = 1;
        public boolean onboardingCompleted = false;
        public boolean ocrTestPassed = false;
        public boolean translationTestPassed = false;
        public final LanguageSettings language = new LanguageSettings();
        public final TranslationSettings translation = new TranslationSettings();
        public final OverlaySettings overlay = new OverlaySettings();
        public final ImageSettings imageSettings = new ImageSettings();
        public final PrivacySettings privacySettings = new PrivacySettings();
        public ProcessingMode processingMode = ProcessingMode.TWO_STAGE;
        public final AiEndpointConfig ocrEndpoint = new AiEndpointConfig(
                "ocr-default",
                "硅基流动 OCR",
                ProviderType.SILICON_FLOW,
                "https://api.siliconflow.cn/v1",
                ModelCapability.VISION_AND_TEXT,
                "ocr_api_key"
        );
        public final AiEndpointConfig translationEndpoint = new AiEndpointConfig(
                "translation-default",
                "DeepSeek 翻译",
                ProviderType.DEEPSEEK,
                "https://api.deepseek.com",
                ModelCapability.TEXT,
                "translation_api_key"
        );
    }

    public static final class BoundingBox {
        public final int left;
        public final int top;
        public final int right;
        public final int bottom;

        public BoundingBox(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        static BoundingBox fromJson(JSONObject json) {
            if (json == null) return null;
            return new BoundingBox(
                    json.optInt("left"),
                    json.optInt("top"),
                    json.optInt("right"),
                    json.optInt("bottom")
            );
        }

        boolean isValid() {
            return left >= 0
                    && top >= 0
                    && right <= 1000
                    && bottom <= 1000
                    && right > left
                    && bottom > top;
        }
    }

    public static final class OcrBlock {
        public final String id;
        public final int order;
        public final String language;
        public final String originalText;
        public final BlockType type;
        public final BoundingBox bbox;
        public final double confidence;

        public OcrBlock(String id, int order, String language, String originalText,
                        BlockType type, BoundingBox bbox, double confidence) {
            this.id = id;
            this.order = order;
            this.language = language;
            this.originalText = originalText;
            this.type = type;
            this.bbox = bbox;
            this.confidence = confidence;
        }

        public JSONObject toJson() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("order", order);
            json.put("language", language);
            json.put("original_text", originalText);
            json.put("type", type.name().toLowerCase());
            if (bbox != null) {
                JSONObject box = new JSONObject();
                box.put("left", bbox.left);
                box.put("top", bbox.top);
                box.put("right", bbox.right);
                box.put("bottom", bbox.bottom);
                json.put("bbox", box);
            }
            json.put("confidence", confidence);
            return json;
        }
    }

    public static final class OcrPage {
        public final String pageLanguage;
        public final double languageConfidence;
        public final boolean mixedLanguage;
        public final String readingDirection;
        public final List<OcrBlock> blocks;

        public OcrPage(String pageLanguage, double languageConfidence, boolean mixedLanguage,
                       String readingDirection, List<OcrBlock> blocks) {
            this.pageLanguage = pageLanguage;
            this.languageConfidence = languageConfidence;
            this.mixedLanguage = mixedLanguage;
            this.readingDirection = readingDirection;
            this.blocks = blocks;
        }

        public static OcrPage fromJson(String content) throws JSONException {
            JSONObject root;
            try {
                root = new JSONObject(cleanJsonContent(content));
            } catch (JSONException exception) {
                return fromPlainText(content);
            }
            JSONArray blocksJson = root.optJSONArray("blocks");
            List<OcrBlock> blocks = new ArrayList<>();
            if (blocksJson == null) {
                return fromLooseJson(root, content);
            }
            if (blocksJson != null) {
                for (int i = 0; i < blocksJson.length(); i++) {
                    JSONObject item = blocksJson.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    String original = item.optString("original_text", item.optString("originalText")).trim();
                    if (original.isEmpty()) {
                        continue;
                    }
                    BoundingBox box = BoundingBox.fromJson(item.optJSONObject("bbox"));
                    if (box != null && !box.isValid()) {
                        box = null;
                    }
                    blocks.add(new OcrBlock(
                            item.optString("id", "b" + (i + 1)),
                            item.optInt("order", i + 1),
                            item.optString("language", root.optString("page_language", "unknown")),
                            original,
                            parseBlockType(item.optString("type")),
                            box,
                            item.optDouble("confidence", 0.0)
                    ));
                }
            }
            if (blocks.isEmpty()) {
                return fromLooseJson(root, content);
            }
            return new OcrPage(
                    root.optString("page_language", "unknown"),
                    root.optDouble("language_confidence", 0.0),
                    root.optBoolean("is_mixed_language", false),
                    root.optString("reading_direction", "vertical"),
                    blocks
            );
        }

        private static OcrPage fromPlainText(String content) throws JSONException {
            List<String> lines = extractOcrLines(content);
            List<OcrBlock> blocks = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                blocks.add(new OcrBlock(
                        "b" + (i + 1),
                        i + 1,
                        detectLanguage(lines.get(i)),
                        lines.get(i),
                        BlockType.DIALOGUE,
                        null,
                        0.72
                ));
            }
            if (blocks.isEmpty()) {
                throw new JSONException("OCR 输出无法解析为文字块");
            }
            return new OcrPage(
                    detectPageLanguage(blocks),
                    0.72,
                    hasMixedLanguage(blocks),
                    "rtl",
                    blocks
            );
        }

        private static OcrPage fromLooseJson(JSONObject root, String content) throws JSONException {
            List<String> lines = extractLooseOcrLines(root);
            if (lines.isEmpty()) {
                return fromPlainText(content);
            }
            List<OcrBlock> blocks = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                blocks.add(new OcrBlock(
                        "b" + (i + 1),
                        i + 1,
                        detectLanguage(line),
                        line,
                        BlockType.DIALOGUE,
                        null,
                        0.55
                ));
            }
            return new OcrPage(
                    detectPageLanguage(blocks),
                    0.55,
                    hasMixedLanguage(blocks),
                    root.optString("reading_direction", "rtl"),
                    blocks
            );
        }
    }

    public static final class TranslationBlock {
        public final String id;
        public final int order;
        public final String correctedText;
        public final String translation;
        public final double confidence;
        public final OcrBlock source;

        public TranslationBlock(String id, int order, String correctedText, String translation,
                                double confidence, OcrBlock source) {
            this.id = id;
            this.order = order;
            this.correctedText = correctedText;
            this.translation = translation;
            this.confidence = confidence;
            this.source = source;
        }
    }

    public static final class TranslatedPage {
        public final String detectedSourceLanguage;
        public final String targetLanguage;
        public final String pageSummary;
        public final List<TranslationBlock> blocks;

        public TranslatedPage(String detectedSourceLanguage, String targetLanguage,
                              String pageSummary, List<TranslationBlock> blocks) {
            this.detectedSourceLanguage = detectedSourceLanguage;
            this.targetLanguage = targetLanguage;
            this.pageSummary = pageSummary;
            this.blocks = blocks;
        }

        public static TranslatedPage fromJson(String content, OcrPage ocrPage, TargetLanguage targetLanguage)
                throws JSONException {
            JSONObject root;
            try {
                root = new JSONObject(cleanJsonContent(content));
            } catch (JSONException exception) {
                return fromPlainTranslation(content, ocrPage, targetLanguage);
            }
            JSONArray blocksJson = root.optJSONArray("blocks");
            if (blocksJson == null) {
                return fromPlainTranslation(content, ocrPage, targetLanguage);
            }
            List<TranslationBlock> blocks = new ArrayList<>();
            if (blocksJson != null) {
                for (int i = 0; i < blocksJson.length(); i++) {
                    JSONObject item = blocksJson.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    OcrBlock source = findBlock(ocrPage.blocks, item.optString("id"), item.optInt("order"));
                    if (source == null) {
                        source = i < ocrPage.blocks.size() ? ocrPage.blocks.get(i) : null;
                    }
                    if (source == null) {
                        continue;
                    }
                    String translation = item.optString("translation").trim();
                    if (translation.isEmpty()) {
                        continue;
                    }
                    if (containsBlock(blocks, source.id, source.order)) {
                        continue;
                    }
                    blocks.add(new TranslationBlock(
                            item.optString("id", source.id),
                            item.optInt("order", source.order),
                            item.optString("corrected_text", source.originalText),
                            translation,
                            item.optDouble("translation_confidence", 0.0),
                            source
                    ));
                }
            }
            if (blocks.isEmpty()) {
                return fromPlainTranslation(content, ocrPage, targetLanguage);
            }
            appendMissingTranslations(blocks, ocrPage);
            if (blocks.size() != ocrPage.blocks.size()) {
                return fromPlainTranslation(content, ocrPage, targetLanguage);
            }
            return new TranslatedPage(
                    root.optString("detected_source_language", ocrPage.pageLanguage),
                    root.optString("target_language", targetLanguage.code),
                    root.optString("page_summary"),
                    blocks
            );
        }

        private static TranslatedPage fromPlainTranslation(String content, OcrPage ocrPage, TargetLanguage targetLanguage)
                throws JSONException {
            List<String> lines = extractTranslationLines(content);
            List<TranslationBlock> blocks = new ArrayList<>();
            for (int i = 0; i < ocrPage.blocks.size(); i++) {
                OcrBlock source = ocrPage.blocks.get(i);
                String translation = i < lines.size() ? lines.get(i) : source.originalText;
                if (translation.isEmpty()) {
                    translation = source.originalText;
                }
                blocks.add(new TranslationBlock(
                        source.id,
                        source.order,
                        source.originalText,
                        translation,
                        0.45,
                        source
                ));
            }
            if (blocks.isEmpty()) {
                throw new JSONException("翻译输出无法解析为文字块");
            }
            return new TranslatedPage(
                    ocrPage.pageLanguage,
                    targetLanguage.code,
                    "",
                    blocks
            );
        }

        private static void appendMissingTranslations(List<TranslationBlock> blocks, OcrPage ocrPage) {
            for (OcrBlock source : ocrPage.blocks) {
                if (containsBlock(blocks, source.id, source.order)) {
                    continue;
                }
                blocks.add(new TranslationBlock(
                        source.id,
                        source.order,
                        source.originalText,
                        source.originalText,
                        0.2,
                        source
                ));
            }
        }
    }

    public static final class TranslateFailure extends Exception {
        public final FailureStage stage;
        public final boolean canRetry;

        public TranslateFailure(FailureStage stage, String message, boolean canRetry) {
            super(message);
            this.stage = stage;
            this.canRetry = canRetry;
        }

        public TranslateFailure(FailureStage stage, String message, boolean canRetry, Throwable cause) {
            super(message, cause);
            this.stage = stage;
            this.canRetry = canRetry;
        }
    }

    private static BlockType parseBlockType(String type) {
        if (type == null) return BlockType.UNKNOWN;
        switch (type.toLowerCase()) {
            case "dialogue":
                return BlockType.DIALOGUE;
            case "narration":
                return BlockType.NARRATION;
            case "sound_effect":
                return BlockType.SOUND_EFFECT;
            case "note":
                return BlockType.NOTE;
            default:
                return BlockType.UNKNOWN;
        }
    }

    private static OcrBlock findBlock(List<OcrBlock> blocks, String id, int order) {
        for (OcrBlock block : blocks) {
            if (block.id.equals(id) || block.order == order) {
                return block;
            }
        }
        return null;
    }

    private static boolean containsBlock(List<TranslationBlock> blocks, String id, int order) {
        for (TranslationBlock block : blocks) {
            if (block.id.equals(id) || block.order == order) {
                return true;
            }
        }
        return false;
    }

    public static String cleanJsonContent(String content) {
        if (content == null) return "{}";
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstLine = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                trimmed = trimmed.substring(firstLine + 1, lastFence).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    private static List<String> extractOcrLines(String content) {
        Set<String> lines = new LinkedHashSet<>();
        if (content == null) return new ArrayList<>();
        String text = content;
        Matcher cellMatcher = Pattern.compile("(?is)<t[dh][^>]*>(.*?)</t[dh]>").matcher(text);
        while (cellMatcher.find()) {
            addCleanOcrLine(lines, cellMatcher.group(1));
        }
        Matcher refMatcher = Pattern.compile("(?s)<\\|ref\\|>(.*?)<\\|/ref\\|>").matcher(text);
        while (refMatcher.find()) {
            addCleanOcrLine(lines, refMatcher.group(1));
        }
        text = text.replaceAll("(?is)<[^>]+>", "\n")
                .replaceAll("(?s)<\\|det\\|>.*?<\\|/det\\|>", "\n")
                .replaceAll("(?s)<\\|/?ref\\|>", "\n")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"");
        for (String line : text.split("\\R+")) {
            addCleanOcrLine(lines, line);
        }
        return new ArrayList<>(lines);
    }

    private static void addCleanOcrLine(Set<String> lines, String raw) {
        if (raw == null) return;
        String line = raw.replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (line.isEmpty()) return;
        String lower = line.toLowerCase();
        if ("line".equals(lower) || "text".equals(lower) || "image".equals(lower)) return;
        if ("ja".equals(lower) || "zh".equals(lower) || "ko".equals(lower) || "en".equals(lower)
                || "mixed".equals(lower) || "unknown".equals(lower)
                || "rtl".equals(lower) || "ltr".equals(lower) || "vertical".equals(lower)
                || "dialogue".equals(lower) || "narration".equals(lower)
                || "sound_effect".equals(lower) || "note".equals(lower)
                || "true".equals(lower) || "false".equals(lower)) {
            return;
        }
        if (line.matches("[0-9.]+")) return;
        if (line.length() > 120) return;
        lines.add(line);
    }

    private static List<String> extractLooseOcrLines(Object value) {
        Set<String> lines = new LinkedHashSet<>();
        collectLooseOcrLines(value, lines, "");
        return new ArrayList<>(lines);
    }

    private static void collectLooseOcrLines(Object value, Set<String> lines, String key) {
        if (value == null || value == JSONObject.NULL) return;
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String childKey = keys.next();
                collectLooseOcrLines(object.opt(childKey), lines, childKey);
            }
            return;
        }
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                collectLooseOcrLines(array.opt(i), lines, key);
            }
            return;
        }
        if (value instanceof String && isLikelyOcrTextKey(key)) {
            addCleanOcrLine(lines, (String) value);
        }
    }

    private static boolean isLikelyOcrTextKey(String key) {
        if (key == null) return true;
        String lower = key.toLowerCase();
        if (lower.contains("image") || lower.contains("base64") || lower.contains("url")
                || lower.contains("bbox") || lower.contains("box") || lower.contains("score")
                || lower.contains("confidence") || lower.equals("id") || lower.equals("type")
                || lower.equals("language") || lower.equals("page_language")
                || lower.equals("reading_direction")) {
            return false;
        }
        return lower.contains("text")
                || lower.contains("word")
                || lower.contains("ocr")
                || lower.contains("content")
                || lower.contains("transcription")
                || lower.contains("label")
                || lower.contains("value")
                || lower.contains("result")
                || lower.isEmpty();
    }

    private static List<String> extractTranslationLines(String content) {
        Set<String> lines = new LinkedHashSet<>();
        if (content == null) return new ArrayList<>();
        Matcher matcher = Pattern.compile("\"translation\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(content);
        while (matcher.find()) {
            addCleanOcrLine(lines, unescapeJsonString(matcher.group(1)));
        }
        if (!lines.isEmpty()) {
            return new ArrayList<>(lines);
        }
        return extractOcrLines(content);
    }

    private static String unescapeJsonString(String value) {
        try {
            return new JSONArray("[\"" + value + "\"]").getString(0);
        } catch (JSONException ignored) {
            return value.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
    }

    private static String detectLanguage(String text) {
        boolean hasKana = Pattern.compile("[\\u3040-\\u30ff]").matcher(text).find();
        boolean hasHangul = Pattern.compile("[\\uac00-\\ud7af]").matcher(text).find();
        boolean hasCjk = Pattern.compile("[\\u4e00-\\u9fff]").matcher(text).find();
        if (hasKana) return "ja";
        if (hasHangul) return "ko";
        if (hasCjk) return "zh";
        return "unknown";
    }

    private static String detectPageLanguage(List<OcrBlock> blocks) {
        int ja = 0;
        int zh = 0;
        int ko = 0;
        int unknown = 0;
        for (OcrBlock block : blocks) {
            if ("ja".equals(block.language)) ja++;
            else if ("zh".equals(block.language)) zh++;
            else if ("ko".equals(block.language)) ko++;
            else unknown++;
        }
        if ((ja > 0 && zh > 0) || (ja > 0 && ko > 0) || (zh > 0 && ko > 0)) return "mixed";
        if (ja >= zh && ja >= ko && ja > 0) return "ja";
        if (zh >= ja && zh >= ko && zh > 0) return "zh";
        if (ko > 0) return "ko";
        return unknown > 0 ? "unknown" : "mixed";
    }

    private static boolean hasMixedLanguage(List<OcrBlock> blocks) {
        String first = null;
        for (OcrBlock block : blocks) {
            if ("unknown".equals(block.language)) continue;
            if (first == null) {
                first = block.language;
            } else if (!first.equals(block.language)) {
                return true;
            }
        }
        return false;
    }
}
