/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.analyser;

import com.qualcomm.qti.gaiaclient.core.tasks.TaskManager;

import androidx.annotation.NonNull;

public abstract class StreamAnalyser {

    private final StreamAnalyserListener mListener;

    public StreamAnalyser(StreamAnalyserListener listener) {
        mListener = listener;
    }

    public abstract void reset();

    public abstract void analyse(@NonNull TaskManager taskManager, byte[] data);

    public StreamAnalyserListener getListener() {
        return mListener;
    }
}
