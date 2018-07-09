package sonchain.blockchain.net;

import java.util.*;

/**
 * A list of commands for the SonChain network protocol.
 * <br>
 * The codes for these commands are the first byte in every packet.
 *
 */
public enum SonMessageCodes {
	  /* SonChain protocol */

    STATUS(0x00),
    NEW_BLOCK_HASHES(0x01),
    TRANSACTIONS(0x02),
    GET_BLOCK_HEADERS(0x03),
    BLOCK_HEADERS(0x04),
    GET_BLOCK_BODIES(0x05),
    BLOCK_BODIES(0x06),
    NEW_BLOCK(0x07),
    GET_NODE_DATA(0x0d),
    NODE_DATA(0x0e),
    GET_RECEIPTS(0x0f),
    RECEIPTS(0x10),

	ChangeView(0x20),
	PrepareRequest(0x21),
	PrepareResponse(0x22);

    private int m_cmd;

    private static final Map<Integer, SonMessageCodes> m_intToTypeMap = new HashMap<>();
    private static SonMessageCodes[] m_versionToValuesMap = null;

    static {
    	m_versionToValuesMap = new SonMessageCodes[]{
                STATUS,
                NEW_BLOCK_HASHES,
                TRANSACTIONS,
                GET_BLOCK_HEADERS,
                BLOCK_HEADERS,
                GET_BLOCK_BODIES,
                BLOCK_BODIES,
                NEW_BLOCK,
                GET_NODE_DATA,
                NODE_DATA,
                GET_RECEIPTS,
                RECEIPTS,
                ChangeView,
                PrepareRequest,
                PrepareResponse
        };

        for (SonMessageCodes code : values()) {
        	m_intToTypeMap.put(code.m_cmd, code);
        }
    }

    private SonMessageCodes(int cmd) {
    	m_cmd = cmd;
    }

    public static SonMessageCodes[] values(int i) {
        return m_versionToValuesMap;
    }

    public static SonMessageCodes fromByte(byte i) {
        return m_intToTypeMap.get((int) i);
    }

    public static boolean inRange(byte code) {
    	SonMessageCodes[] codes = values(1);
        return code >= codes[0].asByte() && code <= codes[codes.length - 1].asByte();
    }

    public byte asByte() {
        return (byte) (m_cmd);
    }
}
