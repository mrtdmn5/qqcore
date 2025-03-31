/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.upgrade;

import com.qualcomm.qti.libraries.upgrade.messages.UpgradeMessageListener;

public class UploadRequest {

    private final byte[] data;

    private final boolean acknowledged;

    private final boolean flushed;

    private final UpgradeMessageListener upgradeMessageListener;

    public UploadRequest(byte[] data, boolean flushed, boolean acknowledged,
                         UpgradeMessageListener listener) {
        this.data = data;
        this.acknowledged = acknowledged;
        this.flushed = flushed;
        this.upgradeMessageListener = listener;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public boolean isFlushed() {
        return flushed;
    }

    public UpgradeMessageListener getUpgradeMessageListener() {
        return upgradeMessageListener;
    }

}
