/*
 * ************************************************************************************************
 * * © 2021-2023 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.audiocuration;

import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.getUINT8;
import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.setUINT8;

import androidx.annotation.NonNull;

import com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.EarbudPosition;

public class Gain {

    public static final int GAIN_MIN = 0; // 0x00

    public static final int GAIN_MAX = 255; // 0xFF

    private final int mode;

    private final int featureTypeValue;

    private final FeatureType featureType;

    private final int leftGain;

    private final int rightGain;

    public Gain(int leftGain, int rightGain) {
        this.mode = -1;
        this.featureTypeValue = -1;
        this.featureType = null;
        this.leftGain = leftGain;
        this.rightGain = rightGain;
    }

    public Gain(byte[] data) {
        int MODE_OFFSET = 0;
        int FEATURE_TYPE_OFFSET = 1;
        int LEFT_GAIN_OFFSET = 2;
        int RIGHT_GAIN_OFFSET = 3;

        this.mode = getUINT8(data, MODE_OFFSET);
        this.featureTypeValue = getUINT8(data, FEATURE_TYPE_OFFSET);
        this.featureType = FeatureType.valueOf(featureTypeValue);
        this.leftGain = getUINT8(data, LEFT_GAIN_OFFSET);
        this.rightGain = getUINT8(data, RIGHT_GAIN_OFFSET);
    }

    public Gain(int mode, FeatureType featureType, int leftGain, int rightGain) {
        this.mode = mode;
        this.featureTypeValue = featureType.getValue();
        this.featureType = featureType;
        this.leftGain = leftGain;
        this.rightGain = rightGain;
    }

    public int getMode() {
        return mode;
    }

    public int getFeatureTypeValue() {
        return featureTypeValue;
    }

    public FeatureType getFeatureType() {
        return featureType;
    }

    public int getGain(EarbudPosition position) {
        switch (position) {
            case LEFT:
                return leftGain;
            case RIGHT:
                return rightGain;
            default:
                return 0;
        }
    }

    public byte[] getSetterBytes() {
        int LENGTH = 2;
        int LEFT_GAIN_OFFSET = 0;
        int RIGHT_GAIN_OFFSET = 1;
        byte[] parameters = new byte[LENGTH];
        setUINT8(leftGain, parameters, LEFT_GAIN_OFFSET);
        setUINT8(rightGain, parameters, RIGHT_GAIN_OFFSET);
        return parameters;
    }

    @NonNull
    @Override
    public String toString() {
        return "Gain{" +
                "mode=" + mode +
                ", type=" + featureType +
                ", left=" + leftGain +
                ", right=" + rightGain +
                '}';
    }
}
