/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.upgrade.data;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.qualcomm.qti.gaiaclient.core.gaia.core.version.GaiaVersion;

import java.util.Objects;

public class UpdateOptions {

    private final Uri fileLocation;

    private final int expectedChunkSize;

    private final boolean isLogged;

    private final boolean isUploadFlushed;

    private final boolean isUploadAcknowledged;

    public static UpdateOptions getDefaultOptions(Uri fileLocation, int gaiaVersion) {
        return new UpdateOptions(fileLocation, false, gaiaVersion <= GaiaVersion.V2, true, 0);
    }

    public static UpdateOptions getDefaultOptions(Uri fileLocation, int gaiaVersion, boolean isLogged) {
        return new UpdateOptions(fileLocation, isLogged, gaiaVersion <= GaiaVersion.V2, true, 0);
    }

    /**
     * @deprecated This constructor has been deprecated in favour of the constructor
     *         {@link #UpdateOptions(Uri, boolean, boolean, boolean, int)} or the static builders:
     *         {@link #getDefaultOptions(Uri, int)} and {@link #getDefaultOptions(Uri, int, boolean)}.
     */
    @Deprecated
    public UpdateOptions(boolean isLogged) {
        throw new RuntimeException(
                "UpdateOptions(boolean isLogged) has been deprecated, please use" +
                        " UpdateOptions(Uri, boolean, boolean, boolean, int), getDefaultOptions(Uri, int) or" +
                        " {@link #getDefaultOptions(Uri, int, boolean) instead.");
    }

    /**
     * @deprecated This constructor has been deprecated in favour of the constructor
     *         {@link #UpdateOptions(Uri, boolean, boolean, boolean, int)} or the static builders:
     *         {@link #getDefaultOptions(Uri, int)} and {@link #getDefaultOptions(Uri, int, boolean)}.
     */
    @Deprecated
    public UpdateOptions(boolean isLogged, boolean isUploadFlushed, boolean isUploadAcknowledged,
                         int expectedChunkSize) {
        throw new RuntimeException(
                "UpdateOptions(boolean isLogged) has been deprecated, please use" +
                        " UpdateOptions(Uri, boolean, boolean, boolean, int), getDefaultOptions(Uri, int) or" +
                        " {@link #getDefaultOptions(Uri, int, boolean) instead.");
    }

    public UpdateOptions(Uri fileLocation, boolean isLogged, boolean isUploadFlushed, boolean isUploadAcknowledged,
                         int expectedChunkSize) {
        this.expectedChunkSize = expectedChunkSize;
        this.isLogged = isLogged;
        this.isUploadFlushed = isUploadFlushed;
        this.isUploadAcknowledged = isUploadAcknowledged;
        this.fileLocation = fileLocation;
    }

    public int getExpectedChunkSize() {
        return expectedChunkSize;
    }

    public Uri getFileLocation() {
        return fileLocation;
    }

    public boolean isLogged() {
        return isLogged;
    }

    public boolean isUploadAcknowledged() {
        return isUploadAcknowledged;
    }

    public boolean isUploadFlushed() {
        return isUploadFlushed;
    }

    /**
     * @deprecated UpdateOptions only holds the data and no logic related to that data.
     */
    @Deprecated
    public boolean useDefault() {
        throw new RuntimeException("useDefault is deprecated. UpgradeOption do not hold logic data.");
    }

    @NonNull
    @Override
    public String toString() {
        return "UpdateOptions{" +
                "expectedChunkSize=" + expectedChunkSize +
                ", isLogged=" + isLogged +
                ", isUploadFlushed=" + isUploadFlushed +
                ", isUploadAcknowledged=" + isUploadAcknowledged +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UpdateOptions that = (UpdateOptions) o;
        return expectedChunkSize == that.expectedChunkSize &&
                isLogged == that.isLogged &&
                isUploadFlushed == that.isUploadFlushed &&
                isUploadAcknowledged == that.isUploadAcknowledged &&
                Objects.equals(fileLocation, that.fileLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileLocation, expectedChunkSize, isLogged, isUploadFlushed, isUploadAcknowledged);
    }
}
