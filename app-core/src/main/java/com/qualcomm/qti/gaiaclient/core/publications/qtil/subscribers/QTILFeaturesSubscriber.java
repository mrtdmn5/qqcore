/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers;

import com.qualcomm.qti.gaiaclient.core.data.Reason;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.QTILFeature;
import com.qualcomm.qti.gaiaclient.core.publications.core.Subscriber;
import com.qualcomm.qti.gaiaclient.core.publications.core.Subscription;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.CoreSubscription;

import androidx.annotation.NonNull;

public interface QTILFeaturesSubscriber extends Subscriber {

    @NonNull
    default Subscription getSubscription() {
        return CoreSubscription.FEATURES;
    }

    /**
     * @deprecated This method has been deprecated in favour of {@link #onFeatureSupported(QTILFeature, int)}.
     */
    @Deprecated
    default void onFeatureSupported(QTILFeature feature) {}

    void onFeatureSupported(QTILFeature feature, int version);

    void onFeatureNotSupported(QTILFeature feature, Reason reason);

    void onError(Reason reason);

}
