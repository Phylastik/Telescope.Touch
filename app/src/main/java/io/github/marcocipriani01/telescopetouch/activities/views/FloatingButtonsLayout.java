/*
 * Copyright 2021 Marco Cipriani (@marcocipriani01)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.marcocipriani01.telescopetouch.activities.views;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;

/**
 * Contains the provider buttons.
 */

public class FloatingButtonsLayout extends LinearLayout {

    private final int fadeTime;
    private final float distance;
    private final boolean invertDirection;

    public FloatingButtonsLayout(Context context) {
        this(context, null);
    }

    public FloatingButtonsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.FloatingButtonsLayout);
        fadeTime = attributes.getInt(R.styleable.FloatingButtonsLayout_fade_time, 500);
        distance = attributes.getFloat(R.styleable.FloatingButtonsLayout_distance, 200f);
        invertDirection = attributes.getBoolean(R.styleable.FloatingButtonsLayout_invert_direction, false);
        attributes.recycle();
        setClipChildren(false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void setVisibility(int visibility) {
        if (TelescopeTouchApp.DEVICE_IS_CHROME_BOOK) {
            // Chrome OS seems to have troubles with translating views outside the screen.
            // They disappear completely and never come back.
            // Fading instead of translating is an acceptable workaround.
            if (visibility == VISIBLE) {
                fade(View.VISIBLE, 0.0f, 1.0f);
            } else {
                fade(View.GONE, 1.0f, 0.0f);
            }
        } else {
            ObjectAnimator animation = ObjectAnimator.ofFloat(this, "translationX",
                    (visibility == VISIBLE) ? 0f : (invertDirection ? (-distance) : distance));
            animation.setDuration(fadeTime);
            animation.start();
        }
    }

    private void fade(int visibility, float startAlpha, float endAlpha) {
        AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
        anim.setDuration(fadeTime);
        startAnimation(anim);
        super.setVisibility(visibility);
    }

    @Override
    public boolean hasFocus() {
        int numChildren = getChildCount();
        boolean hasFocus = false;
        for (int i = 0; i < numChildren; ++i) {
            hasFocus = hasFocus || getChildAt(i).hasFocus();
        }
        return hasFocus;
    }
}