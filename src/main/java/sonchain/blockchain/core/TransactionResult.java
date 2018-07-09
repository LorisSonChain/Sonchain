package sonchain.blockchain.core;

public class TransactionResult {

    private TransactionReceipt m_receipt = null;
    private TransactionExecutionSummary m_executionSummary = null;

    public boolean isIncluded() {
        return m_receipt != null;
    }

    public TransactionReceipt getReceipt() {
        return m_receipt;
    }

    public TransactionExecutionSummary getExecutionSummary() {
        return m_executionSummary;
    }
}
