/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core.sending;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.bluetooth.DataSender;
import com.qualcomm.qti.gaiaclient.core.bluetooth.SendListener;
import com.qualcomm.qti.gaiaclient.core.bluetooth.communication.IdCreator;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.ConnectionState;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Link;
import com.qualcomm.qti.gaiaclient.core.data.Reason;
import com.qualcomm.qti.gaiaclient.core.data.SizeInfo;
import com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm.RfcommFormatter;
import com.qualcomm.qti.gaiaclient.core.gaia.core.version.ProtocolVersion;
import com.qualcomm.qti.gaiaclient.core.publications.PublicationManager;
import com.qualcomm.qti.gaiaclient.core.publications.core.ExecutionType;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.ConnectionSubscriber;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.ProtocolSubscriber;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class GaiaSenderWrapper implements GaiaSender {

    /**
     * This is set to V1 as it is the minimum version supported
     */
    private static final long DEFAULT_PROTOCOL_VERSION = ProtocolVersion.V1;

    private static final String TAG = "GaiaSenderWrapper";

    @Nullable
    private DataSender mSender;

    private final RfcommFormatter mFormatter = new RfcommFormatter();

    private final ConcurrentHashMap<SizeInfo, Integer> mSizes = new ConcurrentHashMap<>();

    private long mProtocolVersion = DEFAULT_PROTOCOL_VERSION;

    @SuppressWarnings("FieldCanBeLocal")
    private final ProtocolSubscriber mProtocolSubscriber = new ProtocolSubscriber() {
        @NonNull
        @Override
        public ExecutionType getExecutionType() {
            return ExecutionType.BACKGROUND;
        }

        @Override
        public void onProtocolVersion(long version) {
            if (version > ProtocolVersion.V4) {
                Log.w(TAG, "[ProtocolSubscriber->onProtocolVersion] unsupported version: "
                        + version);
                // keep the previous version for formatting packets
                return;
            }

            mProtocolVersion = version;
        }

        @Override
        public void onSizeInfo(SizeInfo info, int size) {
            mSizes.put(info, size);
        }

        @Override
        public void onError(Object info, Reason reason) {
            if (info == null) {
                mProtocolVersion = DEFAULT_PROTOCOL_VERSION;
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ConnectionSubscriber mConnectionSubscriber = new ConnectionSubscriber() {
        @NonNull
        @Override
        public ExecutionType getExecutionType() {
            return ExecutionType.BACKGROUND;
        }

        @Override
        public void onConnectionStateChanged(Link link, ConnectionState state) {
            if (state != ConnectionState.CONNECTED) {
                // reset protocol version
                mProtocolVersion = DEFAULT_PROTOCOL_VERSION;
            }
        }

        @Override
        public void onConnectionError(Link link, BluetoothStatus reason) {
            // nothing to do
        }
    };

    public GaiaSenderWrapper(@NonNull PublicationManager publicationManager) {
        publicationManager.subscribe(mProtocolSubscriber);
        publicationManager.subscribe(mConnectionSubscriber);
        // no unsubscription required: GaiaSenderWrapper is destroyed when PublicationManager is
    }

    public DataSender setDataSender(@Nullable DataSender sender) {
        DataSender previous = this.mSender;
        this.mSender = sender;
        return previous;
    }

    @Override
    public long sendData(@NonNull byte[] content, boolean isFlushed, SendListener listener) {
        return mSender == null || !mSender.isConnected() ? IdCreator.NULL_ID :
               mSender.sendData(formatPacket(mProtocolVersion, content), isFlushed, listener);
    }

    @Override
    public boolean isConnected() {
        return mSender != null && mSender.isConnected();
    }

    @Override
    public void holdData(Collection<Long> ids) {
        if (mSender != null) {
            mSender.holdData(ids);
        }
    }

    @Override
    public void resumeData(Collection<Long> ids) {
        if (mSender != null) {
            mSender.resumeData(ids);
        }
    }

    @Override
    public void cancelData(Collection<Long> ids) {
        if (mSender != null) {
            mSender.cancelData(ids);
        }
    }

    @Override
    public int getSize(SizeInfo info) {
        Integer value = mSizes.get(info);
        return value != null ? value : 0;
    }

    private byte[] formatPacket(long version, byte[] content) {
        return mFormatter.format(version, false, content);
    }
}
