package com.jaspergoes.bilight.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import com.jaspergoes.bilight.R;

public class TextViewPlus extends TextView {
    private final TextPaint paint;

    private boolean mJustify = false;
    private int mWidth;

    private int maxViewWidth;
    private int maxViewHeight;

    public TextViewPlus(Context context) {
        this(context, null);
    }

    public TextViewPlus(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextViewPlus(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        paint = this.getPaint();

        /* Added in v76 - 13 nov 2016 - not sure if this works; Seen no results in 2.3.3 emulator */
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextViewPlus);

        int attributeCount = a.getIndexCount();
        for (int i = 0; i < attributeCount; i++) {
            int curAttr = a.getIndex(i);
            switch (curAttr) {
                case R.styleable.TextViewPlus_justify:
                    mJustify = a.getBoolean(R.styleable.TextViewPlus_justify, false);
                    break;
            }
        }

        a.recycle();
    }

    public void setJustify(boolean j) {
        mJustify = j;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mJustify) {
            super.onDraw(canvas);
            return;
        }

        paint.setColor(this.getTextColors().getDefaultColor());

        int gravity = this.getGravity();
        int maxWidth = mWidth - getPaddingLeft() - getPaddingRight();
        float spaceWidth = paint.measureText(" ");
        float positionY = getBaseline() + getPaddingTop() - getPaddingBottom();
        float positionX;
        int lineHeight = getLineHeight();

        String[] textLines = getText().toString().split("((?<=\n)|(?=\n))");
        String textLine;

        boolean mayBreak = true;

        for (int i = 0; i < textLines.length; i++) {
            if (textLines[i].length() == 0) {
                continue;
            } else if (textLines[i].equals("\n")) {
                if (mayBreak) positionY += lineHeight;
                mayBreak = true;
                continue;
            }
            mayBreak = false;

            textLine = textLines[i].trim();

            if (textLine.length() == 0) {
                continue;
            }

            positionX = getPaddingLeft();

            float wordWidth = paint.measureText(textLine);

			/* Used for measuring */
            maxViewWidth = (int) Math.max(maxViewWidth, wordWidth);

            if (wordWidth <= maxWidth) {
                if (gravity == Gravity.CENTER)
                    positionX += (maxWidth - wordWidth) / 2;

                canvas.drawText(textLine, positionX, positionY, paint);

                positionY += lineHeight;
            } else {
                String[] lineAsWords = textLine.split("\\s+");
                String line = "";

                float realLength = 0;

                boolean firstWord = true;

                String word;

                for (int j = 0; j < lineAsWords.length; j++) {
                    word = lineAsWords[j];
                    wordWidth = paint.measureText(word);
                    if (positionX + wordWidth + spaceWidth <= maxWidth) {
                        realLength += wordWidth;
                        positionX += wordWidth + spaceWidth;
                        line += word + " ";
                        firstWord = false;
                    } else {
                        if (positionX + wordWidth <= maxWidth) {
                            realLength += wordWidth;
                            line += word;
                        } else if (firstWord) {
                            boolean okayNow = false;

                            while (true) {
                                if (word.lastIndexOf('/') != -1) {
                                    word = word.substring(0, word.lastIndexOf('/'));
                                    if ((wordWidth = paint.measureText(word)) <= maxWidth) {
                                        okayNow = true;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }

                            if (!okayNow) while (true) {
                                if (word.lastIndexOf('-') != -1) {
                                    word = word.substring(0, word.lastIndexOf('-'));
                                    if ((wordWidth = paint.measureText(word)) <= maxWidth) {
                                        okayNow = true;
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }

                            if (!okayNow) {
                                for (int z = word.length(); z > 0; z--) {
                                    word = word.substring(0, z);
                                    if ((wordWidth = paint.measureText(word)) <= maxWidth)
                                        break;
                                }
                            }

                            textLines[i] = textLine.substring(word.length());

                            if (gravity == Gravity.CENTER)
                                positionX += (maxWidth - wordWidth) / 2;

                            canvas.drawText(word, positionX, positionY, paint);

                            positionY += lineHeight;
                            i--;
                            break;
                        }

                        textLines[i] = textLine.substring(line.length());

                        String[] finalWords = line.trim().split("\\s+");
                        float stretch = (maxWidth - realLength) / (finalWords.length - 1);

						/* Make sure we have no insane spacing between words */
                        if (stretch >= (spaceWidth * 5)) stretch = spaceWidth;

                        positionX = getPaddingLeft();
                        for (int x = 0; x < finalWords.length; x++) {
                            canvas.drawText(finalWords[x], positionX, positionY, paint);
                            positionX += paint.measureText(finalWords[x]) + stretch;
                        }
                        positionY += lineHeight;
                        i--;
                        break;
                    }
                }
            }
        }

		/* Used for measuring */
        maxViewHeight = (int) positionY - (lineHeight / 2) + getPaddingBottom();
        //maxViewHeight = (int) positionY + getPaddingBottom();
        maxViewWidth += getPaddingLeft() + getPaddingRight();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        /* Call supermethod onMeasure so baseline gets populated (getBaseline()) */
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mJustify) return;

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);

        switch (widthSpecMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                mWidth = MeasureSpec.getSize(widthMeasureSpec);
                break;
            case MeasureSpec.UNSPECIFIED:
                mWidth = Integer.MAX_VALUE;
                break;

        }

        this.draw(new Canvas());

        this.setMeasuredDimension(maxViewWidth, maxViewHeight);
    }

    @Override
    public boolean isFocused() {
        /* Return true, so, the view will have an active marquee when put inside a listview (MapActivity) */
        return true;
    }
}