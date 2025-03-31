/*
 * ************************************************************************************************
 * * © 2020-2022 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.qualcomm.qti.gaiaclient.core.GaiaClientService;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.ConnectionState;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Link;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Transport;
import com.qualcomm.qti.gaiaclient.core.bluetooth.uuids.UuidFetcher;
import com.qualcomm.qti.gaiaclient.core.publications.PublicationManager;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.publishers.ConnectionPublisher;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;

final class TransportManagerImpl {

    private static final String TAG = "TransportManager";

    private static final boolean LOG_METHODS = DEBUG.Bluetooth.TRANSPORT_MANAGER;

    private RfcommClient mClient = null;

    private final ConnectionPublisher mConnectionPublisher = new ConnectionPublisher();

    TransportManagerImpl(@NonNull PublicationManager publicationManager) {
        publicationManager.register(mConnectionPublisher);
    }

    void release() {
        if (mClient != null) {
            mClient.disconnect();
            mClient = null;
        }
    }

    void logBytes(boolean log) {
        if (mClient != null) {
            mClient.logBytes(log);
        }
    }

    Link getLink() {
        return mClient != null ? mClient.getLink() : null;
    }

    DataSender getDataSender() {
        return mClient;
    }

    BluetoothStatus connect(@NonNull Context context, @NonNull String address,
                            @NonNull Transport transport) {
        Logger.log(LOG_METHODS, TAG, "connect", new Pair<>("device", address), new Pair<>(
                "transport", transport));
        Link link = new Link(address, transport);

        boolean sameConnection = mClient != null && link.equals(mClient.getLink());
        boolean isConnected = mClient != null && mClient.isConnected();

        if (isConnected && !sameConnection) {
            Log.w(TAG, "[connect] already connected to a different device or through a different transport:" +
                    " call disconnect() first.");
            return BluetoothStatus.INCORRECT_STATE;
        }

        if (mClient == null || !sameConnection) {
            mClient = createRfcommClient(link);
        }

        GaiaClientService.getReconnectionObserver().enable();
        return mClient.connect(context);
    }

    BluetoothStatus reconnect() {
        Logger.log(LOG_METHODS, TAG, "reconnect");
        // reconnect to device
        if (mClient != null) {
            return mClient.reconnect();
        }

        return BluetoothStatus.DEVICE_NOT_FOUND;
    }

    void disconnect(boolean hard) {
        Logger.log(LOG_METHODS, TAG, "disconnect");

        if (mClient != null) {
            if(hard) {
                GaiaClientService.getReconnectionObserver().disable();
            }
            mClient.disconnect();
        }
    }

    BluetoothStatus fetchUuidServices(@NonNull Context context, @NonNull String address,
                                      @NonNull UuidFetcher.UuidFetcherListener listener) {
        Logger.log(LOG_METHODS, TAG, "fetchUuidServices", new Pair<>("device", address));
        UuidFetcher fetcher = new UuidFetcher(listener, context, address);
        return fetcher.fetch(context);
    }

    private RfcommClient createRfcommClient(@NonNull Link link) {
        Logger.log(LOG_METHODS, TAG, "createRfcommClient", new Pair<>("link", link));
        return new RfcommClient(link, link.getTransport().getStreamAnalyser(), buildRfcommClientListener(link));
    }

    @NonNull
    private RfcommClientListener buildRfcommClientListener(@NonNull Link link) {
        Logger.log(LOG_METHODS, TAG, "buildRfcommClientListener", new Pair<>("link", link));

        return new RfcommClientListener() {
            @Override
            public void onConnectionStateChanged(ConnectionState state) {
                Logger.log(LOG_METHODS, TAG, "RfcommClientListener->onConnectionStateChanged",
                           new Pair<>("state=", state));
                TransportManagerImpl.this.onConnectionStateChanged(link, state);
            }

            @Override
            public void onConnectionError(BluetoothStatus error) {
                Logger.log(LOG_METHODS, TAG, "RfcommClientListener->onConnectionError",
                           new Pair<>("error", error));
                mConnectionPublisher.publishConnectionError(link, error);
            }
        };
    }

    private void onConnectionStateChanged(@NonNull Link link, ConnectionState state) {
        Logger.log(LOG_METHODS, TAG, "onConnectionStateChanged", new Pair<>("state", state));
        // publishing connection state
        mConnectionPublisher.publishConnectionState(link, state);
    }
}
