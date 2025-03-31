/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.data;

import androidx.annotation.NonNull;

import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyser;
import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyserFactory;
import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyserType;

import java.util.Objects;
import java.util.UUID;

public class Transport {

    protected final StreamAnalyserType mAnalyserType;
    protected final UUID mUuid;

    public Transport(StreamAnalyserType analyserType, UUID uuid) {
        this.mAnalyserType = analyserType;
        this.mUuid = uuid;
    }

    public StreamAnalyser getStreamAnalyser() {
        return StreamAnalyserFactory.buildDataAnalyser(mAnalyserType);
    }

    public UUID getUuid() {
        return mUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Transport transport = (Transport) o;
        return mAnalyserType == transport.mAnalyserType && Objects.equals(mUuid, transport.mUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAnalyserType, mUuid);
    }

    @NonNull
    @Override
    public String toString() {
        return "Transport{" +
                "analyserType=" + mAnalyserType +
                ", uuid=" + mUuid +
                '}';
    }
}
