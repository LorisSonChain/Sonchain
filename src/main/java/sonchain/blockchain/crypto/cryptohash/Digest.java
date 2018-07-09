package sonchain.blockchain.crypto.cryptohash;

/**
 * 摘要算法
 * @author GAIA
 *
 */
public interface Digest {
	/**
	 * 更新一个字节
	 * @param in
	 */
	void Update(byte in);
	
	/**
	 * 更新多个字节
	 * @param inbuf
	 */
	void Update(byte[] inbuf);
	
	/**
	 * 更新多个字节
	 * @param inbuf 数组缓存
	 * @param off   偏移量
	 * @param len   长度
	 */
	void Update(byte[] inbuf, int off, int len);

	/**
	 * 计算摘要算法值
	 *
	 * @return  the hash output
	 */
	byte[] Digest();

	/**
	 * 计算输入字节流的摘要算法值
	 * @param inbuf 输入的字节流
	 * @return Hash值
	 */
	byte[] Digest(byte[] inbuf);

	/**
	 * 计算当前哈希计算并存储摘要算法
	 * @param outbuf 输出的字节流
	 * @param off 偏移量
	 * @param len 长度
	 * @return
	 */
	int Digest(byte[] outbuf, int off, int len);

	/**
	 * 获取摘要算法函数输出的长度
	 *
	 * @return  Hash函数输出的长度
	 */
	int GetDigestLength();

	/**
	 * 重新计算摘要算法值
	 */
	void Reset();

	/**
	 * 拷贝当前的数据
	 *
	 * @return 
	 */
	Digest Copy();

	/**
	 * 获取区块的长度
	 */
	int GetBlockLength();

	/**
	 * 获取摘要算法函数的名称
	 *
	 * @see Object
	 */
	String ToString();

}
