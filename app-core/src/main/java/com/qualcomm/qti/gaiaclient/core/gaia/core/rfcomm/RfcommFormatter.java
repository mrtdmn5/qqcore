/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.setUINT8;

/**
 * Formats the content of a GAIA frame for RFCOMM with a frame header and footer.
 * It uses the format as described in {@link Format}.
 */
public class RfcommFormatter {

    public RfcommFormatter() {
    }

    /**
     * To format the given content with a GAIA frame header and footer.
     * This method generates all the header and footer fields depending on the given
     *
     * @param protocolVersion
     *         The protocol version as one of
     *         {@link com.qualcomm.qti.gaiaclient.core.gaia.core.version.ProtocolVersion}.
     * @param hasChecksum
     *         True to add a checksum to the packet.
     * @param content
     *         The content of the frame, requires to be at least 4 bytes long - see
     *         {@link Format#CONTENT_HEADER_LENGTH}.
     *
     * @return the bytes of the frame generated.
     */
    @NonNull
    public byte[] format(long protocolVersion, boolean hasChecksum, @NonNull byte[] content) {
        Pair<Integer, Boolean> determined = Format.getPayloadLength(protocolVersion,
                                                                    content.length);
        int length = determined.first; // cannot be null
        boolean hasLengthExtension = determined.second; // cannot be null

        int frameLength = Format.getFrameLength(length, hasChecksum, hasLengthExtension);

        byte[] result = new byte[frameLength];

        // add sof
        result[Format.OFFSET_SOF] = Format.SOF;

        // add version
        result[Format.OFFSET_VERSION] = (byte) protocolVersion;

        // add flags
        setUINT8(Flag.buildValue(hasChecksum, hasLengthExtension), result, Format.OFFSET_FLAGS);

        // add payload length field
        Format.addLength(length, result, hasLengthExtension);

        // add content
        int contentOffset = Format.OFFSET_CONTENT;
        if (hasLengthExtension) {
            contentOffset++;
        }
        System.arraycopy(content, 0, result, contentOffset, Format.getContentLength(length));

        // add checksum
        if (hasChecksum) {
            result[frameLength - 1] = calculateChecksum(result, frameLength - 1);
        }

        return result;
    }

    /**
     * Calculates the checksum of a byte array as an unsigned byte - from 0x00 to 0xFF.
     */
    private byte calculateChecksum(byte[] source, int length) {
        byte check = 0;
        for (int i = 0; i < length; i++) {
            check ^= source[i];
        }
        return check;
    }

}
