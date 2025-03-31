/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth;

import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.ConnectionState;

public interface RfcommClientListener {

    /**
     * <p>This method is called by this client when the connection state changes: device is
     * disconnected,
     * connection had been lost, device is connecting, etc.</p>
     * <p>If the connection state has changed due to en error, the method
     * {@link #onConnectionError(BluetoothStatus) onConnectionError} will also be called.</p>
     *
     * @param state
     *         the new state of the connection.
     */
    void onConnectionStateChanged(ConnectionState state);

    /**
     * <p>When an error occurs related to the RFCOMM connection this method is called by this
     * client.</p>
     *
     * @param error
     *         The type of error which occurred. Please see {@link BluetoothStatus} for more
     *         information.
     */
    void onConnectionError(BluetoothStatus error);

}
