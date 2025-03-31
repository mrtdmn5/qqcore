/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.qualcomm.qti.gaiaclient.core.upgrade.UpgradeHelper;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpdateOptions;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpgradeConfirmation;
import com.qualcomm.qti.libraries.upgrade.data.ConfirmationOptions;

public interface UpgradePlugin {

    @NonNull
    UpgradeHelper getUpgradeHelper();

    /**
     * @deprecated Use {@link #startUpgrade(Context, UpdateOptions)} instead. The file location is now contained within
     * the options.
     */
    @Deprecated
    default void startUpgrade(Context context, Uri fileLocation, @NonNull UpdateOptions options) {
        throw new RuntimeException("Use startUpgrade(Context, UpdateOptions) instead. The file location is now" +
                                           " contained within the UpdateOptions object.");
    }

    default void startUpgrade(Context context, @NonNull UpdateOptions options) {
        getUpgradeHelper().startUpgrade(context, options);
    }

    default void confirm(UpgradeConfirmation confirmation, @NonNull ConfirmationOptions option) {
        getUpgradeHelper().confirm(confirmation, option);
    }
}
