/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.data.LogInfo;
import com.qualcomm.qti.gaiaclient.core.data.Reason;
import com.qualcomm.qti.gaiaclient.core.requests.core.Request;
import com.qualcomm.qti.gaiaclient.core.requests.core.RequestListener;

/**
 * @deprecated This class has been deprecated in favour of
 *         {@link GetLogInfoRequest#GetLogInfoRequest(RequestListener, LogInfo)} to be called with
 *         {@link LogInfo#PARTITION_SIZE} and {@link LogInfo#LOG_FILE}.
 */
@Deprecated
public class GetLogSizesRequest extends Request<Void, Void, Reason> {

    public GetLogSizesRequest(@NonNull RequestListener<Void, Void, Reason> listener) {
        super(null);
        throw new RuntimeException(
                "GetLogSizesRequest has been deprecated, please use GetLogInfoRequest(LogInfo.PARTITION_SIZE)" +
                        " and GetLogInfoRequest(LogInfo.LOG_FILE) instead.");
    }

    @Override
    public void run(@Nullable Context context) {
        onComplete(null);
    }

    @Override
    protected void onEnd() {
    }
}
