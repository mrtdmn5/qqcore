/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.upgrade;

import com.qualcomm.qti.gaiaclient.core.gaia.core.sending.PacketSentListener;

public interface UpgradeHelperListener {

    void sendUpgradeMessage(byte[] data);

    void sendUpgradeMessage(byte[] data, boolean isAcknowledged, boolean isFlushed,
                            PacketSentListener listener);

    void setUpgradeModeOn();

    void setUpgradeModeOff();

    void cancelAll();
}
