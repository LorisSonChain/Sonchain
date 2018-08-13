package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import sonchain.blockchain.core.TransactionReceipt;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPElement;
import sonchain.blockchain.util.RLPList;

public class ReceiptsMessage  extends BaseMessage{
	public static final Logger m_logger = Logger.getLogger(ReceiptsMessage.class);
	
	private List<List<TransactionReceipt>> m_receipts;

    public ReceiptsMessage(byte[] encoded) {
        super(encoded);
    }

    public ReceiptsMessage(List<List<TransactionReceipt>> receiptList) {
    	m_receipts = receiptList;
        m_parsed = true;
    }

    private synchronized void parse() {
        if (m_parsed){
        	return;
        }
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
        m_receipts = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList blockRLP = (RLPList) paramsList.get(i);

            List<TransactionReceipt> blockReceipts = new ArrayList<>();
            for (RLPElement txReceipt : blockRLP) {
                RLPList receiptRLP = (RLPList) txReceipt;
                if (receiptRLP.size() != 4) {
                    continue;
                }
    	        //TODO
                //TransactionReceipt receipt = new TransactionReceipt(receiptRLP);
                //blockReceipts.add(receipt);
            }
            m_receipts.add(blockReceipts);
        }
        m_parsed = true;
    }

    private void encode() {
        List<byte[]> blocks = new ArrayList<>();

        for (List<TransactionReceipt> blockReceipts : m_receipts) {
            List<byte[]> encodedBlockReceipts = new ArrayList<>();
            for (TransactionReceipt txReceipt : blockReceipts) {
    	        //TODO
                //encodedBlockReceipts.add(txReceipt.getEncoded(true));
            }
            byte[][] encodedElementArray = encodedBlockReceipts.toArray(new byte[encodedBlockReceipts.size()][]);
            byte[] blockReceiptsEncoded = RLP.encodeList(encodedElementArray);
            blocks.add(blockReceiptsEncoded);
        }

        byte[][] encodedElementArray = blocks.toArray(new byte[blocks.size()][]);
        m_encoded = RLP.encodeList(encodedElementArray);
    }

	@Override
    public byte[] getEncoded() {
        if (m_encoded == null) {
        	encode();
        }
        return m_encoded;
    }


    public List<List<TransactionReceipt>> getReceipts() {
        parse();
        return m_receipts;
    }

    @Override
    public SonMessageCodes getCommand() {
        return SonMessageCodes.RECEIPTS;
    }

    public String toString() {
        parse();
        final StringBuilder sb = new StringBuilder();
        if (m_receipts.size() < 4) {
            for (List<TransactionReceipt> blockReceipts : m_receipts)
                sb.append("\n   ").append(blockReceipts.size()).append(" receipts in block");
        } else {
            for (int i = 0; i < 3; i++) {
                sb.append("\n   ").append(m_receipts.get(i).size()).append(" receipts in block");
            }
            sb.append("\n   ").append("[Skipped ").append(m_receipts.size() - 3).append(" blocks]");
        }
        return "[" + getCommand().name() + " num:"
                + m_receipts.size() + " " + sb.toString() + "]";
    }

}
