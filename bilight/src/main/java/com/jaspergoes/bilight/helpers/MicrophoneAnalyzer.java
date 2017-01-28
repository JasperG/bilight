package com.jaspergoes.bilight.helpers;

/**
 * Created by Jasper on 26-1-2017.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.jaspergoes.bilight.milight.Controller;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class MicrophoneAnalyzer {

    private static final int RECORDING_SAMPLE_RATE = 44100;

    private AudioRecord mAudioRecord;
    private boolean mIsRecording;
    private int mBufSize;

    private int mSamplingInterval = 40;
    private Timer mTimer;

    public MicrophoneAnalyzer() {
        initAudioRecord();
    }

    /**
     * setter of samplingInterval
     *
     * @param samplingInterval interval volume sampling
     */
    public void setSamplingInterval(int samplingInterval) {
        mSamplingInterval = samplingInterval;
    }

    /**
     * getter isRecording
     *
     * @return true:recording, false:not recording
     */
    public boolean isRecording() {
        return mIsRecording;
    }

    private void initAudioRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(
                RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDING_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
        );

        if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            mBufSize = bufferSize;
        }
    }

    /**
     * start AudioRecord.read
     */
    public void startRecording() {
        mTimer = new Timer();
        mAudioRecord.startRecording();
        mIsRecording = true;
        runRecording();
    }

    /**
     * stop AudioRecord.read
     */
    public void stopRecording() {
        mIsRecording = false;
        mTimer.cancel();
    }

    public static short[] shortMe(byte[] bytes) {
        short[] out = new short[bytes.length / 2]; // will drop last byte if odd number
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        for (int i = 0; i < out.length; i++) {
            out[i] = bb.getShort();
        }
        return out;
    }

    private void runRecording() {
        final byte buf[] = new byte[mBufSize];
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // stop recording
                if (!mIsRecording) {
                    mAudioRecord.stop();
                    return;
                }
                mAudioRecord.read(buf, 0, mBufSize);

                short[] audioData = shortMe(buf);
                int numSamples = audioData.length;
                int numCrossing = 0;
                for (int p = 0; p < numSamples - 1; p++) {
                    if ((audioData[p] > 0 && audioData[p + 1] <= 0) ||
                            (audioData[p] < 0 && audioData[p + 1] >= 0)) {
                        numCrossing++;
                    }
                }

                float numSecondsRecorded = (float) numSamples / (float) RECORDING_SAMPLE_RATE;
                float numCycles = numCrossing / 2;
                float frequency = numCycles / numSecondsRecorded;

                int decibel = calculateDecibel(buf);

                Log.e("MW", Float.toString(frequency));
                Log.e("DECIBEL", Integer.toString(decibel));

                Controller.newBrightness = ((decibel / 40) * 100);
                Controller.newColor = (int) ((frequency / 4) % 256);
                synchronized (Controller.INSTANCE) {
                    Controller.INSTANCE.notify();
                }
            }
        }, 0, mSamplingInterval);
    }

    private int calculateDecibel(byte[] buf) {
        int sum = 0;
        for (int i = 0; i < mBufSize; i++) {
            sum += Math.abs(buf[i]);
        }
        // avg 10-50
        return sum / mBufSize;
    }

    /**
     * release member object
     */
    public void release() {
        stopRecording();
        mAudioRecord.release();
        mAudioRecord = null;
        mTimer = null;
    }
}