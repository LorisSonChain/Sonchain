package sonchain.blockchain.contract;

/**
 * Represents the contract storage which is effectively the
 * mapping( uint256 => uint256 )
 *
 */
public interface SonContractStorage {
    byte[] getStorageSlot(long slot);
    byte[] getStorageSlot(byte[] slot);
}
