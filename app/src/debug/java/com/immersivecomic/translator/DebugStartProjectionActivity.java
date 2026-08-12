package com.immersivecomic.translator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class DebugStartProjectionActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, ProjectionPermissionActivity.class));
        finish();
    }
}
