package sonchain.blockchain.config;

import java.math.BigInteger;

/**
 * 常量定义
 * @author GAIA
 *
 */
public class Constants {

    private static final int BEST_NUMBER_DIFF_LIMIT = 100;
	private static final int MAXIMUM_EXTRA_DATA_SIZE = 32;
    private static final BigInteger SECP256K1N 
    		= new BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141", 16);

    /**
     * Introduced in the Homestead release
     */
    public boolean createEmptyContractOnOOG() {
        return true;
    }

    public int getBestNumberDiffLimit() {
        return BEST_NUMBER_DIFF_LIMIT;
    }

    public int getDurationLinit() {
        return 8;
    }

    public BigInteger getInitialNonce() {
        return BigInteger.ZERO;
    }

    public int getMaxContractSzie() { 
    	return Integer.MAX_VALUE; 
    }

    public int getMaximumExtraDataSize() {
        return MAXIMUM_EXTRA_DATA_SIZE;
    }

    /**
     * Introduced in the Homestead release
     */
    public static BigInteger getSECP256K1N() {
        return SECP256K1N;
    }

    /**
     * New DELEGATECALL opcode introduced in the Homestead release. Before Homestead this opcode should generate
     * exception
     */
    public boolean hasDelegateCallOpcode() {
    	return false; 
    }
}
