package sonchain.blockchain.consensus;

import sonchain.blockchain.core.Transaction;

public class InventoryReceivingEventArgs extends CancelEventArgs{
	private ConsensusPayload m_inventory = null;
	private Transaction m_transaction = null;
	
	public ConsensusPayload getInventory(){
		return m_inventory;
	}
	
	public Transaction getTransaction(){
		return m_transaction;
	}
	
	public InventoryReceivingEventArgs(ConsensusPayload inventory, Transaction transaction){
		m_inventory = inventory;
		m_transaction = transaction;
	}
}
