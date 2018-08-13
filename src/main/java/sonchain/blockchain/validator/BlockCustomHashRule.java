package sonchain.blockchain.validator;

import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.util.FastByteComparisons;

public class BlockCustomHashRule extends BlockHeaderRule {

    public final byte[] m_blockHash;

    public BlockCustomHashRule(byte[] blockHash) {
    	m_blockHash = blockHash;
    }

    @Override
    public ValidationResult validate(BlockHeader header) {
        if (!FastByteComparisons.equal(header.getHash(), m_blockHash)) {
            return fault("Block " + header.getBlockNumber() + " hash constraint violated. Expected:" +
                    Hex.toHexString(m_blockHash) + ", got: " + Hex.toHexString(header.getHash()));
        }
        return Success;
    }
}
