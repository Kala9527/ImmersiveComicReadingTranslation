package com.immersivecomic.translator.capture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.immersivecomic.translator.model.Models;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public final class ScreenshotCapturer {
    public interface Callback {
        void onCaptured(byte[] jpegBytes);

        void onFailed(Models.TranslateFailure failure);
    }

    private final Context context;
    private HandlerThread thread;
    private Handler handler;
    private ImageReader reader;
    private VirtualDisplay virtualDisplay;
    private MediaProjection activeProjection;
    private MediaProjection.Callback projectionCallback;
    private boolean projectionCallbackRegistered;
    private int activeWidth;
    private int activeHeight;
    private int activeDensity;

    public ScreenshotCapturer(Context context) {
        this.context = context.getApplicationContext();
    }

    public void capture(MediaProjection projection, Models.AppSettings settings, Callback callback) {
        ensureThread();
        handler.post(() -> captureOnThread(projection, settings, callback));
    }

    public void shutdown() {
        if (handler != null) {
            handler.post(this::releaseSession);
        } else {
            releaseSession();
        }
        if (thread != null) {
            thread.quitSafely();
            thread = null;
            handler = null;
        }
    }

    private void captureOnThread(MediaProjection projection, Models.AppSettings settings, Callback callback) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        try {
            ensureSession(projection, width, height, density);
            handler.postDelayed(() -> {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    if (image == null) {
                        callback.onFailed(new Models.TranslateFailure(
                                Models.FailureStage.SCREEN_CAPTURE,
                                "未能从屏幕捕获会话取得图像。请重新授权屏幕捕获。",
                                true
                        ));
                        return;
                    }
                    byte[] jpeg = imageToJpeg(image, settings);
                    if (jpeg.length == 0) {
                        callback.onFailed(new Models.TranslateFailure(
                                Models.FailureStage.IMAGE_PROCESSING,
                                "截图为空，无法识别。",
                                true
                        ));
                        return;
                    }
                    callback.onCaptured(jpeg);
                } catch (Exception exception) {
                    callback.onFailed(new Models.TranslateFailure(
                            Models.FailureStage.IMAGE_PROCESSING,
                            "截图处理失败：" + exception.getMessage(),
                            true,
                            exception
                    ));
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }, 650);
        } catch (SecurityException exception) {
            releaseSession();
            callback.onFailed(new Models.TranslateFailure(
                    Models.FailureStage.PERMISSION,
                    "屏幕捕获授权已失效，请重新启动悬浮翻译。",
                    false,
                    exception
            ));
        } catch (Exception exception) {
            releaseSession();
            callback.onFailed(new Models.TranslateFailure(
                    Models.FailureStage.SCREEN_CAPTURE,
                    "无法创建屏幕捕获会话：" + exception.getMessage(),
                    true,
                    exception
            ));
        }
    }

    private void ensureThread() {
        if (thread != null && thread.isAlive() && handler != null) return;
        thread = new HandlerThread("screen-capture");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    private void ensureSession(MediaProjection projection, int width, int height, int density) {
        if (virtualDisplay != null
                && reader != null
                && activeProjection == projection
                && activeWidth == width
                && activeHeight == height
                && activeDensity == density) {
            return;
        }
        releaseSession();
        activeProjection = projection;
        activeWidth = width;
        activeHeight = height;
        activeDensity = density;
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
        projectionCallback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                releaseSession();
            }
        };
        projection.registerCallback(projectionCallback, handler);
        projectionCallbackRegistered = true;
        virtualDisplay = projection.createVirtualDisplay(
                "comic-translation-capture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(),
                null,
                handler
        );
    }

    private byte[] imageToJpeg(Image image, Models.AppSettings settings) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * image.getWidth();
        Bitmap padded = Bitmap.createBitmap(
                image.getWidth() + rowPadding / pixelStride,
                image.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        padded.copyPixelsFromBuffer(buffer);
        Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, image.getWidth(), image.getHeight());
        padded.recycle();

        Bitmap output = scaleDown(cropped, settings.imageSettings.maxLongSide);
        if (output != cropped) {
            cropped.recycle();
        }
        if (settings.imageSettings.detectBlackScreen && isMostlyBlack(output)) {
            output.recycle();
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        output.compress(Bitmap.CompressFormat.JPEG, settings.imageSettings.jpegQuality, out);
        output.recycle();
        return out.toByteArray();
    }

    private Bitmap scaleDown(Bitmap bitmap, int maxLongSide) {
        int longSide = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longSide <= maxLongSide) return bitmap;
        float scale = maxLongSide / (float) longSide;
        int width = Math.max(1, Math.round(bitmap.getWidth() * scale));
        int height = Math.max(1, Math.round(bitmap.getHeight() * scale));
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private boolean isMostlyBlack(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int stepX = Math.max(1, width / 24);
        int stepY = Math.max(1, height / 24);
        int samples = 0;
        int dark = 0;
        for (int y = 0; y < height; y += stepY) {
            for (int x = 0; x < width; x += stepX) {
                int color = bitmap.getPixel(x, y);
                int luminance = (int) (0.299f * android.graphics.Color.red(color)
                        + 0.587f * android.graphics.Color.green(color)
                        + 0.114f * android.graphics.Color.blue(color));
                if (luminance < 12) {
                    dark++;
                }
                samples++;
            }
        }
        return samples > 0 && dark / (float) samples > 0.96f;
    }

    private void releaseSession() {
        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
            }
        } catch (Exception ignored) {
        }
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (Exception ignored) {
        }
        try {
            if (activeProjection != null && projectionCallback != null && projectionCallbackRegistered) {
                activeProjection.unregisterCallback(projectionCallback);
            }
        } catch (Exception ignored) {
        }
        virtualDisplay = null;
        reader = null;
        projectionCallback = null;
        projectionCallbackRegistered = false;
        activeProjection = null;
    }
}
