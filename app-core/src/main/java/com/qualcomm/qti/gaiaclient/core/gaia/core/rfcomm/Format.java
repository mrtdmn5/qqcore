/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm;

import android.util.Log;

import com.qualcomm.qti.gaiaclient.core.gaia.core.version.ProtocolVersion;

import androidx.core.util.Pair;

import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.setUINT16;
import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.setUINT8;

/**
 * A helper class to contain the lengths and offsets values to identify the fields for a
 * {@link Frame}.
 */
public final class Format {

    /**
     * A tag to display when logging information from this class.
     */
    private static final String TAG = "Format";

    // VALUES
    /**
     * All GAIA frames over RFCOMM uses 0xFF to identify their start
     */
    static final byte SOF = (byte) 0xFF;

    // OFFSETS
    static final int OFFSET_SOF = 0;
    static final int OFFSET_VERSION = 1;
    static final int OFFSET_FLAGS = 2;
    static final int OFFSET_LENGTH = 3;
    static final int OFFSET_CONTENT = 4;

    // FIXED LENGTHS
    private static final int SOF_LENGTH = 1;
    private static final int VERSION_LENGTH = 1;
    private static final int FLAGS_LENGTH = 1;
    /**
     * The content of a GAIA frame over RFCOMM has always at least 4 bytes. This is used to
     * describe the content in a {@link com.qualcomm.qti.gaiaclient.core.gaia.core.GaiaPacket}.
     */
    public static final int CONTENT_HEADER_LENGTH = 4;
    private static final int CHECKSUM_LENGTH = 1;

    // VARIABLE LENGTHS: DEFAULT - used for protocols < 0x04
    private static final int DEFAULT_LENGTH_FIELD_LENGTH = 1;
    private static final int DEFAULT_FRAME_HEADER_LENGTH =                              /* 4 */
            SOF_LENGTH + VERSION_LENGTH + FLAGS_LENGTH + DEFAULT_LENGTH_FIELD_LENGTH;
    public static final int DEFAULT_PACKET_FIXED_LENGTH =                               /* 8 */
            DEFAULT_FRAME_HEADER_LENGTH + CONTENT_HEADER_LENGTH;
    /**
     * maximum value is 0xFF, requirement was to have an even number => 0xFE (254)
     */
    public static final int DEFAULT_PAYLOAD_MAX_LENGTH = 0xFE;                          /* 254 */
    private static final int DEFAULT_CONTENT_MAX_LENGTH =                               /* 258 */
            DEFAULT_PAYLOAD_MAX_LENGTH + CONTENT_HEADER_LENGTH;
    public static final int DEFAULT_PACKET_MAX_LENGTH =                                 /* 263 */
            DEFAULT_FRAME_HEADER_LENGTH + DEFAULT_CONTENT_MAX_LENGTH + CHECKSUM_LENGTH;

    // VARIABLE LENGTHS: LENGTH EXTENSION - used in combination with DEFAULT for protocols >= 0x04
    public static final int LENGTH_FIELD_WITH_EXTENSION_LENGTH = 2;
    private static final int FRAME_HEADER_WITH_EXTENSION_MAX_LENGTH =                   /* 5 */
            SOF_LENGTH + VERSION_LENGTH + FLAGS_LENGTH + LENGTH_FIELD_WITH_EXTENSION_LENGTH;
    public static final int PAYLOAD_WITH_EXTENSION_MAX_LENGTH = 0xFFFF;                 /* 65535 */
    static final int CONTENT_WITH_EXTENSION_MAX_LENGTH =                                /* 65539 */
            PAYLOAD_WITH_EXTENSION_MAX_LENGTH + CONTENT_HEADER_LENGTH;
    public static final int PACKET_WITH_EXTENSION_MAX_LENGTH =                          /* 65544 */
            FRAME_HEADER_WITH_EXTENSION_MAX_LENGTH + CONTENT_WITH_EXTENSION_MAX_LENGTH + CHECKSUM_LENGTH;                                      // checksum


    // LENGTHS & OFFSETS GETTERS

    /**
     * To get the length of the frame using the flags field.
     */
    static int getFrameLength(int payloadLength, int flags) {
        return DEFAULT_PACKET_FIXED_LENGTH + payloadLength
                + (Flag.CHECKSUM.isRaised(flags) ? 1 : 0)
                + (Flag.LENGTH_EXTENSION.isRaised(flags) ? 1 : 0);
    }

    /**
     * To get the length of the frame using the knowledge of the presence of the different fields.
     */
    static int getFrameLength(int payloadLength, boolean hasChecksum,
                              boolean hasLengthExtension) {
        return DEFAULT_PACKET_FIXED_LENGTH + payloadLength
                + (hasChecksum ? 1 : 0)
                + (hasLengthExtension ? 1 : 0);
    }

    /**
     * <p>To add the length to its field depending on the presence of the length extension.</p>
     *
     * @param length
     *         the value of the length field.
     * @param result
     *         the array to add the field to.
     * @param hasLengthExtension
     *         True if the result uses the length extension, false otherwise.
     */
    static void addLength(int length, byte[] result, boolean hasLengthExtension) {
        if (hasLengthExtension) {
            setUINT16(length, result, OFFSET_LENGTH);
        }
        else {
            setUINT8(length, result, OFFSET_LENGTH);
        }
    }

    /**
     * To get the offset the checksum is within the frame.
     */
    static int getChecksumOffset(int payloadLength, int flags) {
        return DEFAULT_PACKET_FIXED_LENGTH + payloadLength
                + (Flag.LENGTH_EXTENSION.isRaised(flags) ? 1 : 0);
    }

    /**
     * To get the offset that matches the frame offset with the content field.
     * frame = {0xff, 0x01, 0x00, 0x04, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77}
     * offset to get is for "0x66"
     * getContentOffset(10, 0x00) -> 6
     */
    static int getContentOffset(int frameOffset, boolean hasLengthExtension) {
        return frameOffset
                - Format.DEFAULT_FRAME_HEADER_LENGTH
                - (hasLengthExtension ? 1 : 0);
    }

    static int getContentLength(int payloadLength) {
        return CONTENT_HEADER_LENGTH + payloadLength;
    }

    /**
     * To get the value of the payload length field from the version and the content length.
     * The payload length is limited by the following:
     * - protocol <= 0x03: maximum length is 0xFE
     * - protocol >= 0x04: require length extension for 0xFF < length <= 0xFFFF
     *
     * @return The calculated payload length paired with a boolean to know if the length
     * extension is required - true if required, false otherwise.
     */
    static Pair<Integer, Boolean> getPayloadLength(long version, int contentLength) {
        // minimum content length is the content header
        if (contentLength < CONTENT_HEADER_LENGTH) {
            Log.w(TAG, String.format("[getPayloadLength] content length (%1$d) " +
                                             "is smaller than required (%2$d).", contentLength,
                                     CONTENT_HEADER_LENGTH));
            return new Pair<>(0, false);
        }

        int payloadLength = contentLength - CONTENT_HEADER_LENGTH;

        if (payloadLength <= DEFAULT_PAYLOAD_MAX_LENGTH) {
            return new Pair<>(payloadLength, false);
        }

        if (version <= ProtocolVersion.V3) {
            Log.w(TAG, String.format("[getPayloadLength] version (%1$d) does not support length " +
                                             "extension", version));
            return new Pair<>(DEFAULT_PAYLOAD_MAX_LENGTH, false);
        }

        if (payloadLength == DEFAULT_PAYLOAD_MAX_LENGTH + 1) {
            // 0xFF does not require the length extension
            return new Pair<>(payloadLength, false);
        }

        // data extension necessary

        if (payloadLength > PAYLOAD_WITH_EXTENSION_MAX_LENGTH) {
            Log.w(TAG, String.format("[getPayloadLength] payload length (%1$d) is bigger than " +
                                             "maximum length (%2$d)", payloadLength,
                                     PAYLOAD_WITH_EXTENSION_MAX_LENGTH));
            return new Pair<>(PAYLOAD_WITH_EXTENSION_MAX_LENGTH, true);
        }

        return new Pair<>(payloadLength, true);
    }
}
