package com.immersivecomic.translator;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.immersivecomic.translator.ai.OpenAiCompatibleClient;
import com.immersivecomic.translator.model.Models;
import com.immersivecomic.translator.settings.SecretStore;
import com.immersivecomic.translator.settings.SettingsRepository;
import com.immersivecomic.translator.util.Ui;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EndpointConfigActivity extends Activity {
    public static final String EXTRA_ENDPOINT_KIND = "endpoint_kind";
    public static final String KIND_OCR = "ocr";
    public static final String KIND_TRANSLATION = "translation";

    private SettingsRepository repository;
    private SecretStore secretStore;
    private Models.AppSettings settings;
    private boolean isOcr;
    private LinearLayout content;
    private TextView keyStatus;
    private TextView testStatus;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private EditText nameEdit;
    private EditText baseUrlEdit;
    private EditText pathEdit;
    private EditText modelEdit;
    private EditText timeoutEdit;
    private EditText tokensEdit;
    private EditText extraJsonEdit;
    private Switch jsonModeSwitch;
    private Switch thinkingSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new SettingsRepository(this);
        secretStore = new SecretStore(this);
        isOcr = KIND_OCR.equals(getIntent().getStringExtra(EXTRA_ENDPOINT_KIND));
        settings = repository.load();
        render();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void render() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Ui.PAPER);
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 28));
        scrollView.addView(content);
        setContentView(scrollView);

        Models.AiEndpointConfig endpoint = endpoint();
        LinearLayout hero = Ui.heroPanel(this);
        hero.addView(Ui.title(this, isOcr ? "OCR 模型配置" : "纠错翻译模型配置"));
        hero.addView(Ui.label(this, isOcr
                ? "先保存 API Key，再填写 OCR 模型 ID 和测试图片识别能力。"
                : "先保存 API Key，再填写纠错翻译模型 ID 并测试结构化翻译能力。"));
        content.addView(hero);
        Ui.addSpace(content, 14);

        addKeyCard(endpoint);
        addModelCard(endpoint);
        addAdvancedCard(endpoint);
        addTestCard(endpoint);
    }

    private void addKeyCard(Models.AiEndpointConfig endpoint) {
        LinearLayout card = Ui.comicCard(this);
        card.addView(Ui.sectionTitle(this, "1. 保存 API Key"));
        keyStatus = Ui.label(this, secretStore.hasSecret(endpoint.secretReference)
                ? "已安全保存。更新 Key 不会清空模型 ID。"
                : "未保存。请先保存密钥，再填写模型。");
        card.addView(keyStatus);
        Ui.addSpace(card, 10);
        Button saveKey = Ui.button(this, secretStore.hasSecret(endpoint.secretReference) ? "更新 API Key" : "保存 API Key",
                Ui.PINK, Color.WHITE);
        saveKey.setOnClickListener(v -> showApiKeyDialog(endpoint));
        card.addView(saveKey, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addModelCard(Models.AiEndpointConfig endpoint) {
        LinearLayout card = Ui.comicCard(this);
        card.addView(Ui.sectionTitle(this, "2. 填写模型配置"));
        card.addView(Ui.label(this, "密钥保存后再保存本页模型配置，避免未保存输入被刷新。"));
        Ui.addSpace(card, 10);
        addProviderRow(card, endpoint);
        nameEdit = addEdit(card, "显示名称", endpoint.name, false);
        baseUrlEdit = addEdit(card, "Base URL", endpoint.baseUrl, false);
        pathEdit = addEdit(card, "接口路径", endpoint.endpointPath, false);
        modelEdit = addEdit(card, "模型 ID", endpoint.modelId, false);
        if (isOcr) {
            addImageDetailRow(card, endpoint);
        }
        Button preset = Ui.ghostButton(this, isOcr ? "填入 SiliconFlow OCR 推荐配置" : "填入 SiliconFlow Qwen 推荐配置");
        preset.setOnClickListener(v -> applyRecommendedPreset(endpoint));
        card.addView(preset, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        Ui.addSpace(card, 8);
        Button save = Ui.button(this, "保存模型配置", Ui.TEAL, Color.WHITE);
        save.setOnClickListener(v -> saveEndpointConfig());
        card.addView(save, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addAdvancedCard(Models.AiEndpointConfig endpoint) {
        LinearLayout card = Ui.comicCard(this);
        card.addView(Ui.sectionTitle(this, "3. 高级参数"));
        timeoutEdit = addEdit(card, "超时时间（秒）", String.valueOf(endpoint.requestTimeoutSeconds), false);
        timeoutEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        tokensEdit = addEdit(card, "最大输出 tokens", String.valueOf(endpoint.maxOutputTokens), false);
        tokensEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        jsonModeSwitch = new Switch(this);
        jsonModeSwitch.setText("JSON 模式");
        jsonModeSwitch.setTextColor(Ui.INK);
        jsonModeSwitch.setChecked(endpoint.enableJsonMode);
        thinkingSwitch = new Switch(this);
        thinkingSwitch.setText("思考模式");
        thinkingSwitch.setTextColor(Ui.INK);
        thinkingSwitch.setChecked(endpoint.enableThinking);
        card.addView(jsonModeSwitch);
        card.addView(thinkingSwitch);
        Ui.addSpace(card, 8);
        extraJsonEdit = addEdit(card, "附加请求参数 JSON", endpoint.extraBodyJson, false);
        extraJsonEdit.setSingleLine(false);
        extraJsonEdit.setMinLines(3);
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addTestCard(Models.AiEndpointConfig endpoint) {
        LinearLayout card = Ui.comicCard(this);
        card.addView(Ui.sectionTitle(this, "4. 测试模型"));
        testStatus = Ui.label(this, isTestPassed() ? "当前测试状态：通过" : "当前测试状态：待测试");
        card.addView(testStatus);
        Ui.addSpace(card, 10);
        Button test = Ui.button(this, isOcr ? "测试 OCR 模型" : "测试纠错翻译模型", Ui.PURPLE, Color.WHITE);
        test.setOnClickListener(v -> runModelTest());
        card.addView(test, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        Ui.addSpace(card, 8);
        Button back = Ui.outlineButton(this, "返回首页");
        back.setOnClickListener(v -> finish());
        card.addView(back, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        content.addView(card);
    }

    private void addProviderRow(LinearLayout parent, Models.AiEndpointConfig endpoint) {
        addChoiceRow(parent, "服务商", endpoint.provider.label, v -> {
            Models.ProviderType[] values = Models.ProviderType.values();
            String[] labels = new String[values.length];
            for (int i = 0; i < values.length; i++) labels[i] = values[i].label;
            new AlertDialog.Builder(this)
                    .setTitle("服务商")
                    .setSingleChoiceItems(labels, endpoint.provider.ordinal(), (dialog, which) -> {
                        endpoint.provider = values[which];
                        applyProviderDefaults(endpoint);
                        updateEndpointFields(endpoint);
                        dialog.dismiss();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void addImageDetailRow(LinearLayout parent, Models.AiEndpointConfig endpoint) {
        addChoiceRow(parent, "图片质量", endpoint.imageDetail.wireName, v -> {
            Models.ImageDetail[] values = Models.ImageDetail.values();
            String[] labels = new String[values.length];
            for (int i = 0; i < values.length; i++) labels[i] = values[i].wireName;
            new AlertDialog.Builder(this)
                    .setTitle("图片质量")
                    .setSingleChoiceItems(labels, endpoint.imageDetail.ordinal(), (dialog, which) -> {
                        endpoint.imageDetail = values[which];
                        dialog.dismiss();
                        render();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void addChoiceRow(LinearLayout parent, String label, String value, View.OnClickListener listener) {
        LinearLayout row = Ui.row(this);
        LinearLayout texts = Ui.column(this);
        texts.addView(Ui.label(this, label));
        texts.addView(Ui.text(this, value, 15, Ui.INK, android.graphics.Typeface.BOLD));
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button action = Ui.iconButton(this, "›");
        row.addView(action, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        row.setOnClickListener(listener);
        action.setOnClickListener(listener);
        parent.addView(row);
        Ui.addSpace(parent, 8);
    }

    private EditText addEdit(LinearLayout parent, String label, String value, boolean secret) {
        parent.addView(Ui.label(this, label));
        EditText editText = Ui.edit(this, label, value, secret);
        if (label.contains("模型")) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        parent.addView(editText);
        Ui.addSpace(parent, 8);
        return editText;
    }

    private void showApiKeyDialog(Models.AiEndpointConfig endpoint) {
        EditText input = Ui.edit(this, "输入新的 API Key", "", true);
        input.setHint(secretStore.hasSecret(endpoint.secretReference) ? "留空不修改；输入新 Key 会覆盖" : "请输入 API Key");
        LinearLayout body = Ui.column(this);
        int padding = Ui.dp(this, 18);
        body.setPadding(padding, Ui.dp(this, 8), padding, 0);
        body.addView(input);
        new AlertDialog.Builder(this)
                .setTitle(endpoint.name + " API Key")
                .setView(body)
                .setPositiveButton("保存", (dialog, which) -> {
                    try {
                        String key = input.getText().toString();
                        if (!key.isEmpty()) {
                            secretStore.saveSecret(endpoint.secretReference, key);
                            invalidateTestResult();
                            repository.save(settings);
                            toast("API Key 已安全保存；现在可以填写模型 ID。");
                            if (keyStatus != null) {
                                keyStatus.setText("已安全保存。更新 Key 不会清空模型 ID。");
                            }
                            if (testStatus != null) {
                                testStatus.setText("当前测试状态：待测试");
                            }
                        }
                    } catch (Exception exception) {
                        toast("保存 API Key 失败：" + exception.getMessage());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveEndpointConfig() {
        Models.AiEndpointConfig endpoint = endpoint();
        endpoint.name = nameEdit.getText().toString().trim();
        endpoint.baseUrl = baseUrlEdit.getText().toString().trim();
        endpoint.endpointPath = pathEdit.getText().toString().trim();
        endpoint.modelId = modelEdit.getText().toString().trim();
        endpoint.requestTimeoutSeconds = parseLong(timeoutEdit.getText().toString(), endpoint.requestTimeoutSeconds, 10, 240);
        endpoint.maxOutputTokens = parseInt(tokensEdit.getText().toString(), endpoint.maxOutputTokens, 512, 16384);
        endpoint.enableJsonMode = jsonModeSwitch.isChecked();
        endpoint.enableThinking = thinkingSwitch.isChecked();
        endpoint.extraBodyJson = extraJsonEdit.getText().toString().trim();
        invalidateTestResult();
        repository.save(settings);
        toast("模型配置已保存");
        settings = repository.load();
        render();
    }

    private void runModelTest() {
        saveEndpointConfig();
        Models.AiEndpointConfig endpoint = endpoint();
        if (!endpoint.isConfigured()) {
            toast("请先填写 Base URL 与模型 ID。");
            return;
        }
        executor.execute(() -> {
            try {
                String apiKey = secretStore.readSecret(endpoint.secretReference);
                if (apiKey.isEmpty()) {
                    runOnUiThread(() -> toast("请先保存 API Key。"));
                    return;
                }
                OpenAiCompatibleClient client = new OpenAiCompatibleClient();
                String result = isOcr ? client.testOcr(settings, apiKey) : client.testTranslation(settings, apiKey);
                repository.markTestResult(isOcr, true);
                runOnUiThread(() -> {
                    settings = repository.load();
                    toast(result);
                    render();
                });
            } catch (Models.TranslateFailure failure) {
                repository.markTestResult(isOcr, false);
                runOnUiThread(() -> {
                    settings = repository.load();
                    toast(failure.stage.name() + "：" + failure.getMessage());
                    render();
                });
            } catch (Exception exception) {
                repository.markTestResult(isOcr, false);
                runOnUiThread(() -> {
                    settings = repository.load();
                    toast("测试失败：" + exception.getMessage());
                    render();
                });
            }
        });
        toast(isOcr ? "正在测试 OCR 模型..." : "正在测试纠错翻译模型...");
    }

    private void applyRecommendedPreset(Models.AiEndpointConfig endpoint) {
        endpoint.provider = Models.ProviderType.SILICON_FLOW;
        endpoint.name = isOcr ? "硅基流动 OCR" : "硅基流动翻译";
        endpoint.baseUrl = "https://api.siliconflow.cn/v1";
        endpoint.endpointPath = "/chat/completions";
        endpoint.authentication = Models.AuthenticationType.BEARER;
        endpoint.modelId = isOcr ? "PaddlePaddle/PaddleOCR-VL-1.5" : "Qwen/Qwen2.5-7B-Instruct";
        endpoint.capability = isOcr ? Models.ModelCapability.VISION_AND_TEXT : Models.ModelCapability.TEXT;
        updateEndpointFields(endpoint);
    }

    private void applyProviderDefaults(Models.AiEndpointConfig endpoint) {
        switch (endpoint.provider) {
            case DEEPSEEK:
                endpoint.name = isOcr ? "DeepSeek OCR" : "DeepSeek 翻译";
                endpoint.baseUrl = "https://api.deepseek.com";
                endpoint.endpointPath = "/chat/completions";
                endpoint.authentication = Models.AuthenticationType.BEARER;
                break;
            case SILICON_FLOW:
                endpoint.name = isOcr ? "硅基流动 OCR" : "硅基流动翻译";
                endpoint.baseUrl = "https://api.siliconflow.cn/v1";
                endpoint.endpointPath = "/chat/completions";
                endpoint.authentication = Models.AuthenticationType.BEARER;
                if (endpoint.modelId.isEmpty()) {
                    endpoint.modelId = isOcr ? "PaddlePaddle/PaddleOCR-VL-1.5" : "Qwen/Qwen2.5-7B-Instruct";
                }
                break;
            case ALIBABA_QWEN:
                endpoint.name = isOcr ? "百炼 OCR" : "百炼翻译";
                endpoint.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
                endpoint.endpointPath = "/chat/completions";
                endpoint.authentication = Models.AuthenticationType.BEARER;
                break;
            case OPENAI_COMPATIBLE:
            default:
                endpoint.name = isOcr ? "自定义 OCR" : "自定义翻译";
                endpoint.endpointPath = "/chat/completions";
                break;
        }
        endpoint.capability = isOcr ? Models.ModelCapability.VISION_AND_TEXT : Models.ModelCapability.TEXT;
    }

    private void updateEndpointFields(Models.AiEndpointConfig endpoint) {
        nameEdit.setText(endpoint.name);
        baseUrlEdit.setText(endpoint.baseUrl);
        pathEdit.setText(endpoint.endpointPath);
        modelEdit.setText(endpoint.modelId);
    }

    private Models.AiEndpointConfig endpoint() {
        return isOcr ? settings.ocrEndpoint : settings.translationEndpoint;
    }

    private boolean isTestPassed() {
        return isOcr ? settings.ocrTestPassed : settings.translationTestPassed;
    }

    private void invalidateTestResult() {
        if (isOcr) {
            settings.ocrTestPassed = false;
        } else {
            settings.translationTestPassed = false;
        }
        repository.markTestResult(isOcr, false);
    }

    private int parseInt(String value, int fallback, int min, int max) {
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback, long min, long max) {
        try {
            long parsed = Long.parseLong(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }
}
