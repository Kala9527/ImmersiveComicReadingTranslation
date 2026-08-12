package com.immersivecomic.translator;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;

public final class ComicSampleActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SampleComicView(this));
    }

    private static final class SampleComicView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SampleComicView(Activity activity) {
            super(activity);
            setBackgroundColor(Color.rgb(252, 244, 238));
            textPaint.setColor(Color.rgb(34, 33, 39));
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(Color.rgb(255, 229, 236));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setColor(Color.rgb(255, 255, 252));
            canvas.drawRoundRect(new RectF(48, 80, width - 48, height - 96), 34, 34, paint);

            paint.setColor(Color.rgb(42, 39, 54));
            paint.setStrokeWidth(7);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRoundRect(new RectF(48, 80, width - 48, height - 96), 34, 34, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(255, 250, 255));
            drawBubble(canvas, new RectF(width * 0.52f, 150, width - 82, 365), true);
            drawBubble(canvas, new RectF(84, 440, width * 0.52f, 655), false);

            textPaint.setTextSize(42);
            canvas.drawText("もう帰るの？", width * 0.75f, 240, textPaint);
            canvas.drawText("少し待って！", width * 0.75f, 300, textPaint);
            canvas.drawText("ドン！", width * 0.30f, 525, textPaint);
            canvas.drawText("危ない！", width * 0.30f, 585, textPaint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(76, 63, 94));
            canvas.drawOval(new RectF(width * 0.50f, height - 340, width * 0.86f, height - 106), paint);
            paint.setColor(Color.rgb(255, 210, 228));
            canvas.drawOval(new RectF(width * 0.57f, height - 310, width * 0.67f, height - 215), paint);
            canvas.drawOval(new RectF(width * 0.71f, height - 310, width * 0.81f, height - 215), paint);
        }

        private void drawBubble(Canvas canvas, RectF rect, boolean tailRight) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawOval(rect, paint);
            Path tail = new Path();
            if (tailRight) {
                tail.moveTo(rect.right - 35, rect.bottom - 45);
                tail.lineTo(rect.right + 34, rect.bottom + 32);
                tail.lineTo(rect.right - 96, rect.bottom - 18);
            } else {
                tail.moveTo(rect.left + 44, rect.bottom - 36);
                tail.lineTo(rect.left - 30, rect.bottom + 32);
                tail.lineTo(rect.left + 112, rect.bottom - 18);
            }
            tail.close();
            canvas.drawPath(tail, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6);
            paint.setColor(Color.rgb(42, 39, 54));
            canvas.drawOval(rect, paint);
            canvas.drawPath(tail, paint);
        }
    }
}
