package sonchain.blockchain.consensus;

import java.util.HashMap;
import java.util.Map;

public enum ConsensusMessageType {
	ChangeView(0),
	PrepareRequest(32),
	PrepareResponse(33);

    private int m_cmd;

    private static final Map<Integer, ConsensusMessageType> m_intToTypeMap = new HashMap<>();
    private static ConsensusMessageType[] m_versionToValuesMap = null;

    static {
    	m_versionToValuesMap = new ConsensusMessageType[]{
    			ChangeView,
    			PrepareRequest,
    			PrepareResponse
        };

        for (ConsensusMessageType code : values()) {
        	m_intToTypeMap.put(code.m_cmd, code);
        }
    }

    private ConsensusMessageType(int cmd) {
    	m_cmd = cmd;
    }

    public static ConsensusMessageType[] values(int i) {
        return m_versionToValuesMap;
    }

    public static ConsensusMessageType fromByte(byte i) {
        return m_intToTypeMap.get((int) i);
    }

    public static boolean inRange(byte code) {
    	ConsensusMessageType[] codes = values(1);
        return code >= codes[0].asByte() && code <= codes[codes.length - 1].asByte();
    }

    public byte asByte() {
        return (byte) (m_cmd);
    }
}
