/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core.rfcomm;

import androidx.annotation.VisibleForTesting;

/**
 * An enumeration to represent all the flags that can be contained in the flags filed of a
 * {@link Frame} as follows:
 * <blockquote><pre>
 * 7 bits  6       5       4       3       2                  1           0
 * +-------+-------+-------+-------+-------+------------------+-----------+
 * |               RESERVED                | LENGTH EXTENSION | CHECKSUM  |
 * +-------+-------+-------+-------+-------+------------------+-----------+
 * </pre></blockquote></p>
 */
public enum Flag {

    /**
     * The flag is raised when the frame contains a checksum.
     */
    CHECKSUM(0b1),
    /**
     * The flag is raised when the frame uses the length extension: two bytes to represent the
     * payload length.
     */
    LENGTH_EXTENSION(0b10);

    /**
     * The mask of the flag to know if the flag is raised.
     */
    private final int mask;

    Flag(int mask) {
        this.mask = mask;
    }

    @VisibleForTesting
    public int getMask() {
        return mask;
    }

    /**
     * @return True if the flag is raised in the given value, false otherwise.
     */
    boolean isRaised(int value) {
        return (value & mask) != 0;
    }

    /**
     * To get a value that contains all the given flags.
     * buildValue(CHECKSUM, LENGTH_EXTENSION) -> 0b11
     * buildValue(CHECKSUM) -> 0b01
     */
    static int buildValue(Flag... flags) {
        int result = 0;

        for (Flag flag : flags) {
            result += flag.mask;
        }

        return result;
    }

    /**
     * To build the value depending on the knowledge of the flags being raised.
     */
    static int buildValue(boolean hasChecksum, boolean hasLengthExtension) {
        if (hasChecksum && hasLengthExtension) {
            return buildValue(CHECKSUM, LENGTH_EXTENSION);
        }
        else if (hasChecksum) {
            return buildValue(CHECKSUM);
        }
        else if (hasLengthExtension) {
            return buildValue(LENGTH_EXTENSION);
        }
        else {
            return 0;
        }
    }
}
