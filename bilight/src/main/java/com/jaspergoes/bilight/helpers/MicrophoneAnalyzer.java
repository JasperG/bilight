package com.jaspergoes.bilight.helpers;

/**
 * Created by Jasper on 26-1-2017.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.jaspergoes.bilight.milight.Controller;

public class MicrophoneAnalyzer {

    private static final int RECORDING_SAMPLE_RATE = 44100;

    private volatile boolean mIsRecording;

    public void startRecording() {

        final int bufferSize = AudioRecord.getMinBufferSize(
                MicrophoneAnalyzer.RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        final AudioRecord mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                MicrophoneAnalyzer.RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {

            mIsRecording = true;

            mAudioRecord.startRecording();

            new Thread(new Runnable() {

                @Override
                public void run() {

                    /* Amount of bytes in the buffer */
                    int bufSize = bufferSize;

                    /* In 16-bit audio, each sample is 2 bytes */
                    int numSamples = bufSize / 2;

                    int readLength;

                    short sampleA, sampleB;
                    int numCrossing;

                    byte buffer[] = new byte[bufSize];
                    int p;

                    while (mIsRecording) {

                        /* Fill audio buffer */
                        readLength = mAudioRecord.read(buffer, 0, bufSize);

                        numCrossing = 0;

                        sampleB = (short) (((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF));

                        for (p = 0; p < (readLength / 2) - 1; p++) {

                            sampleA = sampleB;
                            sampleB = (short) (((buffer[(p + 1) * 2] & 0xFF) << 8) | (buffer[((p + 1) * 2) + 1] & 0xFF));

                            if ((sampleA > 0 && sampleB <= 0) || (sampleA < 0 && sampleB >= 0)) {
                                numCrossing++;
                            }

                        }

                        float frequency = ((float) numCrossing / 2) / ((float) numSamples / RECORDING_SAMPLE_RATE);

                        //Log.e("LOG", Double.toString(Math.floor(Math.log(frequency / 440) / Math.log(2))));
                        //Log.e("LOG", Double.toString(57D + (12D / Math.log(2D)) * Math.log(frequency / 440d)));

                        int decibel = calculateDecibel(buffer);

                        Log.e("MW", Float.toString(frequency) + " " + Integer.toString(decibel));

                        Controller.newBrightness = ((decibel / 40) * 100);
                        Controller.newColor = (int) (frequency % 256);

                    }

                    mAudioRecord.stop();
                    mAudioRecord.release();

                }

            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (mIsRecording) {
                        synchronized (Controller.INSTANCE) {
                            Controller.INSTANCE.notify();
                        }
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {

                        }
                    }
                }
            }).start();

        }

    }

    public void stopRecording() {
        mIsRecording = false;
    }

    private int calculateDecibel(byte[] buf) {
        int sum = 0;
        for (int i = 0; i < buf.length; i++) {
            sum += Math.abs(buf[i]);
        }
        // avg 10-50
        return sum / buf.length;
    }

}