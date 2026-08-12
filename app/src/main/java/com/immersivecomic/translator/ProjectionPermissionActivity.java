package com.immersivecomic.translator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.os.Build;
import android.os.Bundle;

import com.immersivecomic.translator.overlay.OverlayTranslationService;

public final class ProjectionPermissionActivity extends Activity {
    private static final int REQUEST_SCREEN_CAPTURE = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = Build.VERSION.SDK_INT >= 34
                ? manager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay())
                : manager.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREEN_CAPTURE && resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, OverlayTranslationService.class);
            service.setAction(OverlayTranslationService.ACTION_START);
            service.putExtra(OverlayTranslationService.EXTRA_RESULT_CODE, resultCode);
            service.putExtra(OverlayTranslationService.EXTRA_RESULT_DATA, data);
            startForegroundService(service);
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
