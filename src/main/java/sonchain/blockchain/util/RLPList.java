package sonchain.blockchain.util;

import java.util.ArrayList;

public class RLPList extends ArrayList<RLPElement> implements RLPElement {

    private byte[] m_rlpData = null;
    public void setRLPData(byte[] rlpData) {
    	m_rlpData = rlpData;
    }

    public byte[] getRLPData() {
        return m_rlpData;
    }

    public static void recursivePrint(RLPElement element) {

        if (element == null){
            throw new RuntimeException("RLPElement object can't be null");
        }
        if (element instanceof RLPList) {
            RLPList rlpList = (RLPList) element;
            System.out.print("[");
            for (RLPElement singleElement : rlpList){
                recursivePrint(singleElement);
            }
            System.out.print("]");
        } else {
            String hex = ByteUtil.toHexString(element.getRLPData());
            System.out.print(hex + ", ");
        }
    }
}
