/*
 * ************************************************************************************************
 * * © 2020-2021 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.        *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyser;
import com.qualcomm.qti.gaiaclient.core.bluetooth.communication.CommunicationError;
import com.qualcomm.qti.gaiaclient.core.bluetooth.communication.Communicator;
import com.qualcomm.qti.gaiaclient.core.bluetooth.communication.IdCreator;
import com.qualcomm.qti.gaiaclient.core.bluetooth.communication.ReceivingListener;
import com.qualcomm.qti.gaiaclient.core.bluetooth.connection.ConnectionListener;
import com.qualcomm.qti.gaiaclient.core.bluetooth.connection.ConnectionThread;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.BluetoothStatus;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.ConnectionState;
import com.qualcomm.qti.gaiaclient.core.bluetooth.data.Link;
import com.qualcomm.qti.gaiaclient.core.utils.BluetoothUtils;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;

import java.util.Collection;
import java.util.UUID;

/**
 * <p>This class provides the tools to connect, disconnect and manage a connection with a Bluetooth
 * device over the RFCOMM channel. The port is known by the system and determined with the UUID
 * this client provides to the system.</p>
 */
public class RfcommClient implements ReceivingListener, ConnectionListener, DataSender {

    // =================================================================================
    // CONSTANT FIELDS

    /**
     * <p>The tag to display for logs.</p>
     */
    private static final String TAG = "RfcommClient";

    private static final boolean LOG_METHODS = DEBUG.Bluetooth.RFCOMM_CLIENT;


    // ================================================================================
    // PRIVATE FIELDS

    /**
     * The description of the connection maintained by this client.
     */
    @NonNull
    private final Link mLink;
    /**
     * The receiver for incoming bytes.
     */
    @NonNull
    private final StreamAnalyser mStreamAnalyser;
    /**
     * A listener to get updates about the connection from this client.
     */
    @NonNull
    private final RfcommClientListener mListener;
    /**
     * <p>The Thread that processes a connection to a device.</p>
     * <p>This field is null when there is no ongoing connection or once the connection has been
     * established.</p>
     */
    private ConnectionThread mConnectionThread = null;
    /**
     * <p>The Thread that allows to communicate with a connected device.</p>
     * <p>This field is null if there is no active connection.</p>
     */
    private Communicator mCommunicator = null;
    /**
     * The connection state of this client.
     */
    private ConnectionState mState = ConnectionState.DISCONNECTED;
    /**
     * The Bluetooth Device for which this class/object provides a RFCOMM connection.
     */
    private BluetoothDevice mDevice;


    // ==================================================================================
    // CONSTRUCTOR

    /**
     * <p>Constructor of this class to get a provider of RFCOMM client connection.</p>
     *
     * @param link
     *         The link that describes the connection this client should maintain.
     * @param analyser
     *         The stream analyser that should receive any incoming data from the connection
     *         maintained by this client.
     * @param listener
     *         The listener that should receive updates about the status of the connection
     *         maintained by this client.
     */
    public RfcommClient(@NonNull Link link, @NonNull StreamAnalyser analyser,
                        @NonNull RfcommClientListener listener) {
        mStreamAnalyser = analyser;
        mListener = listener;
        mLink = link;
    }


    // ============================================================================
    // OVERRIDE METHODS

    @Override // ReceivingListener
    public void onCommunicationReady() {
        Logger.log(LOG_METHODS, TAG, "onCommunicationReady",
                   new Pair<>("address", mLink.getBluetoothAddress()));
        // update connection state
        setConnectionState(ConnectionState.CONNECTED);
    }

    @Override // ReceivingListener
    public void onCommunicationEnded() {
        Logger.log(LOG_METHODS, TAG, "onCommunicationEnded",
                   new Pair<>("address", mLink.getBluetoothAddress()));
        // update connection state
        setConnectionState(ConnectionState.DISCONNECTED);
        // thread has ended/is going to end
        cancelCommunicator();
    }

    @Override // ReceivingListener
    public void onCommunicationFailed(CommunicationError reason) {
        Logger.log(LOG_METHODS, TAG, "onCommunicationFailed",
                   new Pair<>("address", mLink.getBluetoothAddress()));
        // Dispatch the corresponding failure message
        switch (reason) {
            case CONNECTION_LOST:
                mListener.onConnectionError(BluetoothStatus.CONNECTION_LOST);
                break;
            case INITIALISATION_FAILED:
                mListener.onConnectionError(BluetoothStatus.CONNECTION_FAILED);
                break;
        }
    }

    @Override // ConnectionListener
    public void onConnectionFailed() {
        Logger.log(LOG_METHODS, TAG, "onConnectionFailed",
                   new Pair<>("address", mLink.getBluetoothAddress()));
        // update connection state
        setConnectionState(ConnectionState.DISCONNECTED);
        // Dispatch the corresponding failure message
        mListener.onConnectionError(BluetoothStatus.CONNECTION_FAILED);
        // Cancel the connection thread
        cancelConnectionThread();
    }

    @Override // ConnectionListener
    public void onConnectionSuccess(@NonNull BluetoothSocket socket) {
        Logger.log(LOG_METHODS, TAG, "onConnectionSuccess",
                   new Pair<>("address", mLink.getBluetoothAddress()));
        // Cancel the thread that completed the connection
        cancelConnectionThread();
        // connection is successful
        onSocketConnected(socket);
    }

    @Override // Object
    @NonNull
    public String toString() {
        return "RfcommClient{" +
                "link=" + mLink +
                ", state=" + mState +
                ", connectionThread=" + mConnectionThread +
                ", communicator=" + mCommunicator +
                '}';
    }


    // ============================================================================
    // PROTECTED METHODS

    /**
     * <p>To get the description of the connection maintained by this client.</p>
     *
     * @return The link as given when creating the client.
     */
    public Link getLink() {
        return mLink;
    }

    /**
     * <p>This method initiates a new connection over the RFCOMM channel using the link
     * parameters given when creating the client.</p>
     * <p>This method determines the parameters of the connection by using
     * {@link BluetoothUtils#findBluetoothDevice(BluetoothAdapter, String)} to get the
     * {@link BluetoothDevice BluetoothDevice} to connect with and the {@link UUID}
     * provided when creating the client.</p>
     * <p>The call to this method is successful if it returns one of the following status:
     * <li>{@link BluetoothStatus#IN_PROGRESS}: the connection is in progress, the
     * {@link RfcommClientListener RfcommClientListener} will received updates on the process.</li>
     * <li>{@link BluetoothStatus#ALREADY_CONNECTED}: the device is already connected.</li>
     * Any other {@link BluetoothStatus} provides the reason the connection was unsuccessful.</p>
     *
     * @return Return <code>true</code> if the connection has been initiated, false if it cannot be
     *         started.
     */
    public BluetoothStatus connect(@NonNull Context context) {
        // logging
        Logger.log(LOG_METHODS, TAG, "connect", new Pair<>("address", mLink.getBluetoothAddress()));

        // checking current status
        if (getConnectionState() == ConnectionState.CONNECTED) {
            // already connected
            Log.w(TAG, "[connect] Client is already connected.");
            return BluetoothStatus.ALREADY_CONNECTED;
        }

        // getting BluetoothAdapter to get resources
        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter(context);
        if (adapter == null) {
            // no BluetoothAdapter = Bluetooth is off or not available for the device
            Log.w(TAG, "[connect] BluetoothAdapter is null.");
            return BluetoothStatus.NO_BLUETOOTH;
        }

        // getting BluetoothDevice to initiate a connection
        BluetoothDevice device = BluetoothUtils.findBluetoothDevice(adapter,
                                                                    mLink.getBluetoothAddress());

        // all check passed successfully, the request can be initiated
        return connect(adapter, device);
    }

    /**
     * <p>This method will initiate a connection with the last known BluetoothDevice with which a
     * connection had
     * been established or attempted.</p>
     *
     * @return <code>true</code> if the reconnection process could be initiated.
     */
    BluetoothStatus reconnect() {
        Logger.log(LOG_METHODS, TAG, "reconnect",
                   new Pair<>("address", mLink.getBluetoothAddress()));
        // checking current status
        ConnectionState state = getConnectionState();

        if (state == ConnectionState.CONNECTED) {
            // already connected
            Log.w(TAG, "[connect] Client is already connected.");
            return BluetoothStatus.ALREADY_CONNECTED;
        }

        if (state == ConnectionState.CONNECTING) {
            return BluetoothStatus.IN_PROGRESS;
        }

        BluetoothAdapter adapter = BluetoothUtils.getBluetoothAdapter(null);
        return connect(adapter, mDevice);
    }

    /**
     * <p>To disconnect from an ongoing connection or to stop a connection process.</p>
     * <p>This method cancels all Thread that might be running in order to maintain a connection
     * with a device over RFCOMM channel.</p>
     */
    public void disconnect() {
        Logger.log(LOG_METHODS, TAG, "disconnect", new Pair<>("address",
                                                              mLink.getBluetoothAddress()));

        if (getConnectionState() == ConnectionState.DISCONNECTED) {
            Log.w(TAG, "[disconnect] already disconnected.");
            return;
        }

        setConnectionState(ConnectionState.DISCONNECTING);

        // cancel any running thread
        cancelConnectionThread();
        cancelCommunicator();

        setConnectionState(ConnectionState.DISCONNECTED);

        Log.i(TAG, "[disconnect] RFCOMM client disconnected from BluetoothDevice "
                + mLink.getBluetoothAddress());
    }

    /**
     * <p>To send some data to a connected device.</p>
     *
     * @param data
     *         The data to send to the device and its parameters.
     *
     * @return true if the sending could be initiated.
     */
    @Override
    public long sendData(@NonNull byte[] data, boolean isFlushed, SendListener listener) {
        Logger.log(LOG_METHODS, TAG, "sendData");
        return mCommunicator == null ? IdCreator.NULL_ID : mCommunicator.sendData(data, isFlushed, listener);
    }

    @Override
    public void holdData(Collection<Long> ids) {
        if (mCommunicator != null) {
            mCommunicator.holdData(ids);
        }
    }

    @Override
    public void resumeData(Collection<Long> ids) {
        if (mCommunicator != null) {
            mCommunicator.resumeData(ids);
        }
    }

    @Override
    public void cancelData(Collection<Long> ids) {
        if (mCommunicator != null) {
            mCommunicator.cancelData(ids);
        }
    }

    /**
     * <p>To know if this client has an established communication with a remote device.</p>
     *
     * @return True if the client is ready to let the application communicate with the device.
     */
    public boolean isConnected() {
        return getConnectionState() == ConnectionState.CONNECTED;
    }

    void logBytes(boolean log) {
        if (mCommunicator != null) {
            mCommunicator.setLogBytes(log || DEBUG.Bluetooth.COMMUNICATION_THREADS_DATA);
        }
    }


    // ==============================================================================
    // PRIVATE METHODS

    /**
     * Sets the current state of the connection this client maintains.
     *
     * @param state
     *         The new state of the connection.
     */
    private synchronized void setConnectionState(ConnectionState state) {
        Logger.log(LOG_METHODS, TAG, "setConnectionState",
                   new Pair<>("previous", mState), new Pair<>("new", state));
        mState = state;
        mListener.onConnectionStateChanged(state);
    }

    /**
     * <p>This method cancels the ConnectionThread if it exists.</p>
     */
    private void cancelConnectionThread() {
        if (mConnectionThread != null) {
            mConnectionThread.cancel();
            mConnectionThread = null;
        }
    }

    /**
     * <p>This method cancels the threads of the Communicator if it exists.</p>
     */
    private void cancelCommunicator() {
        if (mCommunicator != null) {
            mCommunicator.cancel();
            mCommunicator = null;
        }
    }

    /**
     * <p>Gets the current connection state between this service and a device.</p>
     *
     * @return the current connection state.
     */
    private ConnectionState getConnectionState() {
        ConnectionState state;
        synchronized (this) {
            state = mState;
        }
        return state;
    }

    @SuppressLint("MissingPermission")
    private BluetoothStatus connect(BluetoothAdapter adapter, BluetoothDevice device) {
        // logging
        Logger.log(LOG_METHODS, TAG, "connect",
                   new Pair<>("device=", (device == null ? "null" : device.getAddress())));

        if (device == null) {
            Log.w(TAG, "[connect] Device is null.");
            return BluetoothStatus.DEVICE_NOT_FOUND;
        }

        // checking that the device accepts connections over RFCOMM channel
        if (device.getType() != BluetoothDevice.DEVICE_TYPE_CLASSIC
                && device.getType() != BluetoothDevice.DEVICE_TYPE_DUAL) {
            // the device is not BT classic compatible
            Log.w(TAG, "[connect] connection failed: the device is not Bluetooth Classic" +
                    " (RFCOMM) compatible.");
            return BluetoothStatus.DEVICE_NOT_COMPATIBLE;
        }

        mDevice = device;

        // all check passed successfully, the request can be initiated
        return initiateConnection(device, mLink.getTransport().getUuid(), adapter);
    }

    /**
     * <p><p>To initiate a connection over the RFCOMM channel with the given Bluetooth
     * device and using the given UUID Service.</p>
     *
     * @param device
     *         The BluetoothDevice to connect with.
     * @param uuid
     *         The UUID to use to connect over the RFCOMM channel.
     */
    @SuppressLint("MissingPermission")
    private BluetoothStatus initiateConnection(@NonNull BluetoothDevice device, @NonNull UUID uuid,
                                               @NonNull BluetoothAdapter adapter) {
        Logger.log(LOG_METHODS, TAG, "initiateConnection", new Pair<>("deviceAddress", device.getAddress()),
                   new Pair<>("uuid", uuid));

        // connection is starting
        setConnectionState(ConnectionState.CONNECTING);

        // Cancel any thread attempting to make a connection
        cancelConnectionThread();
        // Cancel any thread currently running a connection
        cancelCommunicator();
        // Cancel discovery otherwise it slows down the connection.
        adapter.cancelDiscovery();

        // Start the thread to connect with the given device
        mConnectionThread = new ConnectionThread(this, device, uuid);
        mConnectionThread.start();

        return BluetoothStatus.IN_PROGRESS;
    }

    /**
     * <p>This method is called when the BluetoothSocket has successfully connected to a
     * Bluetooth device.</p>
     * <p>This method cancels the Connection and Communicator Threads if there were any and creates
     * a new instance of the Communicator to send and listen for messages from the connected
     * device.</p>
     *
     * @param socket
     *         The socket successfully connected to a Bluetooth device.
     */
    private void onSocketConnected(BluetoothSocket socket) {
        Log.i(TAG,
              "[onSocketConnected] Successful connection to device: " + mLink.getBluetoothAddress());

        Logger.log(LOG_METHODS, TAG, "onSocketConnected");

        // Cancel the thread that completed the connection
        cancelConnectionThread();
        // Cancel any ongoing thread maintaining a communication
        cancelCommunicator();

        // the Bluetooth connection is now established

        // initialising the communication pipes
        mCommunicator = new Communicator(socket, this, mStreamAnalyser);
        mCommunicator.start();
    }

}
