/*
 * ************************************************************************************************
 * * © 2021-2022 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.audiocuration;

import androidx.annotation.Nullable;

public enum FeatureType {

    STATIC_ANC(0x01),
    LEAKTHROUGH_ANC(0x02),
    ADAPTIVE_ANC(0x03),
    ADAPTIVE_LEAKTHROUGH_ANC(0x04);

    private final int value;

    private static final FeatureType[] VALUES = values();

    FeatureType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Nullable
    public static FeatureType valueOf(int value) {
        for (FeatureType type : VALUES) {
            if (type.value == value) {
                return type;
            }
        }

        return null;
    }

    public static FeatureType[] getValues() {
        int length = VALUES.length;
        FeatureType[] copy = new FeatureType[length];
        System.arraycopy(VALUES, 0, copy, 0, length);
        return copy;
    }
}
