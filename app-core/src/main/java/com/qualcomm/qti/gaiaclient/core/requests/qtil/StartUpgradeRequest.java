/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.UpgradePlugin;
import com.qualcomm.qti.gaiaclient.core.requests.core.Request;
import com.qualcomm.qti.gaiaclient.core.requests.core.RequestListener;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpdateOptions;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getQtilManager;

public class StartUpgradeRequest extends Request<Void, Void, Void> {

    @NonNull
    private final UpdateOptions options;

    /**
     * @deprecated Use {@link #StartUpgradeRequest(UpdateOptions, RequestListener)} instead. The file location is now
     *         contained within the options.
     */
    @Deprecated
    public StartUpgradeRequest(Uri fileLocation, @NonNull UpdateOptions options,
                               @NonNull RequestListener<Void, Void, Void> listener) {
        super(listener);
        throw new RuntimeException(
                "Use StartUpgradeRequest(UpdateOptions, RequestListener) instead. The file location" +
                        " is now contained within the UpdateOptions object.");
    }

    public StartUpgradeRequest(@NonNull UpdateOptions options, @NonNull RequestListener<Void, Void, Void> listener) {
        super(listener);
        this.options = options;
    }

    @Override
    public void run(@Nullable Context context) {
        UpgradePlugin plugin = getQtilManager().getUpgradePlugin();
        if (plugin != null) {
            plugin.startUpgrade(context, options);
            onComplete(null);
        }
        else {
            onError(null);
        }
    }

    @Override
    protected void onEnd() {
    }

}
