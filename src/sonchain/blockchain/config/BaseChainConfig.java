package sonchain.blockchain.config;

public interface BaseChainConfig {

    /**
     * Get the config for the specific block
     */
	BlockChainConfigInterface getConfigForBlock(long blockNumber);

    /**
     * Returns the constants common for all the blocks in this blockchain
     */
    Constants getCommonConstants();
}
