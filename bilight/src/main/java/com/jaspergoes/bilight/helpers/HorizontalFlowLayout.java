package com.jaspergoes.bilight.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Custom view which extends {@link RelativeLayout} and which places its
 * children horizontally, flowing over to a new line whenever it runs out of
 * horizontal space.
 */
public class HorizontalFlowLayout extends RelativeLayout {
    /**
     * Constructor to use when creating View from code.
     */
    public HorizontalFlowLayout(Context context) {
        super(context);
    }

    /**
     * Constructor that is called when inflating View from XML.
     */
    public HorizontalFlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style.
     */
    public HorizontalFlowLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Call super.onMeasure to get the children measured.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        // the left padding of this layout
        int parentPaddingLeft = getPaddingLeft();
        // the right padding of this layout
        int parentPaddingRight = getPaddingRight();
        // increment the x position as we progress through a line
        int xpos = parentPaddingLeft;
        // increment the y position as we progress through the lines
        int ypos = getPaddingTop();
        // the height of the current line
        int line_height = 0;
        // the amount of children in this group
        int childCount = getChildCount();

        // go through children to work out the height required for this view

        // call to measure size of children not needed I think?!
        // getting child's measured height/width seems to work okay without it as
        // long as super.onMeasure has been called first.
        // measureChildren(widthMeasureSpec, heightMeasureSpec);

        View child;
        MarginLayoutParams childMarginLayoutParams;
        int childWidth, childHeight, childMarginLeft, childMarginRight,
                childMarginTop, childMarginBottom;

        for (int i = 0; i < childCount; i++) {
            if ((child = getChildAt(i)).getVisibility() != GONE) {
                childWidth = child.getMeasuredWidth();
                childHeight = child.getMeasuredHeight();

                if ((childMarginLayoutParams = (MarginLayoutParams) child.getLayoutParams()) != null) {
                    childMarginLeft = childMarginLayoutParams.leftMargin;
                    childMarginRight = childMarginLayoutParams.rightMargin;
                    childMarginTop = childMarginLayoutParams.topMargin;
                    childMarginBottom = childMarginLayoutParams.bottomMargin;
                } else {
                    childMarginLeft = 0;
                    childMarginRight = 0;
                    childMarginTop = 0;
                    childMarginBottom = 0;
                }

                if (xpos + childMarginLeft + childWidth + childMarginRight + parentPaddingRight > width) {
                    // this child will need to go on a new line
                    xpos = parentPaddingLeft;
                    ypos += line_height;

                    line_height = childMarginTop + childHeight + childMarginBottom;
                } else {
                    // enough space for this child on the current line
                    line_height = Math.max(line_height, childMarginTop + childHeight + childMarginBottom);
                }

                xpos += childMarginLeft + childWidth + childMarginRight;
            }
        }

        ypos += line_height + getPaddingBottom();

        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED)
            // set height as measured since there's no height restrictions
            height = ypos;
        else if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.AT_MOST && ypos < height)
            // set height as measured since it's less than the maximum allowed
            height = ypos;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // the left padding of this layout
        int parentPaddingLeft = getPaddingLeft();
        // the right padding of this layout
        int parentPaddingRight = getPaddingRight();
        // increment the x position as we progress through a line
        int xpos = parentPaddingLeft;
        // increment the y position as we progress through the lines
        int ypos = getPaddingTop();
        // the height of the current line
        int line_height = 0;
        // the amount of children in this group
        int childCount = getChildCount();

        View child;
        MarginLayoutParams childMarginLayoutParams;
        int childWidth, childHeight, childMarginLeft, childMarginRight,
                childMarginTop, childMarginBottom;

        for (int i = 0; i < childCount; i++) {
            if ((child = getChildAt(i)).getVisibility() != GONE) {
                childWidth = child.getMeasuredWidth();
                childHeight = child.getMeasuredHeight();

                if ((childMarginLayoutParams = (MarginLayoutParams) child.getLayoutParams()) != null) {
                    childMarginLeft = childMarginLayoutParams.leftMargin;
                    childMarginRight = childMarginLayoutParams.rightMargin;
                    childMarginTop = childMarginLayoutParams.topMargin;
                    childMarginBottom = childMarginLayoutParams.bottomMargin;
                } else {
                    childMarginLeft = 0;
                    childMarginRight = 0;
                    childMarginTop = 0;
                    childMarginBottom = 0;
                }

                if (xpos + childMarginLeft + childWidth + childMarginRight + parentPaddingRight > r - l) {
                    // this child will need to go on a new line
                    xpos = parentPaddingLeft;
                    ypos += line_height;

                    line_height = childMarginTop + childHeight + childMarginBottom;
                } else {
                    // enough space for this child on the current line
                    line_height = Math.max(line_height, childMarginTop + childHeight + childMarginBottom);
                }

                child.layout(xpos + childMarginLeft, ypos + childMarginTop, xpos + childMarginLeft + childWidth, ypos + childMarginTop + childHeight);

                xpos += childMarginLeft + childWidth + childMarginRight;
            }
        }
    }
}