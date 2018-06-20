package sonchain.blockchain.consensus;

public interface IInventory extends IVerifiable {

	byte[] getHash();
	InventoryType getInventoryType();
	boolean verify();
}
