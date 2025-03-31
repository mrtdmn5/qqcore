/*
 * ************************************************************************************************
 * * © 2020-2022 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.requests.qtil;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.qualcomm.qti.gaiaclient.core.data.Reason;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.plugins.DebugPlugin;
import com.qualcomm.qti.gaiaclient.core.requests.core.Request;
import com.qualcomm.qti.gaiaclient.core.requests.core.RequestListener;

import java.io.File;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getQtilManager;

/**
 * @deprecated since v1.0.72 (or higher) / r00009.1 (or higher). Use {@link DownloadDeviceLogsRequest} instead.
 */
@Deprecated
public class DownloadLogsRequest extends Request<Void, Uri, Reason> {

    private static final String TAG = "DownloadLogsRequest";

    private final String fileName;

    private final String fileProviderAuthority;

    public DownloadLogsRequest(@NonNull RequestListener<Void, Uri, Reason> listener,
                               String fileName, String fileProviderAuthority) {
        super(listener);
        this.fileName = fileName;
        this.fileProviderAuthority = fileProviderAuthority;
    }

    @Override
    public void run(@Nullable Context context) {
        if (context == null) {
            onError(Reason.FILE_CREATION_FAILED);
            return;
        }

        File file = createFile(context, fileName);
        Uri uri = getUriFromFile(context, file, fileProviderAuthority);

        if (uri == null) {
            onError(Reason.FILE_CREATION_FAILED);
            return;
        }

        onProgress(uri);

        DebugPlugin plugin = getQtilManager().getDebugPlugin();
        if (plugin != null) {
            plugin.downloadLogs(file);
            onComplete(null);
        }
        else {
            onError(Reason.NOT_SUPPORTED);
        }
    }

    @Override
    protected void onEnd() {
        // no state to clear
    }

    private File createFile(Context context, String fileName) {
        File file = null;

        try {
            file = context.getFileStreamPath(fileName);
        }
        catch (Exception exception) {
            Log.w(TAG, "[createFile] failed to create file: " + exception.getMessage());
            exception.printStackTrace();
        }

        return file;
    }

    private Uri getUriFromFile(Context context, File file, String fileProviderAuthority) {
        return file != null ? FileProvider.getUriForFile(context, fileProviderAuthority, file) : null;
    }
}
