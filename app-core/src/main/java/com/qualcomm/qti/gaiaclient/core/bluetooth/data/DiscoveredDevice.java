/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.data;

import androidx.annotation.NonNull;

/**
 * This represents the data model of a Bluetooth device that could be connectable.
 */
public class DiscoveredDevice {

    @NonNull
    private final String name;

    @NonNull
    private final String bluetoothAddress;

    @NonNull
    private final DeviceType type;

    public DiscoveredDevice(String name, String address, @NonNull DeviceType type) {
        this.name = name != null ? name : "";
        this.bluetoothAddress = address != null ? address : "";
        this.type = type;
    }

    public @NonNull
    String getName() {
        return name;
    }

    public @NonNull
    String getBluetoothAddress() {
        return bluetoothAddress;
    }

    @NonNull
    public DeviceType getType() {
        return type;
    }
}
