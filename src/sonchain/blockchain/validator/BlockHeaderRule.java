package sonchain.blockchain.validator;

import sonchain.blockchain.core.BlockHeader;

/**
 * 区块头规则
 * @author GAIA
 *
 */
public abstract class BlockHeaderRule extends AbstractValidationRule {
	
	@Override
    public Class getEntityClass() {
        return BlockHeader.class;
    }

    /**
     * 验证区块头并返回结果
     *
     * @param 区块头
     */
    abstract public ValidationResult validate(BlockHeader header);

    /**
     * Fault
     * @param error
     * @return
     */
    protected ValidationResult fault(String error) {
        return new ValidationResult(false, error);
    }

    /**
     * 验证成功的结果类
     */
    public static final ValidationResult Success = new ValidationResult(true, null);

    /**
     * 验证并记录日志
     * @param header
     * @param logger
     * @return
     */
    public boolean validateAndLog(BlockHeader header) {
        ValidationResult result = validate(header);
        //if (!result.m_success && logger.isErrorEnabled()) {
        if (!result.m_success) {
            //logger.warn("{} invalid {}", GetEntityClass(), result.m_error);
        }
        return result.m_success;
    }

    /**
     * 验证结果定义类
     */
    public static final class ValidationResult {

        public final boolean m_success;
        public final String m_error;

        public ValidationResult(boolean success, String error) {
            this.m_success = success;
            this.m_error = error;
        }
    }
}
