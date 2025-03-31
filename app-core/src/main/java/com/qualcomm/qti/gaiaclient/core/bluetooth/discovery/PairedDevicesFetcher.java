/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth.discovery;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.DeviceType;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.DiscoveredDevice;
import com.qualcomm.qti.gaiaclient.core.utils.BluetoothUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;

import static com.qualcomm.qti.gaiaclient.core.GaiaClientService.getTaskManager;

public class PairedDevicesFetcher {

    private static final String TAG = "PairedDevicesFetcher";

    @NonNull
    private final PairedDevicesFetcherListener mListener;

    public PairedDevicesFetcher(@NonNull PairedDevicesFetcherListener listener) {
        this.mListener = listener;
    }

    public BluetoothStatus get(Context context) {
        // getting BluetoothAdapter to get resources
        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter(context);
        if (adapter == null) {
            // no BluetoothAdapter = Bluetooth is off or not available for the device
            Log.w(TAG, "[get] BluetoothAdapter is null.");
            return BluetoothStatus.NO_BLUETOOTH;
        }

        // use a handler to make this process ends prior to find any UUIDs
        getTaskManager().runInBackground(() -> findClassicPairedDevices(adapter));

        return BluetoothStatus.IN_PROGRESS;
    }

    private void findClassicPairedDevices(@NonNull BluetoothAdapter adapter) {
        @SuppressLint("MissingPermission") Set<BluetoothDevice> paired = adapter.getBondedDevices();
        mListener.onGetPairedDevices(getClassicPairedDevices(paired));
    }

    @SuppressLint("MissingPermission")
    private List<DiscoveredDevice> getClassicPairedDevices(Set<BluetoothDevice> paired) {
        if (paired == null || paired.isEmpty()) {
            return new ArrayList<>();
        }

        List<DiscoveredDevice> classicPaired = new ArrayList<>();

        paired.forEach(device -> {
            if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC
                    || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                classicPaired.add(new DiscoveredDevice(device.getName(),
                                                              device.getAddress(),
                                                              DeviceType.PAIRED));
            }
        });

        return classicPaired;
    }

    public interface PairedDevicesFetcherListener {

        void onGetPairedDevices(List<DiscoveredDevice> pairedDevices);

    }
}
