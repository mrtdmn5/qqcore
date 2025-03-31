/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins;

import com.qualcomm.qti.gaiaclient.core.data.ANCInfo;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.publishers.ANCPublisher;

public interface ANCPlugin {

    // to force the ANCPlugin to implement VoiceUIPublisher
    // this is unused
    ANCPublisher getANCPublisher();

    /**
     * @deprecated This class has been deprecated in favour of {@link #fetchANCInfo(ANCInfo)}.
     */
    @Deprecated
    default void fetchAll() {
        throw new RuntimeException("fetchAll() has been deprecated, please use fetchANCInfo(ANCInfo) instead.");
    }

    void fetchANCInfo(ANCInfo info);

    void setANCInfo(ANCInfo info, Object value);

}
