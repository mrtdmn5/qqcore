/*
 * ************************************************************************************************
 * * © 2020 Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.             *
 * ************************************************************************************************
 */

package com.qualcomm.qti.gaiaclient.core.gaia.core;

import androidx.annotation.NonNull;

import static com.qualcomm.qti.gaiaclient.core.utils.BytesUtils.setUINT16;
import static java.lang.System.arraycopy;

/**
 * <p>A GAIA packet is a message sent over a transport - most likely to be Bluetooth - that has
 * a format defined by the GAIA definition.</p>
 * <p>The format of the message can change depending on the transport - for instance RFCOMM VS
 * LE packets. This class encapsulates the common PDU for all transports and GAIA versions - up
 * to 3, higher versions being undefined it is not possible to state their compatibility.</p>
 * <p>The common PDU has the following format:
 * <blockquote><pre>
 * 0 bytes     1           2           3            4                      len+4
 * +-----------+-----------+-----------+-----------+ +-----------+-----------+
 * |       VENDOR ID       |        COMMAND        | | Optional PAYLOAD  ... |
 * +-----------+-----------+-----------+-----------+ +-----------+-----------+
 * </pre> </blockquote>
 * Where:
 * <ul>
 * <li><code>VENDOR ID</code>: is a unique ID that matches a vendor.</li>
 * <li><code>COMMAND</code>: is a unique value for a vendor. Commands can be defined as raw
 * values or can have a specific format - as defined for GAIA v3 for instance.</li>
 * <li><code>PAYLOAD</code>: Some optional data that corresponds to the command.</li>
 * </ul></p>
 * <p>This abstract class only manages the vendor of a packet. The command and payload are
 * dependant of the GAIA version. As a result this class is extended for:
 * <ul>
 * <li>v1/v2: {@link com.qualcomm.qti.gaiaclient.core.gaia.core.v1v2.packets.V1V2Packet}</li>
 * <li>v3: {@link com.qualcomm.qti.gaiaclient.core.gaia.core.v3.packets.V3Packet}</li>
 * </ul></p>
 * <p>This class manages the building of the bytes array of a packet through {@link #getBytes()}
 * by using the vendor ID provided in the constructor {@link #GaiaPacket(int)}, the command
 * provided with {@link #getCommandValue()} and the payload as given with {@link #getPayload()}.</p>
 *
 * <i>Note: PDU = Protocol Data Unit.</i>
 */
public abstract class GaiaPacket {

    // offsets and length to identify the fields of a GAIA packet
    public static final int VENDOR_OFFSET = 0;
    private static final int VENDOR_LENGTH = 2; // UIN16
    protected static final int COMMAND_DESCRIPTION_OFFSET = 2;
    private static final int COMMAND_DESCRIPTION_LENGTH = 2; // UIN16
    protected static final int PAYLOAD_OFFSET = 4;
    private static final int HEADER_LENGTH = VENDOR_LENGTH + COMMAND_DESCRIPTION_LENGTH;

    /**
     * The vendor ID of the packet.
     */
    private final int vendorId;


    // ====== CONSTRUCTORS ===================================================================

    /**
     * To build a new instance of this object that holds the vendorID.
     */
    public GaiaPacket(int vendorId) {
        this.vendorId = vendorId;
    }


    // ====== GETTERS ===================================================================

    /**
     * To get the vendor ID of the packet.
     */
    public final int getVendorId() {
        return vendorId;
    }

    /**
     * To get the bytes that represent this packet.
     * This method builds the bytes from its vendor and the information provided from
     * {@link #getCommandValue()} and {@link #getPayload()}.
     */
    @NonNull
    public final byte[] getBytes() {
        byte[] payload = getPayload();

        byte[] result = new byte[HEADER_LENGTH + payload.length];

        setUINT16(vendorId, result, VENDOR_OFFSET);
        setUINT16(getCommandValue(), result, COMMAND_DESCRIPTION_OFFSET);
        arraycopy(getPayload(), 0, result, PAYLOAD_OFFSET, payload.length);

        return result;
    }


    // ====== ABSTRACT METHODS ===================================================================

    /**
     * To get the raw 2 bytes value of the packet command.
     */
    protected abstract int getCommandValue();

    /**
     * To get the payload of the packet, this can be empty.
     */
    @NonNull
    public abstract byte[] getPayload();

    /**
     * To get a key to identify this packet as matching other packets in a command to
     * response/error relationship.
     */
    public abstract int getKey();

}
