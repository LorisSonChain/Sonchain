package sonchain.blockchain.redis;

import sonchain.blockchain.service.DataCenter;
import lord.common.redis.RedisDataUtil;
import redis.clients.jedis.Jedis;

public class RedisSeq {

	/**
	 * Redis关键字 新的区块链
	 */
	private static String REDIS_KEY_BLOCKCHAIN_POOL = "BLOCKCHAIN_POOL";
	
	/**
	 * 获取区块链的区块
	 * @param preHashKey
	 * @return
	 */
	public static String GetBlockChain(String preHashKey)
	{
		Jedis jedis = null;
		try
		{
			jedis = DataCenter.GetJedisPool().getResource();
			return RedisDataUtil.Hget(jedis, REDIS_KEY_BLOCKCHAIN_POOL, preHashKey, false);
		}
		catch (Exception e)
		{
			return "";
		}
		finally
		{
			if (jedis != null)
			{
				jedis.close();
			}
		}
	}
	
	/**
	 * 设置区块链的区块
	 * @param preHashKey
	 * @param jsonBlock
	 * @return
	 */
	public static boolean SetBlockChain(String preHashKey, String jsonBlock)
	{
		Jedis jedis = null;
		try
		{
			jedis = DataCenter.GetJedisPool().getResource();
			RedisDataUtil.Hset(jedis, REDIS_KEY_BLOCKCHAIN_POOL, preHashKey, jsonBlock, false);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
		finally
		{
			if (jedis != null)
			{
				jedis.close();
			}
		}		
	}
}
