package sonchain.blockchain.consensus;

import java.util.HashMap;
import java.util.Map;

public enum InventoryType {
	
	TX(0x01),
	Block(0x02),
	Consensus(0xe0);

    private int m_cmd;

    private static final Map<Integer, InventoryType> m_intToTypeMap = new HashMap<>();
    private static InventoryType[] m_versionToValuesMap = null;

    static {
    	m_versionToValuesMap = new InventoryType[]{
    			TX,
    			Block,
    			Consensus
        };

        for (InventoryType code : values()) {
        	m_intToTypeMap.put(code.m_cmd, code);
        }
    }

    private InventoryType(int cmd) {
    	m_cmd = cmd;
    }

    public static InventoryType[] values(int i) {
        return m_versionToValuesMap;
    }

    public static InventoryType fromByte(byte i) {
        return m_intToTypeMap.get((int) i);
    }

    public static boolean inRange(byte code) {
    	InventoryType[] codes = values(1);
        return code >= codes[0].asByte() && code <= codes[codes.length - 1].asByte();
    }

    public byte asByte() {
        return (byte) (m_cmd);
    }
}
