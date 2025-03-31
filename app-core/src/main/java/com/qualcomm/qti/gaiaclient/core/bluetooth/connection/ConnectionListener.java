/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.connection;

import android.bluetooth.BluetoothSocket;

import androidx.annotation.NonNull;

public interface ConnectionListener {

    void onConnectionSuccess(@NonNull BluetoothSocket socket);

    /**
     * Indicates that the connection attempt failed.
     */
    void onConnectionFailed();

}
