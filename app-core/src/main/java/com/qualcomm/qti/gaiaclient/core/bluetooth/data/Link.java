/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.data;

import java.util.Objects;

import androidx.annotation.NonNull;

public class Link {

    @NonNull
    private final String mBluetoothAddress;

    @NonNull
    private final Transport mTransport;

    public Link(@NonNull String bluetoothAddress, @NonNull Transport transport) {
        this.mBluetoothAddress = bluetoothAddress;
        this.mTransport = transport;
    }

    @NonNull
    public String getBluetoothAddress() {
        return mBluetoothAddress;
    }

    @NonNull
    public Transport getTransport() {
        return mTransport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Link link = (Link) o;
        return mBluetoothAddress.equalsIgnoreCase(link.mBluetoothAddress) &&
                mTransport == link.mTransport;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mBluetoothAddress, mTransport);
    }

    @Override
    @NonNull
    public String toString() {
        return "Link{" +
                "address='" + mBluetoothAddress + '\'' +
                ", transport=" + mTransport +
                '}';
    }
}
