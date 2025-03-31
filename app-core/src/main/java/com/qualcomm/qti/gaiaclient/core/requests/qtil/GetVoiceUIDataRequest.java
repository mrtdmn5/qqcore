/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;

import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.requests.core.Request;

/**
 * @deprecated This class has been deprecated in favour of
 *         {@link GetSelectedAssistantRequest#GetSelectedAssistantRequest()} and
 *         {@link GetSupportedAssistantsRequest#GetSupportedAssistantsRequest()}.
 */
@Deprecated
public class GetVoiceUIDataRequest extends Request<Void, Void, Void> {

    public GetVoiceUIDataRequest() {
        super(null);
        throw new RuntimeException(
                "GetVoiceUIDataRequest has been deprecated, please use GetSelectedAssistantRequest" +
                        " and GetSupportedAssistantsRequest instead.");
    }

    @Override
    public void run(@Nullable Context context) {
        onComplete(null);
    }

    @Override
    protected void onEnd() {
    }
}
