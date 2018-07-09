package sonchain.blockchain.consensus;

import java.util.HashMap;
import java.util.Map;

import sonchain.blockchain.net.SonMessageCodes;

public enum ConsensusState {
    Initial(0x00),
    Primary(0x01),
    Backup(0x02),
    RequestSent(0x04),
    RequestReceived(0x08),
    SignatureSent(0x10),
    BlockSent(0x20),
    ViewChanging(0x40);

    private int m_cmd;

    private static final Map<Integer, ConsensusState> m_intToTypeMap = new HashMap<>();
    private static ConsensusState[] m_versionToValuesMap = null;

    static {
    	m_versionToValuesMap = new ConsensusState[]{
    			Initial,
    			Primary,
    			Backup,
    			RequestSent,
    			RequestReceived,
    			SignatureSent,
    			BlockSent,
    			ViewChanging
        };

        for (ConsensusState code : values()) {
        	m_intToTypeMap.put(code.m_cmd, code);
        }
    }

    private ConsensusState(int cmd) {
    	m_cmd = cmd;
    }

    public static ConsensusState[] values(int i) {
        return m_versionToValuesMap;
    }

    public static ConsensusState fromByte(byte i) {
        return m_intToTypeMap.get((int) i);
    }

    public static boolean inRange(byte code) {
    	ConsensusState[] codes = values(1);
        return code >= codes[0].asByte() && code <= codes[codes.length - 1].asByte();
    }

    public byte asByte() {
        return (byte) (m_cmd);
    }
}
