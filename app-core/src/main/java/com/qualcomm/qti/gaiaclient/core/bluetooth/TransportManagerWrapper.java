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
import com.qualcomm.qti.gaiaclient.core.publications.PublicationManager;

import androidx.annotation.NonNull;

public final class TransportManagerWrapper implements TransportManager {

    private final TransportManagerImpl mManager;

    public TransportManagerWrapper(@NonNull PublicationManager publicationManager) {
        mManager = new TransportManagerImpl(publicationManager);
    }

    public void release() {
        mManager.release();
    }

    @Override
    public void logBytes(boolean log) {
        mManager.logBytes(log);
    }

    @Override
    public Link getLink() {
        return mManager.getLink();
    }

    @Override
    public DataSender getDataSender() {
        return mManager.getDataSender();
    }

    @Override
    public BluetoothStatus connect(@NonNull Context context, @NonNull String address,
                                   @NonNull Transport transport) {
        return mManager.connect(context, address, transport);
    }

    @Override
    public BluetoothStatus reconnect() {
        return mManager.reconnect();
    }

    @Override
    public void disconnect(boolean hard) {
        mManager.disconnect(hard);
    }

    @Override
    public BluetoothStatus fetchUuidServices(@NonNull Context context, @NonNull String address,
                                             @NonNull UuidFetcher.UuidFetcherListener listener) {
        return mManager.fetchUuidServices(context, address, listener);
    }
}
