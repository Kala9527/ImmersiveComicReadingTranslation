package com.immersivecomic.translator;

import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.immersivecomic.translator.model.Models;
import com.immersivecomic.translator.settings.SecretStore;
import com.immersivecomic.translator.settings.SettingsRepository;

public final class DebugConfigureActivity extends Activity {
    private static final String DEFAULT_BASE_URL = "https://api.siliconflow.cn/v1";
    private static final String DEFAULT_OCR_MODEL = "PaddlePaddle/PaddleOCR-VL-1.5";
    private static final String DEFAULT_TRANSLATION_MODEL = "Qwen/Qwen2.5-7B-Instruct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsRepository repository = new SettingsRepository(this);
        SecretStore secretStore = new SecretStore(this);
        Models.AppSettings settings = repository.load();

        String baseUrl = textExtra("base_url", DEFAULT_BASE_URL);
        configureEndpoint(
                settings.ocrEndpoint,
                "硅基流动 OCR",
                baseUrl,
                textExtra("ocr_model", DEFAULT_OCR_MODEL),
                Models.ModelCapability.VISION_AND_TEXT
        );
        configureEndpoint(
                settings.translationEndpoint,
                "硅基流动翻译",
                baseUrl,
                textExtra("translation_model", DEFAULT_TRANSLATION_MODEL),
                Models.ModelCapability.TEXT
        );
        settings.onboardingCompleted = true;
        settings.language.sourceLanguage = Models.SourceLanguage.AUTO;
        settings.language.targetLanguage = Models.TargetLanguage.SIMPLIFIED_CHINESE;
        settings.processingMode = Models.ProcessingMode.TWO_STAGE;
        boolean testsPassed = getIntent().getBooleanExtra("mark_tests_passed", false);
        settings.ocrTestPassed = testsPassed;
        settings.translationTestPassed = testsPassed;
        repository.save(settings);

        String apiKey = getIntent().getStringExtra("api_key");
        String encodedApiKey = getIntent().getStringExtra("api_key_b64");
        if ((apiKey == null || apiKey.trim().isEmpty())
                && encodedApiKey != null
                && !encodedApiKey.trim().isEmpty()) {
            apiKey = new String(Base64.decode(encodedApiKey.trim(), Base64.DEFAULT), StandardCharsets.UTF_8);
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = readKeyFile();
        }
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                secretStore.saveSecret(settings.ocrEndpoint.secretReference, apiKey.trim());
                secretStore.saveSecret(settings.translationEndpoint.secretReference, apiKey.trim());
                Toast.makeText(this, "Debug 配置已写入", Toast.LENGTH_SHORT).show();
            } catch (Exception exception) {
                Toast.makeText(this, "Debug 密钥写入失败：" + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Debug 配置已写入，未写入密钥", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private String textExtra(String name, String fallback) {
        String value = getIntent().getStringExtra(name);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String readKeyFile() {
        File defaultFile = new File(getExternalFilesDir(null), "debug_value.txt");
        String path = textExtra("value_file", defaultFile.getAbsolutePath());
        File file = new File(path);
        if (!file.exists()) {
            return "";
        }
        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name()).trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private void configureEndpoint(
            Models.AiEndpointConfig endpoint,
            String name,
            String baseUrl,
            String modelId,
            Models.ModelCapability capability
    ) {
        endpoint.provider = Models.ProviderType.SILICON_FLOW;
        endpoint.name = name;
        endpoint.baseUrl = baseUrl;
        endpoint.endpointPath = "/chat/completions";
        endpoint.modelId = modelId;
        endpoint.authentication = Models.AuthenticationType.BEARER;
        endpoint.capability = capability;
        endpoint.maxOutputTokens = 4096;
        endpoint.requestTimeoutSeconds = 90;
        endpoint.enableJsonMode = false;
        endpoint.enableThinking = false;
        endpoint.extraBodyJson = "";
    }
}
