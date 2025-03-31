/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth;

import androidx.annotation.NonNull;

import java.util.Collection;

public interface DataSender {

    long sendData(@NonNull byte[] bytes, boolean isFlushed, SendListener listener);

    boolean isConnected();

    void holdData(Collection<Long> ids);

    void resumeData(Collection<Long> ids);

    void cancelData(Collection<Long> ids);
}
