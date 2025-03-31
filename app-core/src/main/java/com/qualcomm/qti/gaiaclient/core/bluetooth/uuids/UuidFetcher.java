/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.uuids;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getTaskManager;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.utils.BluetoothUtils;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * <p>This class fetches the list of {@link UUID} supported by a Bluetooth Device.</p>
 * <p>This class uses {@link BluetoothDevice#getUuids() getUuids()} to identify the supported
 * UUIDs.</p>
 * <p>If {@link BluetoothDevice#getUuids() getUuids()} returns a null array - happens when the
 * system hasn't fetch the SDP record yet - this class uses
 * {@link BluetoothDevice#fetchUuidsWithSdp() fetchUuidsWithSdp()} to fetch them. In this case
 * this class also register a {@link android.content.BroadcastReceiver BroadcastReceiver} to be
 * notified of the SDP record once discovered by the system. see
 * {@link UUIDReceiver UUIDReceiver}.</p>
 */
public class UuidFetcher {

    /**
     * Used to tag the any message logged in this class.
     */
    private static final String TAG = "UuidFetcher";

    private static final boolean LOG_METHODS = DEBUG.Bluetooth.UUID_FETCHER;

    /**
     * The delay in millisecond before to cancel a {@link UUIDReceiver UUIDReceiver}.
     */
    private static final long UNREGISTER_UUID_RECEIVER_DELAY_MS = 5000;
    /**
     * A listener to be notified when the fetcher has completed.
     */
    @NonNull
    private final UuidFetcherListener mListener;
    /**
     * <p>To know the process is waiting for UUIDs to be fetched to complete.</p>
     */
    private boolean isWaitingForUUIDs = false;
    /**
     * The BluetoothDevice to get the Uuids for.
     */
    @Nullable
    private final BluetoothDevice mDevice;

    /**
     * <p>Constructor for the fetcher. This constructor looks for the {@link BluetoothDevice
     * BluetoothDevice} that corresponds to the given Bluetooth address.</p>
     *
     * @param listener
     *         A listener to be notified when the fetcher has completed.
     * @param context
     *         A context to get the BluetoothAdapter from. This is not kept as a field.
     * @param address
     *         The Bluetooth address of the device to get the Uuids from.
     */
    public UuidFetcher(@NonNull UuidFetcherListener listener,
                       @NonNull Context context, @NonNull String address) {
        this.mListener = listener;

        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter(context);
        this.mDevice = adapter != null ?
                       BluetoothUtils.findBluetoothDevice(adapter, address) : null;
    }

    /**
     * <p>Starts the UUIDs fetching process.</p>
     * <p>Result of the fetching process will be sent to the
     * {@link UuidFetcherListener UuidFetcherListener} that has been attached to
     * this fetcher.</p>
     *
     * @return {@link BluetoothStatus#IN_PROGRESS IN_PROGRESS} if the process could start, any
     *         other value would give the reason of the failure.
     */
    public BluetoothStatus fetch(@NonNull Context context) {
        Logger.log(LOG_METHODS, TAG, "fetch");

        if (mDevice == null) {
            Log.w(TAG, "[fetch] Bluetooth Device is null.");
            return BluetoothStatus.DEVICE_NOT_FOUND;
        }

        // use a handler to make this process ends prior to find any UUIDs
        getTaskManager().runInBackground(() -> findUuids(context, mDevice));

        return BluetoothStatus.IN_PROGRESS;
    }

    /**
     * <p>This method gets the UUID services of a device and initiates the fetching of the SDP
     * record if no UUID services could be found.</p>
     *
     * @param device
     *         The device to get the UUIDs from.
     * @param context
     *         A non null context to register a receiver when fetching the device UUID services
     *         record.
     */
    private void findUuids(@NonNull Context context, @NonNull BluetoothDevice device) {
        Logger.log(LOG_METHODS, TAG, "findUuids", new Pair<>("device", device.getAddress()));

        @SuppressLint("MissingPermission") ParcelUuid[] uuids = device.getUuids();

        if (uuids != null) {
            dispatchUuids(uuids);
            return;
        }

        // UUIDs service record needs to be discovered, the discovery is asynchronous =>
        // starts the discovery and returns true.
        Log.i(TAG, "[fetchUuidAndConnect] No UUIDs found, starting procedure to fetch" +
                " UUIDs with SDP.");
        fetchSdpRecord(context, device);
    }

    /**
     * <p>This method starts the procedure to fetch the device's UUIDs.</p>
     * <p>This method registers a {@link UUIDReceiver UUIDReceiver} to get the broadcast events for
     * {@link BluetoothDevice#ACTION_UUID ACTION_UUID}, calls
     * {@link BluetoothDevice#fetchUuidsWithSdp() fetchSdpRecord} and adds a time out in order to
     * unregister the receiver if the receiver was not receiving any updates from the system.</p>
     *
     * @param device
     *         The device to fetch UUIDs for.
     * @param context
     *         The context ot use to register the receiver.
     */
    @SuppressLint("MissingPermission")
    private void fetchSdpRecord(@NonNull Context context, @NonNull BluetoothDevice device) {
        Logger.log(LOG_METHODS, TAG, "fetchSdpRecord", new Pair<>("device", device.getAddress()));

        isWaitingForUUIDs = true;

        // Cancel any discovery otherwise UUIDs cannot be fetched.
        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter(context);
        if (adapter != null) {
            adapter.cancelDiscovery();
        }

        // register a UUIDReceiver
        final UUIDReceiver receiver = new UUIDReceiver(this::onUuidsFound, device);
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_UUID);
        context.registerReceiver(receiver, filter);

        // request the system to fetch the SDP record
        device.fetchUuidsWithSdp();

        // Add runnable to cancel
        getTaskManager().schedule(() -> {
            Logger.log(LOG_METHODS, TAG, "fetchSdpRecord->TimeOutRunnable",
                       new Pair<>("device", device.getAddress()));

            context.unregisterReceiver(receiver);
            if (isWaitingForUUIDs) {
                isWaitingForUUIDs = false;
                Log.w(TAG, "[fetchSdpRecord] time out.");
                mListener.onFailed(BluetoothStatus.TIME_OUT);
            }
        }, UNREGISTER_UUID_RECEIVER_DELAY_MS);
    }

    /**
     * <p>This method checks that the found Uuids belongs to the expected device and request them
     * to be dispatched to the listener.</p>
     *
     * @param device
     *         The device for which UUIDs have been found.
     * @param uuids
     *         The UUIDs that have been found.
     */
    private void onUuidsFound(@NonNull BluetoothDevice device, @NonNull ParcelUuid[] uuids) {
        Logger.log(LOG_METHODS, TAG, "onUuidsFound", new Pair<>("device", device.getAddress()));

        if (!isWaitingForUUIDs) {
            Log.i(TAG, "[onUuidsFound] Not waiting for UUIDs (anymore?)");
            return;
        }

        if (device.equals(mDevice)) {
            isWaitingForUUIDs = false; // all UUIDs are received at once
            dispatchUuids(uuids);
        }
    }

    /**
     * <p>Notifies the given Uuids to the listener.</p>
     */
    private void dispatchUuids(@NonNull ParcelUuid[] uuids) {
        Logger.log(LOG_METHODS, TAG, "dispatchUuids");
        mListener.onUuidFetched(getUUIDs(uuids));
    }

    /**
     * <p>This method matches an array of {@link ParcelUuid ParcelUuid} to a list of {@link UUID UUID}.</p>
     *
     * @param parcelUuids
     *         The list of parcel UUIDs to get a list of matching {@link UUID uuids}.
     *
     * @return a list of {@link UUID UUID} that matches the given <code>uuids</code>.
     */
    private List<UUID> getUUIDs(ParcelUuid[] parcelUuids) {
        Logger.log(LOG_METHODS, TAG, "getUUIDs");

        if (parcelUuids == null || parcelUuids.length == 0) {
            return new ArrayList<>();
        }

        List<UUID> uuids = new ArrayList<>();

        Arrays.stream(parcelUuids).forEach(parcelUuid -> {
            uuids.add(parcelUuid.getUuid());
        });

        return uuids;
    }

    /**
     * Listener to get the result from this fetcher.
     */
    public interface UuidFetcherListener {

        /**
         * <p>This method dispatches the list of {@link UUID uuids} that has been
         * found by the fetcher.</p>
         *
         * @param uuids
         *         The list of supported uuids that has been found.
         */
        void onUuidFetched(List<UUID> uuids);

        /**
         * <p>This method informs that the fetcher has failed to get a list of uuids.</p>
         *
         * @param reason
         *         The reason for the failure.
         */
        void onFailed(BluetoothStatus reason);

    }
}
