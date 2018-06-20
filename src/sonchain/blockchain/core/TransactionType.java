package sonchain.blockchain.core;

public enum TransactionType{
	/**
	 * 空交易
	 */
	NullTransaction,
	/**
	 * 用于创建合约的交易， 接收的地址忽略
	 */
	ContractCreation,
	/**
	 * 用于消息调用的交易，接收地址正常使用
	 */
	MessageCall ;
	
	public int GetValue()
	{
		return ordinal();
	}
  
	public static TransactionType ForValue(int value)
	{
		return values()[value];
	}
}
