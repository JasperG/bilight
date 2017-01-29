package com.jaspergoes.bilight.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import com.jaspergoes.bilight.R;
import com.jaspergoes.bilight.milight.Controller;

public class ColorPickerView extends View {

    private final float PI = 3.1415926f;
    private final int[] mColors = new int[]{
            0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00,
            0xFFFFFF00, 0xFFFF0000
    };
    private Paint mRadialPaint;
    private Paint mShadowPaint;
    private Paint mCenterPaint;
    private OnColorChangeListener mListener;
    private boolean mTrackingCenter;
    private boolean mHighlightCenter;
    private boolean mTracking;
    private int CENTER;
    private int FULL_RADIUS;
    private int CENTER_RADIUS;
    private int MAX_HEIGHT = 0;
    private int BACKCOLOR = 0xffffffff;
    private ScrollViewPlus scrollParent;
    private Path arrowPath;

    private static float colorAngle;
    private static int color = 0xffff0000;

    public ColorPickerView(Context context) {
        this(context, null, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();

        int satMaxHeight = Math.min(display.getWidth(), display.getHeight());

        TypedArray a = ((Activity) context).obtainStyledAttributes((new TypedValue()).data, new int[]{R.attr.actionBarSize});
        satMaxHeight -= a.getDimensionPixelSize(0, 0);
        a.recycle();

        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            satMaxHeight -= getResources().getDimensionPixelSize(resourceId);

        MAX_HEIGHT = satMaxHeight - (int) (12f * getResources().getDisplayMetrics().density);

        mRadialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRadialPaint.setStyle(Paint.Style.FILL);
        mRadialPaint.setShader(new SweepGradient(0, 0, mColors, null));

        mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadowPaint.setStyle(Paint.Style.FILL);

        mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterPaint.setColor(color);

        arrowPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.translate(CENTER, CENTER);
        canvas.drawCircle(0, 0, CENTER, mShadowPaint);
        canvas.drawCircle(0, 0, FULL_RADIUS, mRadialPaint);
        canvas.drawCircle(0, 0, CENTER_RADIUS + 2, mShadowPaint);
        canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

        int c = mCenterPaint.getColor();
        mCenterPaint.setStyle(Paint.Style.STROKE);
        mCenterPaint.setColor(BACKCOLOR);
        canvas.drawCircle(0, 0, CENTER_RADIUS, mCenterPaint);

        if (!Controller.nowWhite) {

            float l = 1.25f;
            float a = (360 - colorAngle) * (float) Math.PI / 180;
            float radius = CENTER_RADIUS - (mCenterPaint.getStrokeWidth() / 2);

            arrowPath.reset();
            arrowPath.moveTo((float) Math.cos(a) * radius, (float) Math.sin(a) * radius);
            arrowPath.lineTo((float) Math.cos(a + 0.1) * radius * l, (float) Math.sin(a + 0.1) * radius * l);
            arrowPath.lineTo((float) Math.cos(a - 0.1) * radius * l, (float) Math.sin(a - 0.1) * radius * l);
            arrowPath.lineTo((float) Math.cos(a) * radius, (float) Math.sin(a) * radius);
            canvas.drawPath(arrowPath, mRadialPaint);

        }
        
        if (mTrackingCenter) {

            mCenterPaint.setColor(c);

            if (mHighlightCenter) {
                mCenterPaint.setAlpha(0xFF);
            } else {
                mCenterPaint.setAlpha(0x80);
            }

            canvas.drawCircle(0, 0, CENTER_RADIUS + mCenterPaint.getStrokeWidth(), mCenterPaint);

        }

        mCenterPaint.setStyle(Paint.Style.FILL);
        mCenterPaint.setColor(c);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = 0;
        int height = 0;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = widthSize;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = heightSize;
        }

        int size = Math.min(width, MAX_HEIGHT);

        //MUST CALL THIS
        setMeasuredDimension(size, size);

        CENTER = size / 2;

        //Create transparent circle
        int[] colors = {0xff000000, 0x00000000};
        float[] colorPositions = {0.98f, 1f};
        mShadowPaint.setShader(new RadialGradient(0, 0, CENTER, colors, colorPositions, Shader.TileMode.MIRROR));

        size -= 10;

        FULL_RADIUS = size / 2;
        CENTER_RADIUS = (int) (0.28 * size);
        mCenterPaint.setStrokeWidth((int) (0.05 * size));

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - CENTER;
        float y = event.getY() - CENTER;
        boolean inCenter = java.lang.Math.sqrt(x * x + y * y) <= CENTER_RADIUS;
        boolean onPainting = java.lang.Math.sqrt(x * x + y * y) <= CENTER;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (onPainting) {
                    mTracking = true;
                    scrollParent.setScrollingEnabled(false);
                    mTrackingCenter = inCenter;
                    if (inCenter) {
                        mHighlightCenter = true;
                        invalidate();
                        break;
                    }
                }
            case MotionEvent.ACTION_MOVE:
                if (mTracking) {
                    if (mTrackingCenter) {
                        if (mHighlightCenter != inCenter) {
                            mHighlightCenter = inCenter;
                            invalidate();
                        }
                    } else {
                        float angle = (float) java.lang.Math.atan2(y, x);
                        // need to turn angle [-PI ... PI] into unit [0....1]
                        float unit = angle / (2 * PI);
                        if (unit < 0) {
                            unit += 1;
                        }
                        color = interpColor(mColors, unit);
                        mCenterPaint.setColor(color);

                        float hsv[] = new float[3];
                        Color.colorToHSV(color, hsv);
                        colorAngle = hsv[0];
                        mListener.colorChanged((int) ((hsv[0] / 360) * 256));

                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mTracking = false;
                scrollParent.setScrollingEnabled(true);
                if (mTrackingCenter) {
                    if (inCenter) mListener.refresh();
                    mTrackingCenter = false;    // so we draw w/o halo
                    invalidate();
                }
                break;
        }
        return true;
    }

    public void setOnColorChangeListener(OnColorChangeListener listener) {
        mListener = listener;
    }

    public void setScrollingParent(ScrollViewPlus scrollParent) {
        this.scrollParent = scrollParent;
    }

    public void setParentBackground(int color) {
        if (color != 0) {
            BACKCOLOR = (230 << 24) | (color & 0x00ffffff);
            invalidate();
        }
    }

    private int ave(int s, int d, float p) {
        return s + java.lang.Math.round(p * (d - s));
    }

    private int interpColor(int colors[], float unit) {
        if (unit <= 0) {
            return colors[0];
        }
        if (unit >= 1) {
            return colors[colors.length - 1];
        }

        float p = unit * (colors.length - 1);
        int i = (int) p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i + 1];

        return Color.rgb(ave(Color.red(c0), Color.red(c1), p), ave(Color.green(c0), Color.green(c1), p), ave(Color.blue(c0), Color.blue(c1), p));
    }

}