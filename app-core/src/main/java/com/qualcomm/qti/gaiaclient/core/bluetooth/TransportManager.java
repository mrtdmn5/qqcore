/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth;

import android.content.Context;

import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Link;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Transport;
import com.qualcomm.qti.gaiaclient.core.bluetooth.uuids.UuidFetcher;

import androidx.annotation.NonNull;

public interface TransportManager {

    BluetoothStatus fetchUuidServices(@NonNull Context context, @NonNull String address,
                                      @NonNull UuidFetcher.UuidFetcherListener listener);

    BluetoothStatus connect(@NonNull Context context, @NonNull String address,
                            @NonNull Transport transport);

    BluetoothStatus reconnect();

    void disconnect(boolean hard);

    DataSender getDataSender();

    void logBytes(boolean log);

    Link getLink();
}
