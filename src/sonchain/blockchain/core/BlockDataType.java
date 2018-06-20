package sonchain.blockchain.core;

public enum BlockDataType {
	/**
	 * 头数据
	 */
	HeaderData,
	/**
	 * 板块数据
	 */
	BlockData;
	public int getValue()
	{
		return ordinal();
	}
  
	public static BlockDataType forValue(int value)
	{
		return values()[value];
	}
}
