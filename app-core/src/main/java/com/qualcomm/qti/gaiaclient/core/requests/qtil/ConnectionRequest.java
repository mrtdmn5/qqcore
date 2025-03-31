/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.qualcomm.qti.gaiaclient.core.GaiaClientService;
import com.qualcomm.qti.gaiaclient.core.bluetooth.TransportManager;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.ConnectionState;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Link;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Transport;
import com.qualcomm.qti.gaiaclient.core.bluetooth.uuids.UuidFetcher;
import com.qualcomm.qti.gaiaclient.core.publications.PublicationManager;
import com.qualcomm.qti.gaiaclient.core.publications.core.ExecutionType;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.ConnectionSubscriber;
import com.qualcomm.qti.gaiaclient.core.requests.core.Request;
import com.qualcomm.qti.gaiaclient.core.requests.core.RequestListener;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getTransportManager;

import java.util.List;
import java.util.UUID;

public abstract class ConnectionRequest extends Request<Void, Void, BluetoothStatus> {

    private static final String TAG = "ConnectionRequest";

    private static final boolean LOG_METHODS = DEBUG.Request.CONNECTION_REQUEST;

    private static final int ATTEMPTS_MAX = 2;

    private final String mBluetoothAddress;

    private Transport mTransport;

    private int attempts = 0;

    private Context context;

    private final ConnectionSubscriber mSubscriber = new ConnectionSubscriber() {
        @NonNull
        @Override
        public ExecutionType getExecutionType() {
            return ExecutionType.BACKGROUND;
        }

        @Override
        public void onConnectionStateChanged(Link link, ConnectionState state) {
            ConnectionRequest.this.onConnectionStateChanged(link, state);
        }

        @Override
        public void onConnectionError(Link link, BluetoothStatus reason) {
            ConnectionRequest.this.onConnectionError(link, reason);
        }
    };

    public ConnectionRequest(String deviceAddress, @NonNull RequestListener<Void, Void, BluetoothStatus> listener) {
        super(listener);
        this.mBluetoothAddress = deviceAddress;
    }

    String getAddress() {
        return mBluetoothAddress;
    }

    @Override
    public void run(@Nullable Context context) {
        Logger.log(LOG_METHODS, TAG, "run", new Pair<>("address", getAddress()));

        if (context == null) {
            // without the context the Android device Bluetooth is not accessible.
            onError(BluetoothStatus.NO_BLUETOOTH);
            return;
        }

        attempts++;
        this.context = context;

        // determine transport
        TransportManager manager = getTransportManager();
        BluetoothStatus status = manager.fetchUuidServices(context, getAddress(),
                                                           buildFetcherListener(context));

        // check returned status
        if (status != BluetoothStatus.IN_PROGRESS) {
            onError(status);
        }
        else {
            onProgress(null);
        }
    }

    @Override
    protected void onEnd() {
        Logger.log(LOG_METHODS, TAG, "onEnd");
        PublicationManager manager = GaiaClientService.getPublicationManager();
        manager.unsubscribe(mSubscriber);
    }

    private void connect(@NonNull Context context, @NonNull String address,
                         @NonNull Transport transport) {
        Logger.log(LOG_METHODS, TAG, "connect",
                   new Pair<>("address", address), new Pair<>("transport", transport));

        // subscribe for connection updates
        PublicationManager publicationManager = GaiaClientService.getPublicationManager();
        publicationManager.subscribe(mSubscriber);

        // start connection
        TransportManager transportManager = getTransportManager();
        BluetoothStatus status = transportManager.connect(context, address, transport);

        // check returned status
        if (status == BluetoothStatus.ALREADY_CONNECTED) {
            onComplete(null);
        }
        else if (status != BluetoothStatus.IN_PROGRESS) {
            onError(status);
        }
        else {
            onProgress(null);
        }
    }

    private void onConnectionStateChanged(Link link, ConnectionState state) {
        Logger.log(LOG_METHODS, TAG, "onConnectionStateChanged",
                   new Pair<>("link", link), new Pair<>("state", state));

        if (link == null || !link.getBluetoothAddress().equals(mBluetoothAddress)
                || link.getTransport() != mTransport) {
            // not the expected device
            return;
        }

        switch (state) {
            case CONNECTING:
                onProgress(null);
                break;
            case CONNECTED:
                // get the transport manager
                onComplete(null);
                break;
            case DISCONNECTING:
            case DISCONNECTED:
                // an error occurred, reported using onConnectionError
                break;
        }
    }

    private void onConnectionError(Link link, BluetoothStatus status) {
        Logger.log(LOG_METHODS, TAG, "onConnectionError",
                   new Pair<>("link", link), new Pair<>("status", status));

        if (link == null || !link.getBluetoothAddress().equals(mBluetoothAddress)
                || link.getTransport() != mTransport) {
            // not the expected device
            return;
        }

        onError(status);
    }

    private void runConnection(@NonNull Context context, Transport transport) {
        Logger.log(LOG_METHODS, TAG, "runConnection", new Pair<>("transport", transport));
        mTransport = transport;

        // check parameters
        if (mBluetoothAddress == null) {
            Log.w(TAG, "[run] device address is null");
            onError(BluetoothStatus.DEVICE_NOT_FOUND);
            return;
        }

        if (mTransport == null) {
            Log.w(TAG, "[run] transport is null");
            onError(BluetoothStatus.NO_TRANSPORT_FOUND);
            return;
        }

        // run the request
        connect(context, mBluetoothAddress, mTransport);
    }

    private UuidFetcher.UuidFetcherListener buildFetcherListener(@NonNull Context context) {
        return new UuidFetcher.UuidFetcherListener() {
            @Override
            public void onUuidFetched(List<UUID> uuids) {
                Logger.log(LOG_METHODS, TAG, "UuidFetcherListener->onUuidFetched");
                Transport transport = getTransport(uuids);
                runConnection(context, transport);
            }

            @Override
            public void onFailed(BluetoothStatus reason) {
                Logger.log(LOG_METHODS, TAG, "UuidFetcherListener->onFailed",
                           new Pair<>("reason", reason));
                onError(reason);
            }
        };
    }

    @NonNull
    protected abstract Transport getTransport(List<UUID> uuids);


    @Override
    protected void onError(BluetoothStatus reason) {
        if (ATTEMPTS_MAX <= attempts || context == null) {
            super.onError(reason);
        }
        else {
            // we try again
            run(context);
        }
    }
}
