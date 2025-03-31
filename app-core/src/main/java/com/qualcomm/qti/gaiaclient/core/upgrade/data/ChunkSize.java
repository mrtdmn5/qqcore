/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.upgrade.data;

import android.util.Log;

import com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm.Format;
import com.qualcomm.qti.libraries.upgrade.messages.UpgradeMessage;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;

public class ChunkSize {

    private static final String TAG = "ChunkSize";

    private final AtomicInteger mAvailable = new AtomicInteger(Format.DEFAULT_PAYLOAD_MAX_LENGTH);

    private final AtomicInteger mDefault = new AtomicInteger(Format.DEFAULT_PAYLOAD_MAX_LENGTH);

    public void setAvailable(int size) {
        mAvailable.set(size);
    }

    public void setDefault(int size) {
        mDefault.set(size);
    }

    public int getAvailable() {
        return mAvailable.get();
    }

    public int getDefault() {
        return mDefault.get();
    }

    public void reset() {
        mAvailable.set(Format.DEFAULT_PAYLOAD_MAX_LENGTH);
        mDefault.set(Format.DEFAULT_PAYLOAD_MAX_LENGTH);
    }

    /**
     * To get the chunk size for the transfer of data depending on the expected and available
     * sizes. If expected > available, this method returns the available size.
     *
     * @param expected
     *         the chunk size that is expected.
     *
     * @return the maximum length that can be used for the data chunks in an UPGRADE_DATA message.
     */
    public int getChunkSize(int expected) {
        int available = mAvailable.get();

        if (available < UpgradeMessage.REQUIRED_MIN_CONTENT_LENGTH) {
            Log.w(TAG, String.format("calculateChunkSize: expected length (%1$s) is too small, " +
                                             "data length set to: %2$s", expected, available));
            return UpgradeMessage.REQUIRED_MIN_CONTENT_LENGTH;
        }

        if (available < expected) {
            Log.w(TAG, String.format("calculateChunkSize: expected length (%1$s) is too big, data" +
                                             " length set to: %2$s", expected, available));
            return available;
        }
        else {
            return expected;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ChunkSize{" +
                "available=" + mAvailable +
                ", default=" + mDefault +
                '}';
    }
}
