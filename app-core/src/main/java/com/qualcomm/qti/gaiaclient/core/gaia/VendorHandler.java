/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia;

import android.util.Log;

import com.qualcomm.qti.gaiaclient.core.gaia.core.Vendor;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;

import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import static com.qualcomm.qti.gaiaclient.core.gaia.core.GaiaPacket.VENDOR_OFFSET;
import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.getUINT16;
import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.getHexadecimalStringFromInt;

public class VendorHandler {

    private static final String TAG = "VendorHandler";

    private static final boolean LOG_METHODS = DEBUG.Gaia.VENDOR_HANDLER;

    private final ConcurrentHashMap<Integer, Vendor> mVendors =
            new ConcurrentHashMap<>();

    public void addVendor(@NonNull Vendor vendor) {
        mVendors.put(vendor.getVendorId(), vendor);
    }

    public void start(int gaiaVersion) {
        Logger.log(LOG_METHODS, TAG, "start", new Pair<>("version", gaiaVersion));
        for (Vendor vendor : mVendors.values()) {
            vendor.start(gaiaVersion);
        }
    }

    public void stop() {
        Logger.log(LOG_METHODS, TAG, "stop");
        for (Vendor vendor : mVendors.values()) {
            vendor.stop();
        }
    }

    public void handleData(byte[] data) {
        Logger.log(LOG_METHODS, TAG, "handleData", new Pair<>("data", data));
        int vendorId = getUINT16(data, VENDOR_OFFSET);

        Vendor vendor = mVendors.get(vendorId);

        if (vendor == null) {
            Log.w(TAG, String.format("[handleData] vendor(%s) is unknown, use addVendor" +
                                             "(int, Vendor) to add a vendor.",
                                     getHexadecimalStringFromInt(vendorId)));
            return;
        }

        vendor.handleData(data);
    }

    public void release() {
        Logger.log(LOG_METHODS, TAG, "release");
        stop();
        mVendors.clear();
    }
}
