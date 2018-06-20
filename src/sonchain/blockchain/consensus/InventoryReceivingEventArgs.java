package sonchain.blockchain.consensus;

import sonchain.blockchain.core.Transaction;

public class InventoryReceivingEventArgs extends CancelEventArgs{
	private IInventory m_inventory = null;
	private Transaction m_transaction = null;
	
	public IInventory getInventory(){
		return m_inventory;
	}
	
	public Transaction getTransaction(){
		return m_transaction;
	}
	
	public InventoryReceivingEventArgs(IInventory inventory, Transaction transaction){
		m_inventory = inventory;
		m_transaction = transaction;
	}
}
