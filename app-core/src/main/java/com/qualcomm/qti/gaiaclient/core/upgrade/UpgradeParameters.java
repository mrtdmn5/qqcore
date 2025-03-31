/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.upgrade;

import com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm.Format;
import com.qualcomm.qti.libraries.upgrade.messages.OpCodes;
import com.qualcomm.qti.libraries.upgrade.messages.UpgradeMessage;

class UpgradeParameters {

    private static final int DEFAULT_CHUNK_SIZE = Format.DEFAULT_PAYLOAD_MAX_LENGTH
                                                    - UpgradeMessage.HEADER_LENGTH
                                                    - OpCodes.UpgradeData.DATA_HEADER_LENGTH; // 250

    private int expectedChunkSize;

    private boolean isLogged;

    private boolean isUploadFlushed;

    private boolean isUploadAcknowledged;

    UpgradeParameters() {
        this.expectedChunkSize = DEFAULT_CHUNK_SIZE;
        this.isLogged = false;
        this.isUploadFlushed = false;
        this.isUploadAcknowledged = true;
    }

    void reset(int expectedChunkSize) {
        this.expectedChunkSize = expectedChunkSize;
        this.isLogged = false;
        this.isUploadFlushed = false;
        this.isUploadAcknowledged = true;
    }

    void set(int expectedChunkSize, boolean isLogged, boolean isUploadFlushed, boolean isUploadAcknowledged) {
        this.expectedChunkSize = expectedChunkSize;
        this.isLogged = isLogged;
        this.isUploadFlushed = isUploadFlushed;
        this.isUploadAcknowledged = isUploadAcknowledged;
    }

    int getExpectedChunkSize() {
        return expectedChunkSize;
    }

    boolean isLogged() {
        return isLogged;
    }

    boolean isUploadAcknowledged() {
        return isUploadAcknowledged;
    }

    boolean isFlushed() {
        return isUploadFlushed;
    }
}
