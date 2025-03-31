/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.publications.qtil.publishers;

import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.ConnectionState;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Link;
import com.qualcomm.qti.gaiaclient.core.publications.core.Publisher;
import com.qualcomm.qti.gaiaclient.core.publications.core.Subscription;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.CoreSubscription;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.ConnectionSubscriber;

import androidx.annotation.NonNull;

public class ConnectionPublisher extends Publisher<ConnectionSubscriber> {

    @Override
    public Subscription getSubscription() {
        return CoreSubscription.CONNECTION;
    }

    public void publishConnectionState(@NonNull Link link, ConnectionState state) {
        forEachSubscriber(subscriber -> subscriber.onConnectionStateChanged(link, state));
    }

    public void publishConnectionError(Link link, BluetoothStatus error) {
        forEachSubscriber(subscriber -> subscriber.onConnectionError(link, error));
    }


}
