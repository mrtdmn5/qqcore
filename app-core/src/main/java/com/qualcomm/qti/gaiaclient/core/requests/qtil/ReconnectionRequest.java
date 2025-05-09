/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;

import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.bluetooth.TransportManager;
import com.qualcomm.qti.gaiaclient.core.requests.core.Request;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getTransportManager;

public class ReconnectionRequest extends Request<Void, Void, Void> {

    public ReconnectionRequest() {
        super(null);
    }

    @Override
    public void run(@Nullable Context context) {
        TransportManager manager = getTransportManager();
        manager.reconnect();
        onComplete(null);
    }

    @Override
    protected void onEnd() {

    }

}
