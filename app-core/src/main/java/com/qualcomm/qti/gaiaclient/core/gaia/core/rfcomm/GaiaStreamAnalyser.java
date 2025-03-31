/*
 * ************************************************************************************************
 * * © 2020, 2022 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.       *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm;

import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyser;
import com.qualcomm.qti.gaiaclient.core.bluetooth.analyser.StreamAnalyserListener;
import com.qualcomm.qti.gaiaclient.core.tasks.TaskManager;
import com.qualcomm.qti.gaiaclient.core.utils.DEBUG;
import com.qualcomm.qti.gaiaclient.core.utils.Logger;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.getUINT16;


/**
 * <p>This class analyses incoming data in order to identify a packet of type GAIA.</p>
 */
public class GaiaStreamAnalyser extends StreamAnalyser {

    /**
     * A tag to display logs within this class.
     */
    private static final String TAG = "GaiaStreamAnalyser";
    /**
     * Static debugging boolean to log all the methods calls.
     */
    private static final boolean LOG_METHODS = DEBUG.Gaia.GAIA_STREAM_ANALYSER;
    /**
     * When reading a frame this keeps track of the offset that is analysed.
     */
    private int mFrameOffset = 0;
    /**
     * The length field of the frame can be over multiple bytes: as a packet can be split between
     * streams, this keep track of the bytes to calculate the length once all received.
     */
    private final byte[] mLengthField = new byte[Format.LENGTH_FIELD_WITH_EXTENSION_LENGTH];
    /**
     * When fields of a frame are identified they are stored into this object.
     */
    private final Frame mFrame = new Frame();

    /**
     * <p>To build a new instance of this analyser.</p>
     */
    public GaiaStreamAnalyser(StreamAnalyserListener listener) {
        super(listener);
        mFrame.reset(); // unnecessary but done for consistency
    }

    /**
     * <p>To reset the data of the analyser: no current packet at the moment.</p>
     */
    @Override // StreamAnalyser
    public void reset() {
        Logger.log(LOG_METHODS, TAG, "reset");
        mFrame.reset();
        mFrameOffset = 0;
    }

    /**
     * <p>This method identifies a GAIA frame from a stream of bytes.</p>
     * <p>This method uses the data provided at each call to build a {@link Frame} following this
     * process:
     * <ol>
     * <li>Looks for the start of the packet known as "start of frame":
     * <code>{@link Format#SOF SOF} = 0xFF</code>.</li>
     * <li>Calculate the length of the frame using the bytes that follow SOF: flags
     * and length, through {@link #read(int, byte)}.</li>
     * <li>Copies each byte of the frame into the {@link Frame}, through
     * {@link #read(int, byte)}.</li>
     * <li>Calls {@link StreamAnalyserListener#onDataFound(byte[])} once the frame is
     * complete.</li>
     * </ol></p>
     *
     * @param taskManager
     *         A task manager to dispatch packets on another thread
     * @param stream
     *         The data to analyse in order to build GAIA packet(s).
     */
    @Override // StreamAnalyser
    public void analyse(@NonNull TaskManager taskManager, byte[] stream) {
        Logger.log(LOG_METHODS, TAG, "analyse", new Pair<>("stream", stream));
        int length = stream.length;

        // go through the received stream
        //noinspection ForLoopReplaceableByForEach  // more efficient to not use foreach here
        for (int i = 0; i < length; ++i) {
            byte value = stream[i];

            if (mFrameOffset == 0 && value != Format.SOF) {
                // is not analysing a frame and has not identified a new frame
                continue;
            }

            // read the value
            read(mFrameOffset, value);
            // number of received bytes can be incremented
            this.mFrameOffset++;

            // once the frame is completed, the content can be dispatched
            if (isFrameComplete()) {
                StreamAnalyserListener listener = getListener();
                if (listener != null) {
                    byte[] data = mFrame.getContent();
                    // dispatch the packet on another thread to allow the reading of the next stream
                    taskManager.runInBackground(() -> listener.onDataFound(data));
                }
                reset();
            }
        }
    }

    /**
     * This method identifies the field of the frame this value belongs to and stores it into the
     * {@link Frame} dependently.
     */
    private void read(int frameOffset, byte value) {
        // get sof
        if (frameOffset == Format.OFFSET_SOF) {
            // start of a new frame
            mFrame.setSof(value);
        }

        // get version
        else if (frameOffset == Format.OFFSET_VERSION) { // = 1
            mFrame.setVersion(value); // uint8
        }

        // get flags
        else if (frameOffset == Format.OFFSET_FLAGS) { // = 2
            mFrame.setFlags(value);
        }

        // get length
        else if (frameOffset == Format.OFFSET_LENGTH) { // = 3
            if (!mFrame.hasLengthExtension()) {
                mFrame.setLength(value & 0xFF);
            }
            else {
                mLengthField[0] = value;
            }
        }
        else if (frameOffset == Format.OFFSET_LENGTH + 1     // = 4
                && mFrame.hasLengthExtension()) {
            mLengthField[1] = value;
            mFrame.setLength(getUINT16(mLengthField, 0));
        }

        // get checksum
        else if (mFrame.hasChecksum() && frameOffset == mFrame.getChecksumOffset()) {
            mFrame.setChecksum(value);
        }

        // add content
        else {
            mFrame.addContent(value, mFrameOffset);
        }
    }

    /**
     * Return true if all the bytes have a frame have been read.
     */
    private boolean isFrameComplete() {
        return this.mFrameOffset == mFrame.getFrameLength();
    }

}

