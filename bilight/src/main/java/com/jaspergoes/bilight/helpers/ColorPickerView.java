package com.jaspergoes.bilight.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
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

    private static final float PI = 3.1415926f;

    private final int[] mColors = new int[]{0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000};

    private Paint mShadowPaint;
    private Paint mRadialPaint;
    private Paint mCenterPaint;
    private Paint mBorderPaint;

    private Path mArrowPath;

    private OnColorChangeListener mListener;
    private ScrollViewPlus mScrollParent;

    private boolean mTrackingCenter;
    private boolean mHighlightCenter;
    private boolean mTracking;

    private int mMaxHeight = 0;

    private int CENTER;
    private int FULL_RADIUS;
    private int CENTER_RADIUS;
    private int BACKCOLOR = 0xffffffff;

    private static int COLOR = 0xffff0000;
    private static float COLOR_ANGLE;

    public ColorPickerView(Context context) {

        this(context, null, 0);

    }

    public ColorPickerView(Context context, AttributeSet attrs) {

        this(context, attrs, 0);

    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {

        super(context, attrs, defStyle);

        /* Maximum height of this ColorPickerView */
        mMaxHeight = calculateMaximumHeight(context);

        /* Used to draw border around ColorPickerView */
        mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mShadowPaint.setStyle(Paint.Style.FILL);

        /* Used to draw the color spectrum */
        mRadialPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRadialPaint.setStyle(Paint.Style.FILL);
        mRadialPaint.setShader(new SweepGradient(0, 0, mColors, null));

        /* Used to draw the inner selected color */
        mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterPaint.setColor(COLOR);

        /* Used to draw the border of the indicator arrow */
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setColor(0x52000000);

        /* Used to draw the indicator arrow */
        mArrowPath = new Path();

    }

    private int calculateMaximumHeight(Context context) {

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();

        /* Never bigger than width or height of display ..
         * Intentionally using deprecated methods to keep backward compatibility with android versions < 4.0 */
        int height = Math.min(display.getWidth(), display.getHeight());

        /* Minus the height of the actionbar .. */
        TypedArray a = context.obtainStyledAttributes((new TypedValue()).data, new int[]{R.attr.actionBarSize});
        height -= a.getDimensionPixelSize(0, 0);
        a.recycle();

        Resources resources = context.getResources();

        /* Minus the height of the status bar, where applicable .. */
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height -= resources.getDimensionPixelSize(resourceId);
        }

        /* Minus the top and bottom padding of parent layout combined */
        height -= (int) (resources.getDimension(R.dimen.activity_vertical_margin) * 2);

        /* Return the maximum height of our ColorPickerView */
        return height;

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

            float l = 1.205f;
            float a = COLOR_ANGLE * PI / 180;
            float radius = CENTER_RADIUS - (mCenterPaint.getStrokeWidth() / 2);

            mArrowPath.reset();
            mArrowPath.moveTo((float) Math.cos(a) * radius, (float) Math.sin(a) * radius);
            mArrowPath.lineTo((float) Math.cos(a + 0.1) * radius * l, (float) Math.sin(a + 0.1) * radius * l);
            mArrowPath.lineTo((float) Math.cos(a - 0.1) * radius * l, (float) Math.sin(a - 0.1) * radius * l);
            mArrowPath.lineTo((float) Math.cos(a) * radius, (float) Math.sin(a) * radius);
            canvas.drawPath(mArrowPath, mRadialPaint);

            mArrowPath.reset();
            mArrowPath.moveTo((float) Math.cos(a + 0.1) * radius * l, (float) Math.sin(a + 0.1) * radius * l);
            mArrowPath.lineTo((float) Math.cos(a) * radius, (float) Math.sin(a) * radius);
            mArrowPath.lineTo((float) Math.cos(a - 0.1) * radius * l, (float) Math.sin(a - 0.1) * radius * l);
            canvas.drawPath(mArrowPath, mBorderPaint);

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

        /*int r = (int) (Math.sqrt((FULL_RADIUS * FULL_RADIUS) + (FULL_RADIUS * FULL_RADIUS)) - FULL_RADIUS) / 2;
        int y = (int) (Math.sqrt((r * r) + (r * r)) - r) / 2;
        canvas.drawCircle(FULL_RADIUS - r + y, FULL_RADIUS - r + y, (r - y) - 1, mRadialPaint);
        canvas.drawCircle(FULL_RADIUS - r + y, FULL_RADIUS - r + y, (r - y) - 2, mBorderPaint);*/

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int width = 0;

        /* Measure width
         * Width of the ColorPickerView will be restricted in landscape mode layout ( See XML ) */
        if (widthMode == MeasureSpec.EXACTLY) {

            /* View must be exactly this size */
            width = widthSize;

        } else if (widthMode == MeasureSpec.AT_MOST) {

            /* View may not be bigger than this size */
            width = widthSize;

        }

        /* Take the minimum of width, or, previously calculated maximum height */
        int size = Math.min(width, mMaxHeight);

        /* Set calculated size into effect */
        setMeasuredDimension(size, size);

        /* Calculate sizes of elements to draw on canvas */
        CENTER = size / 2;

        /* Set the shader for the shadow around the ColorPickerView now we know it's size */
        mShadowPaint.setShader(new RadialGradient(0, 0, CENTER, new int[]{0xff000000, 0x00000000}, new float[]{0.98f, 1f}, Shader.TileMode.MIRROR));

        /* Subtract a 10 density dependent pixels from the element's size, leaving room for shadow
         * Effectively creating a 5dp padding */
        size -= 10;

        /* Set the radius of the color spectrum to draw */
        FULL_RADIUS = size / 2;

        /* Set the radius of the inner selected color to draw */
        CENTER_RADIUS = (int) (0.28 * size);

        /* Set the width of the border around the centered inner selected color */
        mCenterPaint.setStrokeWidth((int) (0.05 * size));

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        /*int r = (int) (Math.sqrt((FULL_RADIUS * FULL_RADIUS) + (FULL_RADIUS * FULL_RADIUS)) - FULL_RADIUS) / 2;
        int l = (int) (Math.sqrt((r * r) + (r * r)) - r) / 2;
        int t = FULL_RADIUS - r + l;

        float f = (float) Math.sqrt((event.getX() - t) * (event.getX() - t) + (event.getY() - t) + (event.getY() - t));

        Log.e("BL", Float.toString(f));*/

        float x = event.getX() - CENTER;
        float y = event.getY() - CENTER;

        float d = (float) Math.sqrt(x * x + y * y);

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:

                /*if (f <= l) {
                    mTracking = true;

                    mScrollParent.setScrollingEnabled(false);

                    mHighlightCenter = true;

                    invalidate();

                } else */
                if (d <= CENTER) {

                    mTracking = true;

                    mScrollParent.setScrollingEnabled(false);

                    if (mTrackingCenter = d <= CENTER_RADIUS) {

                        mHighlightCenter = true;

                        invalidate();

                    } else {

                        float angle = (float) java.lang.Math.atan2(y, x);

                        COLOR_ANGLE = (angle < 0 ? 2 * PI + angle : angle) * 180 / PI;

                        // need to turn angle [-PI ... PI] into unit [0....1]
                        float unit = angle / (2 * PI);
                        if (unit < 0) unit += 1;
                        mCenterPaint.setColor(COLOR = interpColor(unit));

                        mListener.colorChanged((int) ((360 - COLOR_ANGLE) * 0.7111111f));

                        invalidate();

                    }

                }

                break;

            case MotionEvent.ACTION_MOVE:

                if (mTracking) {

                    if (mTrackingCenter) {

                        if (mHighlightCenter != (mHighlightCenter = d <= CENTER_RADIUS)) {

                            invalidate();

                        }

                    } else {

                        float angle = (float) java.lang.Math.atan2(y, x);

                        COLOR_ANGLE = (angle < 0 ? 2 * PI + angle : angle) * 180 / PI;

                        // need to turn angle [-PI ... PI] into unit [0....1]
                        float unit = angle / (2 * PI);
                        if (unit < 0) unit += 1;
                        mCenterPaint.setColor(COLOR = interpColor(unit));

                        mListener.colorChanged((int) ((360 - COLOR_ANGLE) * 0.7111111f));

                        invalidate();

                    }

                }

                break;

            case MotionEvent.ACTION_UP:

                mTracking = false;

                mScrollParent.setScrollingEnabled(true);

                if (mTrackingCenter) {

                    if (d <= CENTER_RADIUS) mListener.refresh();

                    mTrackingCenter = false;

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

        mScrollParent = scrollParent;

    }

    public void setParentBackground(int color) {

        if (color != 0) {

            BACKCOLOR = (230 << 24) | (color & 0x00ffffff);

            invalidate();

        }

    }

    private int interpColor(float unit) {

        if (unit <= 0) return mColors[0];
        if (unit >= 1) return mColors[mColors.length - 1];

        unit *= (mColors.length - 1);

        int i = (int) unit;
        unit -= i;

        /* now unit is just the fractional part [0...1) and i is the index */
        int c0 = mColors[i];
        int c1 = mColors[i + 1];

        int r = (c0 >> 16) & 0xFF;
        int g = (c0 >> 8) & 0xFF;
        int b = c0 & 0xFF;

        return Color.rgb(r + (int) ((unit * (((c1 >> 16) & 0xFF) - r)) + 0.5f), g + (int) ((unit * (((c1 >> 8) & 0xFF) - g)) + 0.5f), b + (int) ((unit * ((c1 & 0xFF) - b)) + 0.5f));

    }

}