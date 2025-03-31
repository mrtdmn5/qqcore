/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;

import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.data.ANCInfo;
import com.qualcomm.qti.gaiaclient.core.requests.core.Request;

/**
 * @deprecated This class has been deprecated in favour of {@link GetANCInfoRequest#GetANCInfoRequest(ANCInfo)}.
 */
@Deprecated
public class GetANCDataRequest extends Request<Void, Void, Void> {

    public GetANCDataRequest() {
        super(null);
        throw new RuntimeException(
                "GetANCDataRequest has been deprecated, please use GetANCInfoRequest(ANCInfo) instead.");
    }

    @Override
    public void run(@Nullable Context context) {
        onComplete(null);
    }

    @Override
    protected void onEnd() {
    }
}
