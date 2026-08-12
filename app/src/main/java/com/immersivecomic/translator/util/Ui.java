package com.immersivecomic.translator.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int INK = Color.rgb(36, 26, 36);
    public static final int PAPER = Color.rgb(255, 247, 232);
    public static final int CARD = Color.rgb(255, 252, 245);
    public static final int TEAL = Color.rgb(34, 184, 199);
    public static final int PINK = Color.rgb(255, 90, 138);
    public static final int PURPLE = Color.rgb(123, 97, 255);
    public static final int AMBER = Color.rgb(255, 180, 70);
    public static final int DANGER = Color.rgb(217, 74, 61);
    public static final int MUTED = Color.rgb(103, 88, 103);
    public static final int LINE = Color.rgb(48, 37, 49);
    public static final int SOFT_PINK = Color.rgb(255, 231, 239);
    public static final int SOFT_TEAL = Color.rgb(224, 250, 252);

    private Ui() {
    }

    public static int dp(Context context, float value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static TextView text(Context context, String text, float sp, int color, int style) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setLineSpacing(dp(context, 2), 1.0f);
        return view;
    }

    public static TextView label(Context context, String text) {
        return text(context, text, 13, MUTED, Typeface.NORMAL);
    }

    public static TextView title(Context context, String text) {
        return text(context, text, 24, INK, Typeface.BOLD);
    }

    public static TextView sectionTitle(Context context, String text) {
        return text(context, text, 16, INK, Typeface.BOLD);
    }

    public static Button button(Context context, String text, int fillColor, int textColor) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextColor(textColor);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(context, 48));
        button.setBackground(round(fillColor, dp(context, 8)));
        button.setPadding(dp(context, 14), 0, dp(context, 14), 0);
        return button;
    }

    public static Button outlineButton(Context context, String text) {
        Button button = button(context, text, Color.TRANSPARENT, INK);
        GradientDrawable bg = round(Color.TRANSPARENT, dp(context, 8));
        bg.setStroke(dp(context, 1.5f), LINE);
        button.setBackground(bg);
        return button;
    }

    public static Button ghostButton(Context context, String text) {
        Button button = button(context, text, SOFT_PINK, INK);
        button.setMinHeight(dp(context, 44));
        return button;
    }

    public static Button iconButton(Context context, String text) {
        Button button = outlineButton(context, text);
        button.setMinWidth(dp(context, 44));
        button.setMinHeight(dp(context, 44));
        button.setPadding(0, 0, 0, 0);
        return button;
    }

    public static EditText edit(Context context, String hint, String value, boolean secret) {
        EditText editText = new EditText(context);
        editText.setHint(hint);
        editText.setText(value == null ? "" : value);
        editText.setTextSize(15);
        editText.setSingleLine(true);
        editText.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        editText.setMinHeight(dp(context, 50));
        editText.setTextColor(INK);
        editText.setHintTextColor(Color.rgb(145, 152, 149));
        editText.setBackground(inputBg(context));
        if (secret) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        }
        return editText;
    }

    public static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    public static LinearLayout row(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        return layout;
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = column(context);
        card.setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14));
        card.setBackground(stroke(CARD, LINE, dp(context, 8), dp(context, 1)));
        return card;
    }

    public static LinearLayout comicCard(Context context) {
        LinearLayout card = card(context);
        card.setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 16));
        return card;
    }

    public static LinearLayout overlayCard(Context context) {
        LinearLayout card = column(context);
        card.setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12));
        card.setBackground(stroke(CARD, LINE, dp(context, 8), dp(context, 1)));
        return card;
    }

    public static LinearLayout heroPanel(Context context) {
        LinearLayout hero = column(context);
        hero.setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18));
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{SOFT_PINK, SOFT_TEAL, Color.rgb(255, 246, 213)}
        );
        bg.setCornerRadius(dp(context, 8));
        bg.setStroke(dp(context, 2), LINE);
        hero.setBackground(bg);
        return hero;
    }

    public static TextView chip(Context context, String text, int fillColor) {
        TextView chip = text(context, text, 12, INK, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5));
        chip.setBackground(stroke(fillColor, LINE, dp(context, 8), dp(context, 1)));
        return chip;
    }

    public static View divider(Context context) {
        View line = new View(context);
        line.setBackgroundColor(Color.rgb(233, 220, 218));
        line.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(context, 1))
        ));
        return line;
    }

    public static void margin(View view, int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams lp;
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        } else {
            lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        Context context = view.getContext();
        lp.setMargins(dp(context, left), dp(context, top), dp(context, right), dp(context, bottom));
        view.setLayoutParams(lp);
    }

    public static void addSpace(LinearLayout parent, int dp) {
        View space = new View(parent.getContext());
        parent.addView(space, new LinearLayout.LayoutParams(1, dp(parent.getContext(), dp)));
    }

    public static ImageView icon(Context context, int resId, int sizeDp) {
        ImageView icon = new ImageView(context);
        icon.setImageResource(resId);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{PINK, PURPLE}
        );
        bg.setCornerRadius(dp(context, 8));
        bg.setStroke(dp(context, 2), LINE);
        icon.setBackground(bg);
        icon.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(context, sizeDp), dp(context, sizeDp));
        icon.setLayoutParams(lp);
        return icon;
    }

    public static GradientDrawable round(int color, int radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    public static GradientDrawable stroke(int color, int strokeColor, int radiusPx, int strokePx) {
        GradientDrawable drawable = round(color, radiusPx);
        drawable.setStroke(strokePx, strokeColor);
        return drawable;
    }

    private static GradientDrawable inputBg(Context context) {
        return stroke(Color.WHITE, Color.rgb(219, 202, 212), dp(context, 8), dp(context, 1));
    }
}
