package sonchain.blockchain.validator;

import sonchain.blockchain.core.BlockHeader;

/**
 * @author GAIA
 *
 */
public abstract class BlockHeaderRule extends AbstractValidationRule {
	
	@Override
    public Class getEntityClass() {
        return BlockHeader.class;
    }

    abstract public ValidationResult validate(BlockHeader header);

    protected ValidationResult fault(String error) {
        return new ValidationResult(false, error);
    }

    public static final ValidationResult Success = new ValidationResult(true, null);

    public boolean validateAndLog(BlockHeader header) {
        ValidationResult result = validate(header);
        //if (!result.m_success && logger.isErrorEnabled()) {
        if (!result.m_success) {
            //logger.warn("{} invalid {}", GetEntityClass(), result.m_error);
        }
        return result.m_success;
    }
    
    public static final class ValidationResult {

        public final boolean m_success;
        public final String m_error;

        public ValidationResult(boolean success, String error) {
            this.m_success = success;
            this.m_error = error;
        }
    }
}
