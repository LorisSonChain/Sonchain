package sonchain.blockchain.validator;

import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.service.DataCenter;

public class ExtraDataRule extends BlockHeaderRule {

    private final int MAXIMUM_EXTRA_DATA_SIZE;

    public ExtraDataRule() {
        MAXIMUM_EXTRA_DATA_SIZE = DataCenter.m_config.
                getCommonConstants().getMaximumExtraDataSize();
    }

    @Override
    public ValidationResult validate(BlockHeader header) {
        if (header.getExtraData() != null && header.getExtraData().length > MAXIMUM_EXTRA_DATA_SIZE) {
            return fault(String.format(
                    "#%d: header.getExtraData().length > MAXIMUM_EXTRA_DATA_SIZE",
                    header.getNumber()
            ));
        }

        return Success;
    }
}

