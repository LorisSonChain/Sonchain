package sonchain.blockchain.core;

public enum BlockStatus {
	/**
	 * 无
	 */
	None,
	/**
	 * 挖矿中
	 */
	Mining,
	/**
	 * 挖矿完成
	 */
	Mined,
	/**
	 * 等待确认
	 */
	WaitConfirm,
	/**
	 * 确认完成
	 */
	Confirmed;
	
	public int getValue()
	{
		return ordinal();
	}
  
	public static BlockStatus forValue(int value)
	{
		return values()[value];
	}

}
