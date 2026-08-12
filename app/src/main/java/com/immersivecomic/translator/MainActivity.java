package com.immersivecomic.translator;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.immersivecomic.translator.ai.OpenAiCompatibleClient;
import com.immersivecomic.translator.model.Models;
import com.immersivecomic.translator.overlay.OverlayTranslationService;
import com.immersivecomic.translator.settings.SecretStore;
import com.immersivecomic.translator.settings.SettingsRepository;
import com.immersivecomic.translator.settings.TranslationHistoryRepository;
import com.immersivecomic.translator.util.Ui;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private SettingsRepository repository;
    private SecretStore secretStore;
    private TranslationHistoryRepository historyRepository;
    private Models.AppSettings appSettings;
    private LinearLayout content;
    private ScrollView rootScrollView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TextView stateLabel;
    private TextView languageSummary;
    private TextView ocrSummary;
    private TextView translationSummary;
    private TextView displaySummary;
    private TextView checklist;
    private View languageCardView;
    private View ocrCardView;
    private View translationCardView;
    private View displayCardView;
    private View testCardView;
    private boolean onboardingExpanded;
    private boolean languageExpanded = true;
    private boolean ocrExpanded = true;
    private boolean translationEndpointExpanded = true;
    private boolean translationExpanded;
    private boolean displayExpanded;
    private boolean testExpanded = true;
    private boolean privacyExpanded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new SettingsRepository(this);
        secretStore = new SecretStore(this);
        historyRepository = new TranslationHistoryRepository(this);
        appSettings = repository.load();
        requestNotificationPermissionIfNeeded();
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        appSettings = repository.load();
        updateSummaries();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void render() {
        rootScrollView = new ScrollView(this);
        rootScrollView.setFillViewport(false);
        rootScrollView.setBackgroundColor(Ui.PAPER);
        content = Ui.column(this);
        content.setPadding(Ui.dp(this, 18), Ui.dp(this, 20), Ui.dp(this, 18), Ui.dp(this, 28));
        rootScrollView.addView(content);
        setContentView(rootScrollView);

        addHeader();
        addHomeStatusCard();
        addOnboardingCard();
        addLanguageCard();
        addTranslationCard();
        addDisplayCard();
        addPrivacyCard();
        updateSummaries();
    }

    private void addHeader() {
        LinearLayout hero = Ui.heroPanel(this);
        LinearLayout row = Ui.row(this);
        row.addView(Ui.icon(this, R.drawable.ic_translate, 48));
        LinearLayout titleBlock = Ui.column(this);
        titleBlock.addView(Ui.title(this, "漫画悬浮翻译"));
        titleBlock.addView(Ui.label(this, "把漫画对白变成侧边译文。先配置模型，再进入漫画 App 点击悬浮球。"));
        row.addView(titleBlock, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        hero.addView(row);
        Ui.addSpace(hero, 14);

        LinearLayout cover = Ui.row(this);
        TextView bubble = Ui.text(this, "译", 22, Color.WHITE, android.graphics.Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(Ui.stroke(Ui.PINK, Ui.LINE, Ui.dp(this, 24), Ui.dp(this, 2)));
        cover.addView(bubble, new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 58)));

        LinearLayout preview = Ui.column(this);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        previewLp.setMargins(Ui.dp(this, 12), 0, 0, 0);
        preview.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        preview.setBackground(Ui.stroke(Color.WHITE, Ui.LINE, Ui.dp(this, 8), Ui.dp(this, 1)));
        preview.addView(Ui.text(this, "もう帰るの？", 14, Ui.MUTED, android.graphics.Typeface.NORMAL));
        preview.addView(Ui.text(this, "你已经要回去了吗？", 17, Ui.INK, android.graphics.Typeface.BOLD));
        cover.addView(preview, previewLp);
        hero.addView(cover);

        Ui.addSpace(hero, 12);
        LinearLayout chips = Ui.row(this);
        chips.addView(Ui.chip(this, "OCR", Ui.SOFT_TEAL));
        chips.addView(Ui.chip(this, "纠错翻译", Ui.SOFT_PINK));
        chips.addView(Ui.chip(this, "侧边面板", Color.rgb(255, 243, 205)));
        hero.addView(chips);

        content.addView(hero);
        Ui.addSpace(content, 14);
    }

    private void addHomeStatusCard() {
        LinearLayout card = Ui.comicCard(this);
        LinearLayout row = Ui.row(this);
        LinearLayout left = Ui.column(this);
        left.addView(Ui.sectionTitle(this, "当前状态"));
        stateLabel = Ui.text(this, "检查中", 15, Ui.MUTED, android.graphics.Typeface.NORMAL);
        left.addView(stateLabel);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button stop = Ui.outlineButton(this, "停止并清除");
        stop.setOnClickListener(v -> stopOverlayService());
        row.addView(stop, new LinearLayout.LayoutParams(Ui.dp(this, 118), Ui.dp(this, 46)));
        card.addView(row);

        Ui.addSpace(card, 14);
        languageSummary = addStatusRow(card, "语言", "自动识别 → 简体中文",
                v -> scrollToCard(languageCardView, "语言区域"));
        ocrSummary = addStatusRow(card, "OCR 模型", "",
                v -> openEndpointConfig(true));
        translationSummary = addStatusRow(card, "翻译模型", "",
                v -> openEndpointConfig(false));
        displaySummary = addStatusRow(card, "显示方式", "",
                v -> scrollToCard(displayCardView, "显示方式区域"));

        Ui.addSpace(card, 14);
        checklist = Ui.label(this, "");
        card.addView(checklist);
        Ui.addSpace(card, 14);

        Button start = Ui.button(this, "启动悬浮翻译", Ui.PINK, Color.WHITE);
        start.setOnClickListener(v -> startFlow());
        card.addView(start, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 52)));
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private TextView addStatusRow(LinearLayout card, String title, String summary, View.OnClickListener listener) {
        LinearLayout row = Ui.row(this);
        LinearLayout texts = Ui.column(this);
        texts.addView(Ui.sectionTitle(this, title));
        TextView summaryView = Ui.label(this, summary);
        texts.addView(summaryView);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button edit = Ui.outlineButton(this, "修改");
        edit.setOnClickListener(listener);
        row.addView(edit, new LinearLayout.LayoutParams(Ui.dp(this, 78), Ui.dp(this, 44)));
        card.addView(row);
        Ui.addSpace(card, 10);
        return summaryView;
    }

    private void addOnboardingCard() {
        LinearLayout card = addCollapsibleSection("首次启动向导", "4 步完成必要配置后再申请屏幕捕获权限", onboardingExpanded,
                expanded -> onboardingExpanded = expanded);
        Ui.addSpace(card, 10);
        addStep(card, "1", "配置语言", "原文可自动识别，目标语言必须明确选择。");
        addStep(card, "2", "配置云端 OCR", "填写 OpenAI 兼容视觉模型地址、模型 ID 和 Key。");
        addStep(card, "3", "配置纠错翻译模型", "填写文本模型，并选择自然、直译、简洁或漫画化风格。");
        addStep(card, "4", "配置显示方式", "默认右侧面板、70% 宽度、显示原文、自动展开。");
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addStep(LinearLayout parent, String number, String title, String detail) {
        LinearLayout row = Ui.row(this);
        TextView circle = Ui.text(this, number, 14, Color.WHITE, android.graphics.Typeface.BOLD);
        circle.setGravity(Gravity.CENTER);
        circle.setBackground(Ui.round(Ui.TEAL, Ui.dp(this, 16)));
        row.addView(circle, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));
        LinearLayout texts = Ui.column(this);
        texts.addView(Ui.text(this, title, 15, Ui.INK, android.graphics.Typeface.BOLD));
        texts.addView(Ui.label(this, detail));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(Ui.dp(this, 10), 0, 0, 0);
        row.addView(texts, lp);
        parent.addView(row);
        Ui.addSpace(parent, 8);
    }

    private void addLanguageCard() {
        LinearLayout card = addCollapsibleSection("语言", "自动识别 → " + appSettings.language.targetLanguage.label,
                languageExpanded, expanded -> languageExpanded = expanded);
        languageCardView = card;
        card.addView(Ui.label(this, "原文语言默认自动识别；目标语言必须明确选择。"));
        Ui.addSpace(card, 10);
        addEnumSpinner(card, "原文语言", Models.SourceLanguage.values(), appSettings.language.sourceLanguage,
                value -> appSettings.language.sourceLanguage = value);
        addEnumSpinner(card, "目标语言", Models.TargetLanguage.values(), appSettings.language.targetLanguage,
                value -> appSettings.language.targetLanguage = value);
        addSwitch(card, "自动识别后锁定主要语言", appSettings.language.lockDetectedLanguage,
                (button, checked) -> appSettings.language.lockDetectedLanguage = checked);
        addSwitch(card, "允许多语言文字块", appSettings.language.allowMixedLanguage,
                (button, checked) -> appSettings.language.allowMixedLanguage = checked);
        addSaveButton(card);
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addEndpointCard(boolean ocr) {
        Models.AiEndpointConfig endpoint = ocr ? appSettings.ocrEndpoint : appSettings.translationEndpoint;
        boolean expanded = ocr ? ocrExpanded : translationEndpointExpanded;
        LinearLayout card = addCollapsibleSection(
                ocr ? "OCR 模型" : "纠错翻译模型",
                endpoint.provider.label + " / " + (endpoint.modelId.isEmpty() ? "待填写模型 ID" : endpoint.modelId),
                expanded,
                value -> {
                    if (ocr) {
                        ocrExpanded = value;
                    } else {
                        translationEndpointExpanded = value;
                    }
                }
        );
        if (ocr) {
            ocrCardView = card;
        } else {
            translationCardView = card;
        }
        card.addView(Ui.label(this, ocr
                ? "推荐使用支持图片输入的视觉/OCR 模型。"
                : "推荐使用文本模型，负责 OCR 纠错、上下文整理和自然翻译。"));
        Ui.addSpace(card, 10);
        addEnumSpinner(card, "服务商", Models.ProviderType.values(), endpoint.provider, value -> {
            if (endpoint.provider == value) return;
            endpoint.provider = value;
            applyProviderDefaults(endpoint, ocr);
            invalidateModelTests(ocr);
            render();
        });
        EditText name = addEdit(card, "显示名称", endpoint.name, false);
        EditText baseUrl = addEdit(card, "Base URL", endpoint.baseUrl, false);
        EditText path = addEdit(card, "接口路径", endpoint.endpointPath, false);
        EditText model = addEdit(card, "模型 ID", endpoint.modelId, false);
        addActionRow(card, "API Key", secretStore.hasSecret(endpoint.secretReference) ? "已安全保存，点击更新" : "未保存，点击填写",
                v -> showApiKeyDialog(endpoint, ocr));
        if (ocr) {
            addEnumSpinner(card, "图片质量", Models.ImageDetail.values(), endpoint.imageDetail,
                    value -> endpoint.imageDetail = value);
        }
        addActionRow(card, "高级参数",
                "超时 " + endpoint.requestTimeoutSeconds + " 秒 / 输出 " + endpoint.maxOutputTokens + " tokens",
                v -> showAdvancedEndpointDialog(endpoint, ocr));

        Button save = Ui.button(this, ocr ? "保存 OCR 配置" : "保存翻译配置", Ui.TEAL, Color.WHITE);
        save.setOnClickListener(v -> {
            endpoint.name = name.getText().toString().trim();
            endpoint.baseUrl = baseUrl.getText().toString().trim();
            endpoint.endpointPath = path.getText().toString().trim();
            endpoint.modelId = model.getText().toString().trim();
            invalidateModelTests(ocr);
            saveSettings(true);
        });
        card.addView(save, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addTranslationCard() {
        LinearLayout card = addCollapsibleSection("翻译偏好", appSettings.translation.style.label + " / 上下文纠错",
                translationExpanded, expanded -> translationExpanded = expanded);
        addEnumSpinner(card, "翻译风格", Models.TranslationStyle.values(), appSettings.translation.style,
                value -> appSettings.translation.style = value);
        addSwitch(card, "纠正明显 OCR 错误", appSettings.translation.correctOcrErrors,
                (button, checked) -> appSettings.translation.correctOcrErrors = checked);
        addSwitch(card, "结合整页上下文翻译", appSettings.translation.usePageContext,
                (button, checked) -> appSettings.translation.usePageContext = checked);
        addSwitch(card, "参考上一页内容", appSettings.translation.usePreviousPageContext,
                (button, checked) -> appSettings.translation.usePreviousPageContext = checked);
        addSwitch(card, "翻译拟声词", appSettings.translation.translateSoundEffects,
                (button, checked) -> appSettings.translation.translateSoundEffects = checked);
        addSwitch(card, "保留角色称呼", appSettings.translation.preserveNames,
                (button, checked) -> appSettings.translation.preserveNames = checked);
        addSwitch(card, "保留日文敬称", appSettings.translation.preserveHonorifics,
                (button, checked) -> appSettings.translation.preserveHonorifics = checked);
        addSaveButton(card);
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addDisplayCard() {
        LinearLayout card = addCollapsibleSection("显示方式",
                (appSettings.overlay.panelSide == Models.ScreenSide.RIGHT ? "右侧" : "左侧")
                        + " / 宽度 " + Math.round(appSettings.overlay.panelWidthPercent * 100) + "%",
                displayExpanded,
                expanded -> displayExpanded = expanded);
        displayCardView = card;
        addEnumSpinner(card, "悬浮球位置", Models.ScreenSide.values(), appSettings.overlay.bubbleSide,
                value -> appSettings.overlay.bubbleSide = value);
        addEnumSpinner(card, "面板位置", Models.ScreenSide.values(), appSettings.overlay.panelSide,
                value -> appSettings.overlay.panelSide = value);
        addSeek(card, "面板宽度", 55, 86, Math.round(appSettings.overlay.panelWidthPercent * 100),
                value -> appSettings.overlay.panelWidthPercent = value / 100f, "%");
        addSeek(card, "字体大小", 85, 130, Math.round(appSettings.overlay.fontScale * 100),
                value -> appSettings.overlay.fontScale = value / 100f, "%");
        addSeek(card, "背景透明度", 78, 100, Math.round(appSettings.overlay.panelOpacity * 100),
                value -> appSettings.overlay.panelOpacity = value / 100f, "%");
        addSwitch(card, "显示原文", appSettings.overlay.showOriginalText,
                (button, checked) -> {
                    appSettings.overlay.showOriginalText = checked;
                    appSettings.translation.showOriginalText = checked;
                });
        addSwitch(card, "翻译成功后自动展开", appSettings.overlay.autoExpandAfterTranslation,
                (button, checked) -> appSettings.overlay.autoExpandAfterTranslation = checked);
        addSwitch(card, "记住悬浮球位置", appSettings.overlay.rememberBubblePosition,
                (button, checked) -> appSettings.overlay.rememberBubblePosition = checked);
        addPreview(card);
        addSaveButton(card);
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addTestCard() {
        LinearLayout card = addCollapsibleSection("配置测试",
                (appSettings.ocrTestPassed && appSettings.translationTestPassed) ? "已通过" : "建议启动前完成",
                testExpanded,
                expanded -> testExpanded = expanded);
        testCardView = card;
        card.addView(Ui.label(this, "测试会真实请求你配置的模型服务，不会使用内置密钥。"));
        Ui.addSpace(card, 10);
        LinearLayout row = Ui.row(this);
        Button ocr = Ui.button(this, "测试 OCR", Ui.TEAL, Color.WHITE);
        Button translation = Ui.button(this, "测试翻译", Ui.PURPLE, Color.WHITE);
        ocr.setOnClickListener(v -> runModelTest(true));
        translation.setOnClickListener(v -> runModelTest(false));
        row.addView(ocr, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));
        row.addView(translation, new LinearLayout.LayoutParams(0, Ui.dp(this, 50), 1));
        card.addView(row);
        Ui.addSpace(card, 8);
        Button history = Ui.outlineButton(this, "翻译历史");
        history.setOnClickListener(v -> showHistoryDialog());
        card.addView(history, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        content.addView(card);
        Ui.addSpace(content, 14);
    }

    private void addPrivacyCard() {
        LinearLayout card = addCollapsibleSection("隐私说明", "截图只在点击悬浮球后产生",
                privacyExpanded, expanded -> privacyExpanded = expanded);
        card.addView(Ui.label(this, "截图只在点击悬浮球后产生，并发送给你配置的 OCR 服务。API Key 由 Android Keystore 加密保存，不导出明文。默认不保存截图，翻译历史上限 50 条。"));
        Ui.addSpace(card, 8);
        addSwitch(card, "保存最近翻译历史", appSettings.privacySettings.saveTranslationHistory,
                (button, checked) -> appSettings.privacySettings.saveTranslationHistory = checked);
        Button clear = Ui.outlineButton(this, "停止并清除当前会话");
        clear.setOnClickListener(v -> stopOverlayService());
        card.addView(clear, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 46)));
        content.addView(card);
    }

    private LinearLayout addCollapsibleSection(String title, String summary, boolean expanded,
                                               BoolConsumer consumer) {
        LinearLayout card = Ui.comicCard(this);
        LinearLayout header = Ui.row(this);
        LinearLayout texts = Ui.column(this);
        texts.addView(Ui.sectionTitle(this, title));
        texts.addView(Ui.label(this, summary));
        header.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button toggle = Ui.iconButton(this, expanded ? "▲" : "▼");
        header.addView(toggle, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        View.OnClickListener listener = v -> {
            consumer.accept(!expanded);
            render();
            rootScrollView.post(() -> rootScrollView.smoothScrollTo(0, Math.max(0, card.getTop() - Ui.dp(this, 10))));
        };
        header.setOnClickListener(listener);
        toggle.setOnClickListener(listener);
        card.addView(header);
        Ui.addSpace(card, 8);
        card.addView(Ui.divider(this));
        Ui.addSpace(card, 10);

        int bodyStart = card.getChildCount();
        if (!expanded) {
            card.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    if (((LinearLayout) parent).indexOfChild(child) >= bodyStart) {
                        child.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {
                }
            });
        }
        return card;
    }

    private <T extends Enum<T>> void addEnumSpinner(LinearLayout parent, String label, T[] values, T selected,
                                                    EnumConsumer<T> consumer) {
        LinearLayout row = Ui.row(this);
        LinearLayout texts = Ui.column(this);
        texts.addView(Ui.label(this, label));
        TextView valueText = Ui.text(this, enumLabel(selected), 15, Ui.INK, android.graphics.Typeface.BOLD);
        texts.addView(valueText);
        row.addView(texts, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button choose = Ui.iconButton(this, "▼");
        row.addView(choose, new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44)));
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = enumLabel(values[i]);
        }
        View.OnClickListener listener = v -> new AlertDialog.Builder(this)
                .setTitle(label)
                .setSingleChoiceItems(labels, selected.ordinal(), (dialog, which) -> {
                    if (values[which] != selected) {
                        boolean endpointChanged = values[which] instanceof Models.ProviderType
                                || values[which] instanceof Models.ImageDetail;
                        if (endpointChanged) {
                            invalidateModelTests(values[which] instanceof Models.ImageDetail
                                    || label.contains("OCR"));
                        }
                        consumer.accept(values[which]);
                        updateSummaries();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
        row.setOnClickListener(listener);
        choose.setOnClickListener(listener);
        parent.addView(row);
        Ui.addSpace(parent, 8);
    }

    private void addActionRow(LinearLayout parent, String label, String value, View.OnClickListener listener) {
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

    private void showApiKeyDialog(Models.AiEndpointConfig endpoint, boolean ocr) {
        EditText input = Ui.edit(this, "输入新的 API Key", "", true);
        input.setHint(secretStore.hasSecret(endpoint.secretReference) ? "留空不修改；输入新 Key 会覆盖" : "请输入 API Key");
        int padding = Ui.dp(this, 18);
        LinearLayout body = Ui.column(this);
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
                            invalidateModelTests(ocr);
                            saveSettings(false);
                            toast("API Key 已安全保存");
                            render();
                        }
                    } catch (Exception exception) {
                        toast("保存 API Key 失败：" + exception.getMessage());
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showHistoryDialog() {
        List<String> items = historyRepository.loadSummaries();
        if (items.isEmpty()) {
            toast("还没有翻译历史。完成一次悬浮翻译后会显示在这里。");
            return;
        }
        String[] labels = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("翻译历史")
                .setItems(labels, null)
                .setPositiveButton("清空", (dialog, which) -> {
                    historyRepository.clear();
                    toast("已清空翻译历史");
                })
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showAdvancedEndpointDialog(Models.AiEndpointConfig endpoint, boolean ocr) {
        LinearLayout body = Ui.column(this);
        int padding = Ui.dp(this, 18);
        body.setPadding(padding, Ui.dp(this, 8), padding, 0);
        EditText timeout = Ui.edit(this, "超时时间（秒）", String.valueOf(endpoint.requestTimeoutSeconds), false);
        timeout.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText tokens = Ui.edit(this, "最大输出 tokens", String.valueOf(endpoint.maxOutputTokens), false);
        tokens.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText extra = Ui.edit(this, "附加请求参数 JSON", endpoint.extraBodyJson, false);
        extra.setSingleLine(false);
        extra.setMinLines(3);
        Switch jsonMode = new Switch(this);
        jsonMode.setText("JSON 模式");
        jsonMode.setTextColor(Ui.INK);
        jsonMode.setChecked(endpoint.enableJsonMode);
        Switch thinking = new Switch(this);
        thinking.setText("思考模式");
        thinking.setTextColor(Ui.INK);
        thinking.setChecked(endpoint.enableThinking);
        body.addView(Ui.label(this, "超时时间"));
        body.addView(timeout);
        Ui.addSpace(body, 8);
        body.addView(Ui.label(this, "最大输出"));
        body.addView(tokens);
        Ui.addSpace(body, 8);
        body.addView(jsonMode);
        body.addView(thinking);
        Ui.addSpace(body, 8);
        body.addView(Ui.label(this, "附加请求参数"));
        body.addView(extra);

        new AlertDialog.Builder(this)
                .setTitle(endpoint.name + " 高级参数")
                .setView(body)
                .setPositiveButton("保存", (dialog, which) -> {
                    endpoint.requestTimeoutSeconds = parseLong(timeout.getText().toString(), endpoint.requestTimeoutSeconds, 10, 240);
                    endpoint.maxOutputTokens = parseInt(tokens.getText().toString(), endpoint.maxOutputTokens, 512, 16384);
                    endpoint.enableJsonMode = jsonMode.isChecked();
                    endpoint.enableThinking = thinking.isChecked();
                    endpoint.extraBodyJson = extra.getText().toString().trim();
                    invalidateModelTests(ocr);
                    saveSettings(true);
                    render();
                })
                .setNegativeButton("取消", null)
                .show();
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

    private void addSwitch(LinearLayout parent, String label, boolean checked,
                           CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = Ui.row(this);
        row.addView(Ui.text(this, label, 15, Ui.INK, android.graphics.Typeface.NORMAL),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Switch sw = new Switch(this);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);
        row.addView(sw);
        parent.addView(row);
        Ui.addSpace(parent, 8);
    }

    private void addSeek(LinearLayout parent, String label, int min, int max, int value,
                         IntConsumer consumer, String suffix) {
        TextView text = Ui.label(this, label + "：" + value + suffix);
        parent.addView(text);
        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(max - min);
        seekBar.setProgress(value - min);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int actual = min + progress;
                text.setText(label + "：" + actual + suffix);
                consumer.accept(actual);
                updateSummaries();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        parent.addView(seekBar);
        Ui.addSpace(parent, 8);
    }

    private void addPreview(LinearLayout parent) {
        LinearLayout preview = Ui.column(this);
        preview.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));
        preview.setBackground(Ui.stroke(Color.rgb(240, 235, 223), Color.rgb(210, 200, 184), Ui.dp(this, 8), Ui.dp(this, 1)));
        preview.addView(Ui.label(this, "显示预览"));
        TextView bubble = Ui.text(this, "译", 18, Color.WHITE, android.graphics.Typeface.BOLD);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(Ui.round(Ui.TEAL, Ui.dp(this, 22)));
        preview.addView(bubble, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48)));
        Ui.addSpace(preview, 8);
        TextView panel = Ui.text(this, "もう帰るの？\n你已经要回去了吗？", 15, Ui.INK, android.graphics.Typeface.BOLD);
        panel.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        panel.setBackground(Ui.round(Color.WHITE, Ui.dp(this, 8)));
        preview.addView(panel);
        parent.addView(preview);
        Ui.addSpace(parent, 8);
    }

    private void addSaveButton(LinearLayout card) {
        Button save = Ui.button(this, "保存设置", Ui.TEAL, Color.WHITE);
        save.setOnClickListener(v -> saveSettings(true));
        card.addView(save, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 50)));
    }

    private void saveSettings(boolean toast) {
        appSettings.onboardingCompleted = true;
        repository.save(appSettings);
        updateSummaries();
        if (toast) toast("已保存设置");
    }

    private void invalidateModelTests(boolean ocr) {
        if (ocr) {
            appSettings.ocrTestPassed = false;
        } else {
            appSettings.translationTestPassed = false;
        }
        repository.markTestResult(ocr, false);
    }

    private void updateSummaries() {
        if (stateLabel == null) return;
        boolean overlay = Settings.canDrawOverlays(this);
        boolean ocrKey = secretStore.hasSecret(appSettings.ocrEndpoint.secretReference);
        boolean translationKey = secretStore.hasSecret(appSettings.translationEndpoint.secretReference);
        boolean ocrReady = appSettings.ocrEndpoint.isConfigured() && ocrKey;
        boolean translationReady = appSettings.translationEndpoint.isConfigured() && translationKey;
        if (overlay && ocrReady && translationReady) {
            stateLabel.setText("可启动 · 等待屏幕捕获授权");
            stateLabel.setTextColor(Ui.TEAL);
        } else {
            stateLabel.setText("待配置 · 还差 " + missingCount(overlay, ocrReady, translationReady) + " 项");
            stateLabel.setTextColor(Ui.AMBER);
        }
        languageSummary.setText(appSettings.language.sourceLanguage.label + " → " + appSettings.language.targetLanguage.label);
        ocrSummary.setText(appSettings.ocrEndpoint.provider.label + " / "
                + (appSettings.ocrEndpoint.modelId.isEmpty() ? "未填写模型 ID" : appSettings.ocrEndpoint.modelId)
                + " / " + (ocrKey ? "Key 已保存" : "Key 未保存")
                + " / " + (appSettings.ocrTestPassed ? "测试通过" : "待测试"));
        translationSummary.setText(appSettings.translationEndpoint.provider.label + " / "
                + (appSettings.translationEndpoint.modelId.isEmpty() ? "未填写模型 ID" : appSettings.translationEndpoint.modelId)
                + " / " + (translationKey ? "Key 已保存" : "Key 未保存")
                + " / " + (appSettings.translationTestPassed ? "测试通过" : "待测试"));
        displaySummary.setText((appSettings.overlay.panelSide == Models.ScreenSide.RIGHT ? "右侧" : "左侧")
                + "翻译面板 / 宽度 " + Math.round(appSettings.overlay.panelWidthPercent * 100) + "%");
        checklist.setText("启动前校验：\n"
                + "目标语言：" + pass(appSettings.language.targetLanguage != null) + "\n"
                + "OCR 支持图片：" + pass(appSettings.ocrEndpoint.capability != Models.ModelCapability.TEXT) + "\n"
                + "OCR API Key：" + pass(ocrKey) + "\n"
                + "翻译模型：" + pass(appSettings.translationEndpoint.isConfigured() && translationKey) + "\n"
                + "模型测试：" + pass(appSettings.ocrTestPassed && appSettings.translationTestPassed) + "\n"
                + "悬浮窗权限：" + pass(overlay));
    }

    private int missingCount(boolean overlay, boolean ocrReady, boolean translationReady) {
        int count = 0;
        if (!overlay) count++;
        if (!ocrReady) count++;
        if (!translationReady) count++;
        return count;
    }

    private String pass(boolean value) {
        return value ? "通过" : "待处理";
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

    private void startFlow() {
        saveSettings(false);
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            toast("请开启悬浮窗权限后返回。");
            return;
        }
        if (!appSettings.ocrEndpoint.isConfigured() || !secretStore.hasSecret(appSettings.ocrEndpoint.secretReference)) {
            toast("请先完整配置 OCR 模型、模型 ID 和 API Key。");
            return;
        }
        if (!appSettings.translationEndpoint.isConfigured()
                || !secretStore.hasSecret(appSettings.translationEndpoint.secretReference)) {
            toast("请先完整配置翻译模型、模型 ID 和 API Key。");
            return;
        }
        if (!appSettings.ocrTestPassed || !appSettings.translationTestPassed) {
            toast("请先完成 OCR 与翻译模型测试，通过后再启动悬浮翻译。");
            openEndpointConfig(!appSettings.ocrTestPassed);
            return;
        }
        Intent intent = new Intent(this, ProjectionPermissionActivity.class);
        startActivity(intent);
    }

    private void openEndpointConfig(boolean ocr) {
        Intent intent = new Intent(this, EndpointConfigActivity.class);
        intent.putExtra(EndpointConfigActivity.EXTRA_ENDPOINT_KIND,
                ocr ? EndpointConfigActivity.KIND_OCR : EndpointConfigActivity.KIND_TRANSLATION);
        startActivity(intent);
    }

    private void stopOverlayService() {
        Intent intent = new Intent(this, OverlayTranslationService.class);
        intent.setAction(OverlayTranslationService.ACTION_STOP);
        startService(intent);
        toast("已请求停止悬浮翻译服务");
    }

    private void runModelTest(boolean ocr) {
        saveSettings(false);
        Models.AiEndpointConfig endpoint = ocr ? appSettings.ocrEndpoint : appSettings.translationEndpoint;
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
                String result = ocr ? client.testOcr(appSettings, apiKey) : client.testTranslation(appSettings, apiKey);
                repository.markTestResult(ocr, true);
                runOnUiThread(() -> {
                    appSettings = repository.load();
                    toast(result);
                    updateSummaries();
                });
            } catch (Models.TranslateFailure failure) {
                repository.markTestResult(ocr, false);
                runOnUiThread(() -> {
                    appSettings = repository.load();
                    toast(failure.stage.name() + "：" + failure.getMessage());
                    updateSummaries();
                });
            } catch (Exception exception) {
                repository.markTestResult(ocr, false);
                runOnUiThread(() -> {
                    appSettings = repository.load();
                    toast("测试失败：" + exception.getMessage());
                    updateSummaries();
                });
            }
        });
        toast(ocr ? "正在测试 OCR 模型..." : "正在测试翻译模型...");
    }

    private void applyProviderDefaults(Models.AiEndpointConfig endpoint, boolean ocr) {
        switch (endpoint.provider) {
            case DEEPSEEK:
                endpoint.name = ocr ? "DeepSeek OCR" : "DeepSeek 翻译";
                endpoint.baseUrl = "https://api.deepseek.com";
                endpoint.endpointPath = "/chat/completions";
                endpoint.authentication = Models.AuthenticationType.BEARER;
                break;
            case SILICON_FLOW:
                endpoint.name = ocr ? "硅基流动 OCR" : "硅基流动翻译";
                endpoint.baseUrl = "https://api.siliconflow.cn/v1";
                endpoint.endpointPath = "/chat/completions";
                endpoint.authentication = Models.AuthenticationType.BEARER;
                break;
            case ALIBABA_QWEN:
                endpoint.name = ocr ? "百炼 OCR" : "百炼翻译";
                endpoint.baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
                endpoint.endpointPath = "/chat/completions";
                endpoint.authentication = Models.AuthenticationType.BEARER;
                break;
            case OPENAI_COMPATIBLE:
            default:
                endpoint.name = ocr ? "自定义 OCR" : "自定义翻译";
                endpoint.endpointPath = "/chat/completions";
                break;
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 18);
        }
    }

    private String enumLabel(Enum<?> value) {
        if (value instanceof Models.SourceLanguage) return ((Models.SourceLanguage) value).label;
        if (value instanceof Models.TargetLanguage) return ((Models.TargetLanguage) value).label;
        if (value instanceof Models.TranslationStyle) return ((Models.TranslationStyle) value).label;
        if (value instanceof Models.ProviderType) return ((Models.ProviderType) value).label;
        if (value instanceof Models.ScreenSide) return value == Models.ScreenSide.RIGHT ? "右侧" : "左侧";
        if (value instanceof Models.ImageDetail) return ((Models.ImageDetail) value).wireName;
        return value.name();
    }

    private void scrollToCard(View target, String fallbackAreaName) {
        if (target == null || rootScrollView == null) {
            toast("请在下方" + fallbackAreaName + "修改");
            return;
        }
        rootScrollView.post(() -> {
            rootScrollView.smoothScrollTo(0, Math.max(0, target.getTop() - Ui.dp(this, 12)));
            target.animate()
                    .alpha(0.55f)
                    .setDuration(120)
                    .withEndAction(() -> target.animate().alpha(1f).setDuration(180).start())
                    .start();
        });
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private interface EnumConsumer<T> {
        void accept(T value);
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private interface BoolConsumer {
        void accept(boolean value);
    }
}
