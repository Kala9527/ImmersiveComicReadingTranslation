package com.immersivecomic.translator.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.immersivecomic.translator.MainActivity;
import com.immersivecomic.translator.R;
import com.immersivecomic.translator.ai.OpenAiCompatibleClient;
import com.immersivecomic.translator.capture.ScreenshotCapturer;
import com.immersivecomic.translator.model.Models;
import com.immersivecomic.translator.settings.SecretStore;
import com.immersivecomic.translator.settings.SettingsRepository;
import com.immersivecomic.translator.settings.TranslationHistoryRepository;
import com.immersivecomic.translator.util.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OverlayTranslationService extends Service {
    public static final String ACTION_START = "com.immersivecomic.translator.START";
    public static final String ACTION_STOP = "com.immersivecomic.translator.STOP";
    public static final String ACTION_TRANSLATE = "com.immersivecomic.translator.TRANSLATE";
    public static final String EXTRA_RESULT_CODE = "result_code";
    public static final String EXTRA_RESULT_DATA = "result_data";

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "comic_translation_overlay";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private WindowManager windowManager;
    private TextView bubble;
    private View panel;
    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams panelParams;
    private MediaProjection mediaProjection;
    private Models.AppSettings settings;
    private SettingsRepository settingsRepository;
    private SecretStore secretStore;
    private TranslationHistoryRepository historyRepository;
    private ScreenshotCapturer screenshotCapturer;
    private boolean translating;
    private Models.TranslatedPage lastPage;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        settingsRepository = new SettingsRepository(this);
        secretStore = new SecretStore(this);
        historyRepository = new TranslationHistoryRepository(this);
        settings = settingsRepository.load();
        screenshotCapturer = new ScreenshotCapturer(this);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification("悬浮翻译已就绪");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (intent != null && ACTION_START.equals(intent.getAction())) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent resultData = intent.getParcelableExtra(EXTRA_RESULT_DATA);
            if (resultCode != 0 && resultData != null) {
                try {
                    MediaProjectionManager manager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                    mediaProjection = manager.getMediaProjection(resultCode, resultData);
                } catch (SecurityException exception) {
                    showError(new Models.TranslateFailure(
                            Models.FailureStage.PERMISSION,
                            "屏幕捕获启动失败：" + exception.getMessage(),
                            false,
                            exception
                    ));
                }
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            toast("缺少悬浮窗权限，无法显示悬浮球。");
            stopSelf();
            return START_NOT_STICKY;
        }
        showBubble();
        if (intent != null && ACTION_TRANSLATE.equals(intent.getAction())) {
            translateCurrentScreen();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        removeView(panel);
        removeView(bubble);
        if (mediaProjection != null) {
            if (screenshotCapturer != null) {
                screenshotCapturer.shutdown();
            }
            mediaProjection.stop();
            mediaProjection = null;
        } else if (screenshotCapturer != null) {
            screenshotCapturer.shutdown();
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    private void showBubble() {
        if (bubble != null) return;
        bubble = new TextView(this);
        bubble.setText("译");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(20);
        bubble.setGravity(Gravity.CENTER);
        bubble.setBackground(Ui.stroke(Ui.PINK, Ui.LINE, Ui.dp(this, 24), Ui.dp(this, 2)));
        bubble.setElevation(Ui.dp(this, 8));
        int size = Ui.dp(this, 56);
        bubbleParams = new WindowManager.LayoutParams(
                size,
                size,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | (settings.overlay.bubbleSide == Models.ScreenSide.LEFT
                ? Gravity.LEFT
                : Gravity.RIGHT);
        bubbleParams.x = Ui.dp(this, 18);
        bubbleParams.y = Ui.dp(this, 180);
        attachBubbleGesture();
        windowManager.addView(bubble, bubbleParams);
    }

    private void attachBubbleGesture() {
        final float[] downRaw = new float[2];
        final int[] downPos = new int[2];
        final long[] downTime = new long[1];
        bubble.setOnTouchListener((view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downRaw[0] = event.getRawX();
                    downRaw[1] = event.getRawY();
                    downPos[0] = bubbleParams.x;
                    downPos[1] = bubbleParams.y;
                    downTime[0] = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    bubbleParams.x = downPos[0] - Math.round(event.getRawX() - downRaw[0])
                            * (settings.overlay.bubbleSide == Models.ScreenSide.RIGHT ? 1 : -1);
                    bubbleParams.y = Math.max(Ui.dp(this, 24),
                            downPos[1] + Math.round(event.getRawY() - downRaw[1]));
                    windowManager.updateViewLayout(bubble, bubbleParams);
                    return true;
                case MotionEvent.ACTION_UP:
                    float moved = Math.abs(event.getRawX() - downRaw[0]) + Math.abs(event.getRawY() - downRaw[1]);
                    long duration = System.currentTimeMillis() - downTime[0];
                    if (moved < Ui.dp(this, 10) && duration > 600) {
                        showQuickSettings();
                    } else if (moved < Ui.dp(this, 10)) {
                        translateCurrentScreen();
                    } else if (event.getRawY() > getResources().getDisplayMetrics().heightPixels - Ui.dp(this, 110)) {
                        toast("已停止悬浮翻译");
                        stopSelf();
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    private void translateCurrentScreen() {
        if (translating) {
            toast("正在处理当前页面，请稍等。");
            return;
        }
        if (mediaProjection == null) {
            showError(new Models.TranslateFailure(
                    Models.FailureStage.PERMISSION,
                    "屏幕捕获授权不存在或已过期。请回到应用重新启动悬浮翻译。",
                    false
            ));
            return;
        }
        translating = true;
        setBubbleState("扫", Ui.AMBER);
        removeView(panel);
        panel = null;
        if (settings.overlay.collapseBubbleDuringTranslation) {
            bubble.setVisibility(View.INVISIBLE);
        }
        screenshotCapturer.capture(mediaProjection, settings, new ScreenshotCapturer.Callback() {
            @Override
            public void onCaptured(byte[] jpegBytes) {
                mainHandler.post(() -> bubble.setVisibility(View.VISIBLE));
                executor.execute(() -> requestTranslation(jpegBytes));
            }

            @Override
            public void onFailed(Models.TranslateFailure failure) {
                mainHandler.post(() -> {
                    bubble.setVisibility(View.VISIBLE);
                    showError(failure);
                    finishTranslation();
                });
            }
        });
    }

    private void requestTranslation(byte[] jpegBytes) {
        mainHandler.post(() -> setBubbleState("译", Ui.AMBER));
        try {
            String ocrKey = secretStore.readSecret(settings.ocrEndpoint.secretReference);
            String translationKey = secretStore.readSecret(settings.translationEndpoint.secretReference);
            if (ocrKey.isEmpty() || translationKey.isEmpty()) {
                throw new Models.TranslateFailure(
                        Models.FailureStage.PERMISSION,
                        "API Key 未配置。请回到应用填写 OCR 与翻译模型 Key。",
                        false
                );
            }
            Models.TranslatedPage page = new OpenAiCompatibleClient()
                    .translateScreenshot(jpegBytes, settings, ocrKey, translationKey, lastPage);
            lastPage = page;
            historyRepository.savePage(page, settings);
            mainHandler.post(() -> showResult(page));
        } catch (Models.TranslateFailure failure) {
            mainHandler.post(() -> showError(failure));
        } catch (Exception exception) {
            mainHandler.post(() -> showError(new Models.TranslateFailure(
                    Models.FailureStage.TRANSLATION_NETWORK,
                    exception.getMessage(),
                    true,
                    exception
            )));
        } finally {
            mainHandler.post(this::finishTranslation);
        }
    }

    private void finishTranslation() {
        translating = false;
        setBubbleState("译", Ui.TEAL);
    }

    private void showResult(Models.TranslatedPage page) {
        removeView(panel);
        LinearLayout root = Ui.overlayCard(this);
        root.setBackground(Ui.stroke(applyAlpha(Ui.CARD, settings.overlay.panelOpacity), Ui.LINE, Ui.dp(this, 8), Ui.dp(this, 1)));

        LinearLayout header = Ui.row(this);
        TextView title = Ui.sectionTitle(this, languageLabel(page.detectedSourceLanguage) + " → " + settings.language.targetLanguage.label);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button collapse = Ui.outlineButton(this, "—");
        collapse.setOnClickListener(v -> showCollapsedTag(page.blocks.size()));
        header.addView(collapse, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 44)));
        Button close = Ui.outlineButton(this, "×");
        close.setOnClickListener(v -> {
            removeView(panel);
            panel = null;
        });
        header.addView(close, new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 44)));
        root.addView(header);

        root.addView(Ui.label(this, "当前页 · " + page.blocks.size() + " 个文字块"));
        Ui.addSpace(root, 10);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout list = Ui.column(this);
        List<String> copyLines = new ArrayList<>();
        for (Models.TranslationBlock block : page.blocks) {
            LinearLayout item = Ui.column(this);
            item.setPadding(Ui.dp(this, 10), Ui.dp(this, 9), Ui.dp(this, 10), Ui.dp(this, 9));
            item.setBackground(Ui.stroke(Color.WHITE, Ui.LINE, Ui.dp(this, 8), Ui.dp(this, 1)));
            TextView original = null;
            if (settings.overlay.showOriginalText && block.source != null) {
                original = Ui.label(this, block.source.originalText);
                item.addView(original);
            }
            TextView translation = Ui.text(this, block.translation, 18 * settings.overlay.fontScale, Ui.INK, android.graphics.Typeface.BOLD);
            item.addView(translation);
            if (block.confidence > 0 && block.confidence < 0.72) {
                item.addView(Ui.text(this, "识别结果可能不准确", 12, Ui.AMBER, android.graphics.Typeface.NORMAL));
            }
            TextView originalView = original;
            item.setOnClickListener(v -> {
                if (originalView != null && originalView.getVisibility() == View.VISIBLE) {
                    originalView.setVisibility(View.GONE);
                } else if (originalView != null) {
                    originalView.setVisibility(View.VISIBLE);
                }
            });
            item.setOnLongClickListener(v -> {
                copy("译文", block.translation);
                toast("已复制译文");
                return true;
            });
            list.addView(item);
            Ui.addSpace(list, 8);
            copyLines.add(block.translation);
        }
        scrollView.addView(list);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Ui.dp(this, Math.round(getResources().getDisplayMetrics().heightPixels / getResources().getDisplayMetrics().density * settings.overlay.panelMaxHeightPercent) - 160)
        ));

        LinearLayout actions = Ui.row(this);
        Button copyAll = Ui.outlineButton(this, "复制全部");
        copyAll.setOnClickListener(v -> {
            copy("全部译文", String.join("\n", copyLines));
            toast("已复制全部译文");
        });
        Button retry = Ui.outlineButton(this, "重新翻译");
        retry.setOnClickListener(v -> translateCurrentScreen());
        Button settingsButton = Ui.outlineButton(this, "设置");
        settingsButton.setOnClickListener(v -> openMainActivity());
        actions.addView(copyAll, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        actions.addView(retry, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        actions.addView(settingsButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        root.addView(actions);
        showPanel(root);
    }

    private void showCollapsedTag(int count) {
        removeView(panel);
        TextView tag = new TextView(this);
        tag.setText("译\n" + count);
        tag.setGravity(Gravity.CENTER);
        tag.setTextColor(Color.WHITE);
        tag.setTextSize(15);
        tag.setBackground(Ui.stroke(Ui.PINK, Ui.LINE, Ui.dp(this, 8), Ui.dp(this, 2)));
        tag.setOnClickListener(v -> {
            if (lastPage != null) showResult(lastPage);
        });
        panel = tag;
        panelParams = new WindowManager.LayoutParams(
                Ui.dp(this, 48),
                Ui.dp(this, 76),
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.CENTER_VERTICAL | (settings.overlay.panelSide == Models.ScreenSide.LEFT
                ? Gravity.LEFT
                : Gravity.RIGHT);
        panelParams.x = 0;
        panelParams.y = 0;
        windowManager.addView(panel, panelParams);
    }

    private void showQuickSettings() {
        LinearLayout root = Ui.overlayCard(this);
        root.addView(Ui.sectionTitle(this, "快捷设置"));
        Button language = Ui.outlineButton(this, "目标语言：" + settings.language.targetLanguage.label);
        language.setOnClickListener(v -> cycleTargetLanguage());
        root.addView(language);
        root.addView(Ui.label(this, "翻译风格：" + settings.translation.style.label));
        root.addView(Ui.label(this, "显示原文：" + (settings.overlay.showOriginalText ? "开启" : "关闭")));
        Ui.addSpace(root, 10);
        Button retry = Ui.button(this, "重新翻译", Ui.TEAL, Color.WHITE);
        retry.setOnClickListener(v -> translateCurrentScreen());
        root.addView(retry);
        Ui.addSpace(root, 8);
        Button full = Ui.outlineButton(this, "打开完整设置");
        full.setOnClickListener(v -> openMainActivity());
        root.addView(full);
        Ui.addSpace(root, 8);
        Button stop = Ui.outlineButton(this, "停止服务");
        stop.setOnClickListener(v -> stopSelf());
        root.addView(stop);
        showPanel(root);
    }

    private void cycleTargetLanguage() {
        Models.TargetLanguage[] values = Models.TargetLanguage.values();
        int next = (settings.language.targetLanguage.ordinal() + 1) % values.length;
        settings.language.targetLanguage = values[next];
        settings.translationTestPassed = false;
        settingsRepository.save(settings);
        settingsRepository.markTestResult(false, false);
        toast("目标语言已切换为：" + settings.language.targetLanguage.label);
        showQuickSettings();
    }

    private void showError(Models.TranslateFailure failure) {
        setBubbleState("!", Ui.DANGER);
        LinearLayout root = Ui.overlayCard(this);
        root.addView(Ui.text(this, "翻译失败", 18, Ui.DANGER, android.graphics.Typeface.BOLD));
        root.addView(Ui.label(this, failure.stage.name()));
        Ui.addSpace(root, 8);
        root.addView(Ui.text(this, failure.getMessage(), 15, Ui.INK, android.graphics.Typeface.NORMAL));
        Ui.addSpace(root, 12);
        LinearLayout actions = Ui.row(this);
        if (failure.canRetry) {
            Button retry = Ui.button(this, "重试", Ui.TEAL, Color.WHITE);
            retry.setOnClickListener(v -> translateCurrentScreen());
            actions.addView(retry, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        }
        Button settingsButton = Ui.outlineButton(this, "设置");
        settingsButton.setOnClickListener(v -> openMainActivity());
        actions.addView(settingsButton, new LinearLayout.LayoutParams(0, Ui.dp(this, 46), 1));
        root.addView(actions);
        showPanel(root);
    }

    private void showPanel(View view) {
        removeView(panel);
        panel = view;
        int width = Math.round(getResources().getDisplayMetrics().widthPixels * settings.overlay.panelWidthPercent);
        panelParams = new WindowManager.LayoutParams(
                width,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.CENTER_VERTICAL | (settings.overlay.panelSide == Models.ScreenSide.LEFT
                ? Gravity.LEFT
                : Gravity.RIGHT);
        panelParams.x = Ui.dp(this, 10);
        panelParams.y = 0;
        windowManager.addView(panel, panelParams);
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent openIntent = PendingIntent.getActivity(
                this,
                1,
                open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        Intent stop = new Intent(this, OverlayTranslationService.class).setAction(ACTION_STOP);
        PendingIntent stopIntent = PendingIntent.getService(
                this,
                2,
                stop,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_translate)
                .setContentTitle("漫画悬浮翻译")
                .setContentText(text)
                .setContentIntent(openIntent)
                .setOngoing(true)
                .addAction(R.drawable.ic_translate, "打开设置", openIntent)
                .addAction(R.drawable.ic_translate, "停止", stopIntent);
        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_text));
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    private void setBubbleState(String text, int color) {
        if (bubble == null) return;
        bubble.setText(text);
        bubble.setBackground(Ui.round(color, Ui.dp(this, 24)));
    }

    private void removeView(View view) {
        if (view == null) return;
        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {
        }
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int applyAlpha(int color, float alpha) {
        return Color.argb(Math.round(255 * alpha), Color.red(color), Color.green(color), Color.blue(color));
    }

    private void copy(String label, String value) {
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText(label, value));
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private String languageLabel(String code) {
        if ("ja".equals(code)) return "日文";
        if ("en".equals(code)) return "英文";
        if ("ko".equals(code)) return "韩文";
        if ("zh".equals(code) || "zh-CN".equals(code) || "zh-TW".equals(code)) return "中文";
        if ("mixed".equals(code)) return "多语言";
        return "未知语言";
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
