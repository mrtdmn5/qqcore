/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.analyser;

import android.util.Log;

import com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm.GaiaStreamAnalyser;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getGaiaManager;

public final class StreamAnalyserFactory {

    private static final String TAG = "StreamAnalyserFactory";

    public static StreamAnalyser buildDataAnalyser(StreamAnalyserType type) {
        if (type == StreamAnalyserType.GAIA) {
            return new GaiaStreamAnalyser(getGaiaManager().getStreamAnalyserListener());
        }
        else {
            Log.w(TAG, "[buildDataAnalyser] unexpected type: type=" + type);
            return null;
        }
    }

}
