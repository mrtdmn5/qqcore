/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia;

import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyserListener;
import com.qualcomm.qti.gaiaclient.core.gaia.core.sending.GaiaSender;
import com.qualcomm.qti.gaiaclient.core.gaia.core.Vendor;
import com.qualcomm.qti.gaiaclient.core.publications.PublicationManager;

import androidx.annotation.NonNull;

public final class GaiaManagerWrapper implements GaiaManager {

    private final GaiaManagerImpl mManager;

    public GaiaManagerWrapper(@NonNull PublicationManager publicationManager) {
        this.mManager = new GaiaManagerImpl(publicationManager);
    }

    @Override
    public void registerVendor(@NonNull Vendor vendor) {
        mManager.getVendorHandler().addVendor(vendor);
    }

    @Override
    public GaiaSender getSender() {
        return mManager.getGaiaSender();
    }

    @Override
    public StreamAnalyserListener getStreamAnalyserListener() {
        return mManager.getStreamAnalyserListener();
    }

    public void release() {
        mManager.release();
    }
}
