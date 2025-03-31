/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;

import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.data.DeviceInfo;
import com.qualcomm.qti.gaiaclient.core.requests.core.Request;

/**
 * @deprecated This class has been deprecated in favour of
 * {@link GetDeviceInformationRequest#GetDeviceInformationRequest(DeviceInfo)} to be called with
 * {@link DeviceInfo#APPLICATION_VERSION}.
 */
@Deprecated
public class GetDeviceVersionRequest extends Request<Void, Void, Void> {

    public GetDeviceVersionRequest() {
        super(null);
        throw new RuntimeException(
                "GetDeviceVersionRequest has been deprecated, please use" +
                        " GetDeviceInformationRequest(DeviceInfo.APPLICATION_VERSION) instead.");
    }

    @Override
    public void run(@Nullable Context context) {
        onComplete(null);
    }

    @Override
    protected void onEnd() {
    }
}
