package sonchain.blockchain.validator;

import java.util.*;

import org.apache.commons.lang3.tuple.Pair;

import sonchain.blockchain.config.BlockChainConfig;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.service.DataCenter;

public class BlockHashRule extends BlockHeaderRule {


    public BlockHashRule() {
    }

    @Override
    public ValidationResult validate(BlockHeader header) {
        List<Pair<Long, BlockHeaderValidator>> validators 
        	= DataCenter.m_config.getConfigForBlock(header.getNumber()).headerValidators();
        for (Pair<Long, BlockHeaderValidator> pair : validators) {
            if (header.getNumber() == pair.getLeft()) {
                ValidationResult result = pair.getRight().validate(header);
                if (!result.m_success) {
                    return fault("Block " + header.getNumber() + " header constraint violated. " + result.m_error);
                }
            }
        }

        return Success;
    }
}