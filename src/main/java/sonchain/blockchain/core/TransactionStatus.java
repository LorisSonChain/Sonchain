package sonchain.blockchain.core;

public enum TransactionStatus {
	Executed(0), ///< succeed, no error handler executed
	SoftFail(1), ///< objectively failed (not executed), error handler executed
	HardFail(2), ///< objectively failed and error handler objectively failed thus no state change
	Delayed(3), ///< transaction delayed/deferred/scheduled for future execution
	Expired(4);  ///< transaction expired and storage space refuned to user
	
    private int m_value = 0;

    private TransactionStatus(int value) {
        this.m_value = value;
    }

    public int Value() {
        return m_value;
    }

    @Override
    public String toString() {
    	if(m_value == 0){
    		return "Executed";
    	}
    	else if(m_value == 1){
    		return "SoftFail";
    	}
    	else if(m_value == 2){
    		return "HardFail";
    	}
    	else if(m_value == 3){
    		return "Delayed";
    	}
    	else if(m_value == 4){
    		return "Expired";
    	}
    	return "";
    }
    
    public static TransactionStatus fromString(String str){
    	if(str.equals("Executed")){
    		return TransactionStatus.Executed;
    	}else if(str.equals("SoftFail")){
    		return TransactionStatus.SoftFail;
    	}else if(str.equals("HardFail")){
    		return TransactionStatus.HardFail;
    	}else if(str.equals("Delayed")){
    		return TransactionStatus.Delayed;
    	}else if(str.equals("Expired")){
    		return TransactionStatus.Expired;
    	}
		return TransactionStatus.Executed;
    }
}
