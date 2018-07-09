package sonchain.blockchain.core;

public enum TransactionType{
	NullTransaction,
	ContractCreation,
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
