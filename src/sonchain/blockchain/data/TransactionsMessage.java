package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import sonchain.blockchain.core.Transaction;
import sonchain.blockchain.net.SonMessageCodes;
import sonchain.blockchain.util.RLP;
import sonchain.blockchain.util.RLPList;

public class TransactionsMessage  extends BaseMessage{

    private List<Transaction> m_transactions = null;

    public TransactionsMessage(byte[] encoded) {
    	super(encoded);
    }

    public TransactionsMessage(Transaction transaction) {
    	m_transactions = new ArrayList<>();
    	m_transactions.add(transaction);
        m_parsed = true;
    }

    public TransactionsMessage(List<Transaction> transactionList) {
    	m_transactions = transactionList;
        m_parsed = true;
    }

    private synchronized void parse() {
        if (m_parsed) {
        	return;
        }
        RLPList paramsList = (RLPList) RLP.decode2(m_encoded).get(0);
        m_transactions = new ArrayList<>();
        for (int i = 0; i < paramsList.size(); ++i) {
            RLPList rlpTxData = (RLPList) paramsList.get(i);
            Transaction tx = new Transaction(rlpTxData.getRLPData());
            m_transactions.add(tx);
        }
        m_parsed = true;
    }

    private void encode() {
        List<byte[]> encodedElements = new ArrayList<>();
        for (Transaction tx : m_transactions)
            encodedElements.add(tx.getEncoded());
        byte[][] encodedElementArray = encodedElements.toArray(new byte[encodedElements.size()][]);
        m_encoded = RLP.encodeList(encodedElementArray);
    }

    public byte[] getEncoded() {
        if (m_encoded == null) {
        	encode();
        }
        return m_encoded;
    }


    public List<Transaction> getTransactions() {
        parse();
        return m_transactions;
    }

    public SonMessageCodes getCommand() {
        return SonMessageCodes.TRANSACTIONS;
    }
    
    @Override
    public String toString() {
        parse();
        final StringBuilder sb = new StringBuilder();
        if (m_transactions.size() < 4) {
            for (Transaction transaction : m_transactions){
                sb.append("\n   ").append(transaction.toString(128));
            }
        } else {
            for (int i = 0; i < 3; i++) {
                sb.append("\n   ").append(m_transactions.get(i).toString(128));
            }
            sb.append("\n   ").append("[Skipped ").append(m_transactions.size() - 3).append(" transactions]");
        }
        return "[" + getCommand().name() + " num:"
                + m_transactions.size() + " " + sb.toString() + "]";
    }
}
