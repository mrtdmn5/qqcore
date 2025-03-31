/*
 * ************************************************************************************************
 * * © 2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyserType;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Transport;
import com.qualcomm.qti.gaiaclient.core.requests.core.RequestListener;

import java.util.List;
import java.util.UUID;

public class QTILConnectionRequest extends ConnectionRequest {

    private static final String TAG = "QTILConnectionRequest";

    public QTILConnectionRequest(String deviceAddress,
                                 @NonNull RequestListener<Void, Void, BluetoothStatus> listener) {
        super(deviceAddress, listener);
    }

    @Override
    public void run(@Nullable Context context) {
        Log.w(TAG, "[run] Connection to use default QTIL UUIDS (SPP or GAIA legacy). We recommend that vendors to" +
                " use their own UUID by extending ConnectionRequest and implementing getTransport().");
        super.run(context);
    }

    @NonNull
    @Override
    protected Transport getTransport(List<UUID> uuids) {
        if (uuids.contains(UUIDS.SPP)) {
            return TRANSPORTS.GAIA_TRANSPORT_DEFAULT;
        }
        else if (uuids.contains(UUIDS.GAIA_LEGACY)) {
            return TRANSPORTS.GAIA_TRANSPORT_LEGACY;
        }
        else {
            // returns the default Transport
            return TRANSPORTS.GAIA_TRANSPORT_DEFAULT;
        }
    }

    private static final class TRANSPORTS {

        private static final Transport GAIA_TRANSPORT_LEGACY =
                new Transport(StreamAnalyserType.GAIA, UUIDS.GAIA_LEGACY);
        private static final Transport GAIA_TRANSPORT_DEFAULT = new Transport(StreamAnalyserType.GAIA, UUIDS.SPP);
    }

    private static final class UUIDS {

        /**
         * The SPP UUID as defined by Bluetooth specifications.
         */
        public static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        /**
         * The specific GAIA UUID that was used on some old devices.
         */
        public static final UUID GAIA_LEGACY = UUID.fromString("00001107-D102-11E1-9B23-00025B00A5A5");
    }
}
