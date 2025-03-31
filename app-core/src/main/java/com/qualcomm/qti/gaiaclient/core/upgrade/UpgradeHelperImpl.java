/*
 * ************************************************************************************************
 * * © 2020-2022 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.upgrade;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.qualcomm.qti.gaiaclient.core.bluetooth.TransportManager;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.ConnectionState;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Link;
import com.qualcomm.qti.gaiaclient.core.data.DeviceInfo;
import com.qualcomm.qti.gaiaclient.core.data.Reason;
import com.qualcomm.qti.gaiaclient.core.data.SizeInfo;
import com.qualcomm.qti.gaiaclient.core.gaia.core.GaiaErrorStatus;
import com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm.Format;
import com.qualcomm.qti.gaiaclient.core.gaia.qtil.data.QTILFeature;
import com.qualcomm.qti.gaiaclient.core.publications.PublicationManager;
import com.qualcomm.qti.gaiaclient.core.publications.core.ExecutionType;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.publishers.UpgradePublisher;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.ConnectionSubscriber;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.DeviceInformationSubscriber;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.ProtocolSubscriber;
import com.qualcomm.qti.gaiaclient.core.publications.qtil.subscribers.QTILFeaturesSubscriber;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.ChunkSize;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.ChunkSizeType;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpdateOptions;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpgradeConfirmation;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpgradeErrorStatus;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpgradeFail;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpgradeProgress;
import com.qualcomm.qti.gaiaclient.core.upgrade.data.UpgradeState;
import com.qualcomm.qti.gaiaclient.core.utils.BytesUtils;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;
import com.qualcomm.qti.libraries.upgrade.UpgradeListener;
import com.qualcomm.qti.libraries.upgrade.UpgradeManager;
import com.qualcomm.qti.libraries.upgrade.UpgradeManagerFactory;
import com.qualcomm.qti.libraries.upgrade.data.ConfirmationOptions;
import com.qualcomm.qti.libraries.upgrade.data.ConfirmationType;
import com.qualcomm.qti.libraries.upgrade.data.EndType;
import com.qualcomm.qti.libraries.upgrade.data.ResumePoint;
import com.qualcomm.qti.libraries.upgrade.data.UpgradeError;
import com.qualcomm.qti.libraries.upgrade.messages.OpCodes;
import com.qualcomm.qti.libraries.upgrade.messages.UpgradeAlert;
import com.qualcomm.qti.libraries.upgrade.messages.UpgradeMessage;
import com.qualcomm.qti.libraries.upgrade.messages.UpgradeMessageListener;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getTaskManager;
import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getTransportManager;

class UpgradeHelperImpl {

    private static final String TAG = "UpgradeHelper";

    private static final boolean LOG_METHODS = DEBUG.Upgrade.PLUGIN_HELPER;

    private static final boolean LOG_BYTES = DEBUG.Upgrade.PLUGIN_HELPER_DETAILS;

    @Nullable
    private UpgradeHelperListener mHelperListener;

    private UpgradeState mState = null;

    private final UpgradePublisher mUpgradePublisher = new UpgradePublisher();

    private final UpgradeParameters mParameters = new UpgradeParameters();

    private final ConcurrentLinkedQueue<UploadRequest> mUploadQueue =
            new ConcurrentLinkedQueue<>();

    private final AtomicBoolean mIsBlocked = new AtomicBoolean(false);

    private final ChunkSize mChunkSize = new ChunkSize();

    /**
     * Listen for connection states in order to update the state when reconnecting.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ConnectionSubscriber mConnectionSubscriber = new ConnectionSubscriber() {
        @NonNull
        @Override
        public ExecutionType getExecutionType() {
            return ExecutionType.BACKGROUND;
        }

        @Override
        public void onConnectionStateChanged(Link link, ConnectionState state) {
            Logger.log(LOG_METHODS, TAG, "Subscriber->onConnectionStateChanged", new Pair<>("state", state));
            if (!isUpgrading()) {
                // upgrade is not processing, nothing to do.
                return;
            }

            if (state == ConnectionState.DISCONNECTED) {
                mChunkSize.reset(); // size is checked during handshake
                if (mState == UpgradeState.VALIDATION) {
                    // expected reboot
                    setState(UpgradeState.REBOOT);
                }
                else if (!isReconnecting()) {
                    // upgrade is ongoing and the disconnection is not an expected reboot
                    setState(UpgradeState.RECONNECTING);
                }
            }
            else if (state == ConnectionState.CONNECTED) {
                logBytes(mParameters.isLogged());
            }
        }

        @Override
        public void onConnectionError(Link link, BluetoothStatus reason) {
            // captured by the Upgrade Reconnection Delegate
            if (reason == BluetoothStatus.RECONNECTION_TIME_OUT && isUpgrading()) {
                abort(new UpgradeFail(UpgradeErrorStatus.RECONNECTION_ERROR));
            }
        }
    };

    /**
     * Listen for the features discovery: if the upgrade feature is not discovered -> not supported
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final DeviceInformationSubscriber mDeviceInformationSubscriber =
            new DeviceInformationSubscriber() {
                @NonNull
                @Override
                public ExecutionType getExecutionType() {
                    return ExecutionType.BACKGROUND;
                }

                @Override
                public void onInfo(DeviceInfo info, Object update) {
                }

                @Override
                public void onError(DeviceInfo info, Reason reason) {
                    Logger.log(LOG_METHODS, TAG, "Subscriber->onError",
                               new Pair<>("info", info), new Pair<>("reason", reason));
                    if (info == DeviceInfo.GAIA_VERSION) {
                        abort(new UpgradeFail(UpgradeErrorStatus.GAIA_INITIALISATION_ERROR));
                    }
                }
            };

    @SuppressWarnings("FieldCanBeLocal")
    private final QTILFeaturesSubscriber mFeatureSubscriber = new QTILFeaturesSubscriber() {
        @NonNull
        @Override
        public ExecutionType getExecutionType() {
            return ExecutionType.BACKGROUND;
        }

        @Override
        public void onFeatureSupported(QTILFeature feature, int version) {
            // nothing to do: upgrade to resume through #onPlugged
        }

        @Override
        public void onFeatureNotSupported(QTILFeature feature, Reason reason) {
            if (feature == QTILFeature.UPGRADE && isUpgrading()) {
                abort(new UpgradeFail(UpgradeErrorStatus.GAIA_INITIALISATION_ERROR));
            }
        }

        @Override
        public void onError(Reason reason) {
            if (isUpgrading()) {
                abort(new UpgradeFail(UpgradeErrorStatus.GAIA_INITIALISATION_ERROR));
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final ProtocolSubscriber mProtocolSubscriber = new ProtocolSubscriber() {
        @NonNull
        @Override
        public ExecutionType getExecutionType() {
            return ExecutionType.BACKGROUND;
        }

        @Override
        public void onSizeInfo(SizeInfo info, int size) {
            switch (info) {
                case MAX_RX_PAYLOAD:
                    onAvailableSizeUpdate(size);
                    break;

                case OPTIMUM_RX_PAYLOAD:
                    onOptimumSizeUpdate(size);
                    break;
            }
        }

        @Override
        public void onError(Object info, Reason reason) {
            if (info instanceof SizeInfo) {
                switch ((SizeInfo) info) {
                    case MAX_RX_PAYLOAD:
                        onAvailableSizeUpdate(Format.DEFAULT_PAYLOAD_MAX_LENGTH);
                        break;

                    case OPTIMUM_RX_PAYLOAD:
                        onOptimumSizeUpdate(Format.DEFAULT_PAYLOAD_MAX_LENGTH);
                        break;
                }
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final UpgradeListener mUpgradeListener = new UpgradeListener() {
        @Override
        public void sendUpgradeMessage(byte[] bytes, @Nullable UpgradeMessageListener uploadListener) {
            Logger.log(LOG_BYTES, TAG, "Listener->sendUpgradeMessage",
                       new Pair<>("plugin", mHelperListener), new Pair<>("data", bytes));
            if (mHelperListener != null && uploadListener != null) {
                prepareUploadRequest(bytes, uploadListener);
            }
            else if (mHelperListener != null) {
                mHelperListener.sendUpgradeMessage(bytes);
            }
        }

        @Override
        public void onUpgradeError(UpgradeError error) {
            Logger.log(LOG_METHODS, TAG, "Listener->onUpgradeError", new Pair<>("error", error));
            resetUpload();
            abort(new UpgradeFail(error));
        }

        @Override
        public void onResumePointChanged(ResumePoint point) {
            Logger.log(LOG_METHODS, TAG, "Listener->onResumePointChanged", new Pair<>("point", point));
            setState(matchResumePointToState(point));
        }

        @Override
        public void onUpgradeEnd(EndType type) {
            Logger.log(LOG_METHODS, TAG, "Listener->onUpgradeEnd");
            UpgradeState state = getState(type);
            setState(state);
            mUpgradePublisher.publishProgress(UpgradeProgress.end(state, type));
        }

        @Override
        public void onUploadProgress(ResumePoint resumePoint, double progress) {
            Logger.log(LOG_METHODS, TAG, "Listener->onFileUploadProgress",
                       new Pair<>("progress", progress));
            mUpgradePublisher.publishProgress(UpgradeProgress.upload(matchResumePointToState(resumePoint), progress));
        }

        @Override
        public void onConfirmationRequired(@NonNull ConfirmationType type, @NonNull ConfirmationOptions[] options) {
            Logger.log(LOG_METHODS, TAG, "Listener->onConfirmationRequired", new Pair<>("confirmation", type),
                       new Pair<>("options", options));
            mUpgradePublisher
                    .publishProgress(UpgradeProgress.confirmation(mState, matchTypeToConfirmation(type), options));
        }

        @Override
        public void enableUpgradeMode() {
            Logger.log(LOG_METHODS, TAG, "Listener->enableUpgradeMode");
            sendConnectUpgrade();
        }

        @Override
        public void disableUpgradeMode() {
            Logger.log(LOG_METHODS, TAG, "Listener->disableUpgradeMode");
            sendDisconnectUpgrade();
        }

        @Override
        public void isAborting() {
            Logger.log(LOG_METHODS, TAG, "Listener->isAborting");
            if (mState != UpgradeState.ABORTING && mState != UpgradeState.ABORTED) {
                // prevents multi call to isAborting to update the state
                setState(UpgradeState.ABORTING);
            }
        }

        @Override
        public void onAlert(UpgradeAlert alert, boolean raised) {
            Logger.log(LOG_METHODS, TAG, "Listener->onAlert", new Pair<>("alert", alert), new Pair<>("raised", raised));
            mUpgradePublisher.publishAlert(alert, raised);
        }
    };

    private final UpgradeManager mUpgradeManager =
            UpgradeManagerFactory.buildUpgradeManager(mUpgradeListener,
                                                      (task, delayMs) -> getTaskManager().schedule(task, delayMs));

    UpgradeHelperImpl(@NonNull PublicationManager publicationManager) {
        publicationManager.register(mUpgradePublisher);
        publicationManager.subscribe(mConnectionSubscriber);
        publicationManager.subscribe(mDeviceInformationSubscriber);
        publicationManager.subscribe(mFeatureSubscriber);
        publicationManager.subscribe(mProtocolSubscriber);
        mUpgradeManager.showDebugLogs(DEBUG.Upgrade.UPGRADE_MANAGER);
    }

    void confirm(UpgradeConfirmation confirmation, @NonNull ConfirmationOptions option) {
        Logger.log(LOG_METHODS, TAG, "onConfirmationRequired",
                   new Pair<>("confirmation", confirmation), new Pair<>("option", option));
        mUpgradeManager.onConfirmation(matchConfirmationToType(confirmation), option);
    }

    void abort() {
        Logger.log(LOG_METHODS, TAG, "abort", new Pair<>("state", mState));
        abort(null);
    }

    void startUpgrade(Context context, @NonNull UpdateOptions options) {
        Logger.log(LOG_METHODS, TAG, "startUpgrade", new Pair<>("file_uri",
                                                                options.getFileLocation() == null ? "null" :
                                                                options.getFileLocation().getLastPathSegment()));

        if (isUpgrading()) {
            // an upgrade is already in progress: no error as progress will be published
            return;
        }

        // getting the data from the Uri
        byte[] file = getBytesFromUri(context, options.getFileLocation());
        byte[] md5Checksum = BytesUtils.getMD5FromUri(context, options.getFileLocation());

        if (file == null || file.length == 0) {
            // bytes could not be found for the given uri or the file is empty
            mUpgradePublisher.publishError(new UpgradeFail(UpgradeErrorStatus.FILE_ERROR));
            setState(UpgradeState.ABORTED);
            mUpgradePublisher.publishProgress(UpgradeProgress.end(UpgradeState.ABORTED, EndType.ABORTED));
            return;
        }

        // setting the options
        int expectedChunkSize =
                0 < options.getExpectedChunkSize() ? options.getExpectedChunkSize() : mChunkSize.getDefault();
        mParameters
                .set(expectedChunkSize, options.isLogged(), options.isUploadFlushed(), options.isUploadAcknowledged());

        logBytes(mParameters.isLogged());
        // init the chunk size for the manager
        setChunkSize();

        // start upgrade
        setState(UpgradeState.INITIALISATION);
        mUpgradeManager.startUpgrade(file, md5Checksum);
    }

    void onUpgradeMessage(byte[] data) {
        Logger.log(LOG_BYTES, TAG, "Listener->onUpgradeMessage", new Pair<>("data", data));
        mUpgradeManager.onUpgradeMessage(data);
    }

    void onAcknowledged() {
        Logger.log(LOG_METHODS, TAG, "Listener->onAcknowledged");

        onMessageTransferred();
    }

    void onSendingFailed(Reason ignored) {
        Logger.log(LOG_METHODS, TAG, "onSendingFailed", new Pair<>("reason", ignored));
        // error is ignored: device is going to be disconnected if an error occurred while sending a packet
    }

    void onErrorResponse(UpgradeGaiaCommand command, GaiaErrorStatus status) {
        Logger.log(LOG_METHODS, TAG, "onErrorResponse", new Pair<>("status", status));
        if (isUpgrading()) {
            switch (command) {
                case CONNECT:
                    onUpgradeModeOnFailed(status);
                    break;
                case CONTROL:
                    abort(new UpgradeFail(status));
                    break;
                case DISCONNECT:
                    // if message cannot reach the device it is assumed that the transport is disconnected
                    onUpgradeDisconnected();
                    break;
            }
            abort(new UpgradeFail(status));
        }
    }

    void onUpgradeConnected() {
        Logger.log(LOG_METHODS, TAG, "onUpgradeConnected");
        if (isUpgrading()) {
            mUpgradeManager.onUpgradeModeEnabled();
        }
    }

    void onUpgradeDisconnected() {
        Logger.log(LOG_METHODS, TAG, "onUpgradeDisconnected");
        mUpgradeManager.onUpgradeModeDisabled();
    }

    void onPlugged(UpgradeHelperListener listener) {
        Logger.log(LOG_METHODS, TAG, "start");
        mHelperListener = listener;

        if (isUpgrading()) {
            setState(UpgradeState.INITIALISATION);
        }

        mUpgradeManager.start();
    }

    void onUnplugged() {
        mUpgradeManager.pause();
        resetUpload();
    }

    boolean isFlushed() {
        return mParameters.isFlushed();
    }

    void release() {
        mUpgradeManager.release();
        mHelperListener = null;
        mUploadQueue.clear();
    }

    private void resetUpload() {
        Logger.log(LOG_METHODS, TAG, "resetUpload");
        if (mHelperListener != null) {
            mHelperListener.cancelAll();
        }
        mUploadQueue.clear();
        mIsBlocked.set(false);
    }

    private void abort(UpgradeFail error) {
        if (mState != UpgradeState.ABORTING) {
            resetUpload();
            setState(UpgradeState.ABORTING);

            if (error != null) {
                mUpgradePublisher.publishError(error);
            }
            mUpgradeManager.abort();

            // stop the logging of the bytes
            logBytes(DEBUG.Bluetooth.COMMUNICATION_THREADS_DATA);
        }
    }

    private void onUpgradeModeOnFailed(GaiaErrorStatus status) {
        resetUpload();
        mUpgradeManager.forceStop();
        setState(UpgradeState.END);
        mUpgradePublisher.publishError(new UpgradeFail(status));
        mUpgradePublisher.publishProgress(UpgradeProgress.end(UpgradeState.END, null));
    }

    private void sendConnectUpgrade() {
        Logger.log(LOG_METHODS, TAG, "sendConnectUpgrade");
        if (mHelperListener != null) {
            mHelperListener.setUpgradeModeOn();
        }
    }

    private void sendDisconnectUpgrade() {
        Logger.log(LOG_METHODS, TAG, "setUpgradeModeOff");
        if (mHelperListener != null) {
            mHelperListener.setUpgradeModeOff();
        }
    }

    private void setState(UpgradeState state) {
        Logger.log(LOG_METHODS, TAG, "setState",
                   new Pair<>("previous", mState), new Pair<>("new", state));
        if (state != mState) {
            mState = state;
            mUpgradePublisher.publishProgress(UpgradeProgress.state(state));
        }
    }

    private boolean isUpgrading() {
        return mUpgradeManager.isUpgrading();
    }

    private boolean isReconnecting() {
        return mState == UpgradeState.REBOOT || mState == UpgradeState.RECONNECTING;
    }

    private byte[] getBytesFromUri(Context context, Uri uri) {
        return BytesUtils.getBytesFromUri(context, uri);
    }

    private void onMessageTransferred() {
        // inform the manager that the message was transferred
        mUpgradeManager.onMessageTransferred();
        // handle next upload request if required
        if (mParameters.isUploadAcknowledged()) {
            mIsBlocked.set(false);
            processUploadRequest();
        }
    }

    private void prepareUploadRequest(@NonNull byte[] bytes,
                                      @NonNull UpgradeMessageListener listener) {
        boolean isAcknowledged = mParameters.isUploadAcknowledged();

        UploadRequest request = new UploadRequest(bytes, mParameters.isFlushed(), isAcknowledged,
                                                  listener);

        if (isAcknowledged) {
            mUploadQueue.add(request);
            processUploadRequest();
        }
        else {
            sendUploadRequest(request);
        }
    }

    private void processUploadRequest() {
        if (mUploadQueue.isEmpty() || !mIsBlocked.compareAndSet(false, true)) {
            return;
        }

        UploadRequest request = mUploadQueue.poll();

        if (request != null) {
            sendUploadRequest(request);
        }
        else {
            Log.w(TAG, "[processUploadRequest] Unexpected null request.");
        }
    }

    private void sendUploadRequest(@NonNull UploadRequest request) {
        if (mHelperListener == null) {
            return;
        }

        if (mState != UpgradeState.UPLOAD) {
            Log.w(TAG, "[sendUploadRequest] helper is not in UPLOAD state.");
            return;
        }

        mHelperListener.sendUpgradeMessage(request.getData(), request.isAcknowledged(),
                                           request.isFlushed(), () -> onPacketSent(request));
    }

    private void onPacketSent(@NonNull UploadRequest request) {
        request.getUpgradeMessageListener().onSent();

        if (!request.isAcknowledged()) {
            onMessageTransferred();
        }
    }

    private void onAvailableSizeUpdate(int payloadSize) {
        Logger.log(LOG_METHODS, TAG, "onAvailableSizeUpdate", new Pair<>("payloadSize",
                                                                         payloadSize));
        int newSize = getChunkSizeFromPayloadSize(payloadSize);

        // update the available size
        mChunkSize.setAvailable(newSize);
        mUpgradePublisher.publishChunkSize(ChunkSizeType.AVAILABLE, mChunkSize.getAvailable());

        if (isUpgrading()) {
            // must update the upgrade manager size
            setChunkSize();
        }
    }

    private void onOptimumSizeUpdate(int payloadSize) {
        Logger.log(LOG_METHODS, TAG, "onOptimumSizeUpdate", new Pair<>("payloadSize", payloadSize));
        int newSize = getChunkSizeFromPayloadSize(payloadSize);
        mChunkSize.setDefault(newSize);
        mUpgradePublisher.publishChunkSize(ChunkSizeType.DEFAULT, newSize);
    }

    /**
     * To set the chunk size, this method checks if the expected size can be used and then set the closest depending on
     * the available size.
     */
    private void setChunkSize() {
        int expected = mParameters.getExpectedChunkSize();

        int size = mChunkSize.getChunkSize(expected);
        int result = mUpgradeManager.setChunkSize(size);

        Logger.log(LOG_METHODS, TAG, "setChunkSize", new Pair<>("expected", expected),
                   new Pair<>("available", mChunkSize.getAvailable()), new Pair<>("size", size),
                   new Pair<>("set", result));

        mUpgradePublisher.publishChunkSize(ChunkSizeType.SET, result);
    }

    /**
     * <p>To get the chunk size when having the size of a GAIA payload.</p>
     *
     * @return the payload minus the upgrade message header and the header of UPGRADE_DATA packets that contains the
     *         data chunks during the upload.
     */
    private int getChunkSizeFromPayloadSize(int gaiaPayloadSize) {
        return gaiaPayloadSize - UpgradeMessage.HEADER_LENGTH - OpCodes.UpgradeData.DATA_HEADER_LENGTH;
    }

    private void logBytes(boolean log) {
        TransportManager manager = getTransportManager();
        manager.logBytes(log);
    }

    private UpgradeState matchResumePointToState(ResumePoint point) {
        switch (point) {
            case START:
                return UpgradeState.UPLOAD;

            case PRE_VALIDATE:
            case PRE_REBOOT:
                return UpgradeState.VALIDATION;

            case POST_REBOOT:
            case COMMIT:
            case POST_COMMIT:
                return UpgradeState.VERIFICATION;

            default:
                return UpgradeState.INITIALISATION;
        }
    }

    private UpgradeConfirmation matchTypeToConfirmation(ConfirmationType type) {
        switch (type) {
            case BATTERY_LOW_ON_DEVICE:
                return UpgradeConfirmation.BATTERY_LOW_ON_DEVICE;
            case COMMIT:
                return UpgradeConfirmation.COMMIT;
            case IN_PROGRESS:
                return UpgradeConfirmation.IN_PROGRESS;
            case TRANSFER_COMPLETE:
                return UpgradeConfirmation.TRANSFER_COMPLETE;
            case WARNING_FILE_IS_DIFFERENT:
                return UpgradeConfirmation.WARNING_FILE_IS_DIFFERENT;
            default:
                return null;
        }
    }

    private ConfirmationType matchConfirmationToType(UpgradeConfirmation confirmation) {
        switch (confirmation) {
            case BATTERY_LOW_ON_DEVICE:
                return ConfirmationType.BATTERY_LOW_ON_DEVICE;
            case COMMIT:
                return ConfirmationType.COMMIT;
            case IN_PROGRESS:
                return ConfirmationType.IN_PROGRESS;
            case TRANSFER_COMPLETE:
                return ConfirmationType.TRANSFER_COMPLETE;
            case WARNING_FILE_IS_DIFFERENT:
                return ConfirmationType.WARNING_FILE_IS_DIFFERENT;
            default:
                return null;
        }
    }

    private static UpgradeState getState(EndType type) {
        if (type == null) {
            return UpgradeState.END;
        }

        switch (type) {
            case COMPLETE:
                return UpgradeState.COMPLETE;
            case ABORTED:
                return UpgradeState.ABORTED;
            case SILENT_COMMIT:
            case UPGRADE_IN_PROGRESS_WITH_DIFFERENT_ID:
            default:
                return UpgradeState.END;
        }
    }
}
