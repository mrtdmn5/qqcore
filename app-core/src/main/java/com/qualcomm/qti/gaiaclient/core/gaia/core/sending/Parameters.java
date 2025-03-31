/*
 * ************************************************************************************************
 * * © 2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core.sending;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.gaia.core.GaiaPacket;
import com.qualcomm.qti.gaiaclient.core.gaia.core.RunningStatus;
import com.qualcomm.qti.gaiaclient.core.gaia.core.v3.V3Plugin;

import java.lang.ref.WeakReference;

/**
 * A class to hold all the parameters that can be set to send a {@link GaiaPacket} through a
 * {@link com.qualcomm.qti.gaiaclient.core.gaia.core.Plugin}.
 */
public class Parameters {

    /**
     * If a packet should be held when a plugin status is {@link RunningStatus#ON_HOLD}. If the packet is not
     * holdable it is sent immediately if the plugin status is not {@link RunningStatus#STOPPED}.
     */
    private boolean holdable;
    /**
     * If the plugin should expect an acknowledging packet for the packet it is sending / has sent.
     */
    private boolean acknowledged;
    /**
     * If a GaiaSender should flush the packet.
     */
    private boolean flushed;
    /**
     * The listener to notify when the packet is sent.
     */
    @NonNull
    private WeakReference<PacketSentListener> listenerReference;
    /**
     * The timeout to wait for an acknowledgement.
     */
    private long timeout;

    public Parameters() {
        this.holdable = true;
        this.acknowledged = true;
        this.flushed = false;
        this.listenerReference = new WeakReference<>(null);
        this.timeout = V3Plugin.DEFAULT_RESPONSE_TIME_OUT_MS;
    }

    /**
     * To set if a packet should be held when a plugin status is {@link RunningStatus#ON_HOLD}.
     * If the packet is not holdable, the packet is sent immediately.
     */
    public void setHoldable(boolean holdable) {
        this.holdable = holdable;
    }

    /**
     * @param acknowledged
     *         True if the packet is expected to be acknowledged, false otherwise.
     */
    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    /**
     * @param flushed
     *         True to force the system to flush the data immediately to the device. This is a
     *         blocking operation for the sending of data that can lead to a slower throughput of
     *         data. If set to false, the system flushes the packets itself at the optimum time -
     *         this can lead packets to be sent over a same stream. When using GAIA v1/v2, it is
     *         recommended to flush the packets due to some delay on the device when packets are
     *         on a same stream.
     */
    public void setFlushed(boolean flushed) {
        this.flushed = flushed;
    }

    /**
     * @param listener
     *         A listener to be notified when the packet is sent. If the packet is supposed to be
     *         "flushed" the listener is only called once the packet has been flushed.
     */
    public void setListener(@Nullable PacketSentListener listener) {
        this.listenerReference = new WeakReference<>(listener);
    }

    /**
     * @param timeout
     *         A value in milliseconds. This is ignored by the plugin if <code>acknowledged</code> is set to False.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * To know if the packet should be held or sent immediately.
     */
    public boolean isHoldable() {
        return holdable;
    }

    /**
     * To know if the plugin should expect an acknowledgement.
     */
    public boolean isAcknowledged() {
        return acknowledged;
    }

    /**
     * To know if the plugin should force the system to flush the data.
     */
    public boolean isFlushed() {
        return flushed;
    }

    /**
     * How long the plugin should wait for an acknowledgement.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * The listener to notify when the packet has been sent. This can be <code>null</code>.
     */
    @Nullable
    public PacketSentListener getListener() {
        return listenerReference.get();
    }
}
