/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm;

import android.util.Log;

import com.qualcomm.qti.gaiaclient.core.gaia.core.version.ProtocolVersion;

/**
 * <p>Default frame:<blockquote><pre>
 * 0 bytes   1         2         3         4  5  6  7  8          len+8          len+9
 * +---------+---------+---------+---------+--+--+--+--+-----------+ +------------+
 * |         |         |         | LENGTH  |        CONTENT        | |            |
 * |   SOF   | VERSION |  FLAGS  | OF      |-----------------------| |  CHECKSUM  |
 * |         |         |         | PAYLOAD |   HEADER  |  PAYLOAD  | | (optional) |
 * +---------+---------+---------+---------+--+--+--+--+-----------+ +------------+
 * </pre></blockquote></p>
 *
 * <p>Frame with Length Extension:<blockquote><pre>
 * bytes  3         4         5  6  7  8  9          len+9       len+10
 *        +---------+---------+--+--+--+--+-----------+ +----------+
 *   ...  |       LENGTH      |        CONTENT        | | CHECKSUM |
 *        +---------+---------+--+--+--+--+-----------+ +----------+
 * </pre></blockquote></p>
 */
class Frame {

    private static final String TAG = "Frame";

    private byte sof;
    private byte version;
    private byte flags;
    private int length;
    private final byte[] content = new byte[Format.CONTENT_WITH_EXTENSION_MAX_LENGTH];
    private byte checksum;

    /**
     * This class can be used without creating new objects by simpling calling this method once a
     * first frame has been used.
     */
    void reset() {
        sof = 0;
        version = 0x00;
        flags = 0;
        length = 0xFFFF;
        checksum = 0;
    }

    int getSof() {
        return sof & 0xFF;
    }

    void setSof(byte sof) {
        this.sof = sof;
    }

    long getVersion() {
        return version & 0xFFFF;
    }

    void setVersion(byte version) {
        this.version = version;
    }

    void setFlags(byte flags) {
        this.flags = flags;
    }

    /**
     * To know if the frame uses the length extension from protocol version 0x04.
     * This method also checks if the version is at least 0x04.
     */
    boolean hasLengthExtension() {
        return getVersion() >= ProtocolVersion.V4 && Flag.LENGTH_EXTENSION.isRaised(flags);
    }

    boolean hasChecksum() {
        return /* version >= 0x01 && */ Flag.CHECKSUM.isRaised(flags);
    }

    /**
     * @return the value of the length field.
     */
    int getLength() {
        return length;
    }

    /**
     * To set the value of the length field.
     */
    void setLength(int length) {
        this.length = length;
    }

    /**
     * To get the content of this frame.
     * This method generates the content array using the payload length of this frame and copies
     * the known content into the generated array.
     *
     * @return a copy of the content.
     */
    byte[] getContent() {
        int contentLength = Format.getContentLength(length);
        byte[] result = new byte[contentLength];
        System.arraycopy(content, 0, result, 0, contentLength);
        return result;
    }

    /**
     * To add the value that corresponds to the given offset on the frame to the content of the
     * frame.
     */
    void addContent(byte value, int frameOffset) {
        int offset = Format.getContentOffset(frameOffset, hasLengthExtension());
        if (offset < 0 || offset >= content.length) {
            Log.w(TAG, String.format("[addContent] offset (%1$d) is out of range (0, %2$d)",
                                     offset, content.length));
            return;
        }

        content[offset] = value;
    }

    int getChecksum() {
        return checksum & 0xFF;
    }

    void setChecksum(byte checksum) {
        this.checksum = checksum;
    }

    /**
     * To get the offset of the checksum within this frame.
     */
    int getChecksumOffset() {
        return Format.getChecksumOffset(length, flags);
    }

    /**
     * To get the total length of this frame.
     */
    int getFrameLength() {
        return Format.getFrameLength(length, flags);
    }

}
