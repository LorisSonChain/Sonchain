package sonchain.blockchain.crypto.cryptohash;

import java.security.MessageDigest;

/**
 * 摘要算法引擎
 * 为应用程序提供信息摘要算法的功能，如 MD5 或 SHA 算法
 * @author GAIA
 *
 */
public abstract class DigestEngine extends MessageDigest implements Digest{

	/**
	 * 区块的数量
	 */
	private long m_blockCount;
	/**
	 * 区块的长度
	 */
	private int m_blockLen = 0;
	/**
	 * 摘要算法长度
	 */
	private int m_digestLen = 0;
	/**
	 * 输入的长度
	 */
	private int m_inputLen = 0;
	/**
	 * 输入的缓冲区
	 */
	private byte[] m_inputBuf = null;
	/**
	 * 输出缓冲区
	 */
	private byte[]  m_outputBuf;

	/**
	 * 初始化
	 * @param arg0
	 */
	protected DigestEngine(String alg) {
		super(alg);
		DoInit();
		m_digestLen = engineGetDigestLength();
		m_blockLen = GetInternalBlockLength();
		m_inputBuf = new byte[m_blockLen];
		m_outputBuf = new byte[m_digestLen];
		m_inputLen = 0;
		m_blockCount = 0;
	}

	/**
	 * 调整摘要算法函数的长度
	 */
	private void AdjustDigestLen()
	{
		if (m_digestLen == 0) {
			m_digestLen = engineGetDigestLength();
			m_outputBuf = new byte[m_digestLen];
		}
	}
	
	/**
	 * 拷贝摘要算法状态
	 * @param dest
	 * @return
	 */
	protected Digest CopyState(DigestEngine dest)
	{
		dest.m_inputLen = m_inputLen;
		dest.m_blockCount = m_blockCount;
		System.arraycopy(m_inputBuf, 0, dest.m_inputBuf, 0,
				m_inputBuf.length);
		AdjustDigestLen();
		dest.AdjustDigestLen();
		System.arraycopy(m_outputBuf, 0, dest.m_outputBuf, 0,
				m_outputBuf.length);
		return dest;
	}
	
	/**
	 * 计算并返回摘要算法的比特流
	 */
	public byte[] Digest()
	{
		AdjustDigestLen();
		byte[] result = new byte[m_digestLen];
		Digest(result, 0, m_digestLen);
		return result;
	}

	/**
	 * 计算输入字节流的摘要算法值并返回
	 */
	public byte[] Digest(byte[] input)
	{
		Update(input, 0, input.length);
		return Digest();
	}

	/**
	 * 计算当前哈希计算并存储哈希值
	 */
	public int Digest(byte[] buf, int offset, int len)
	{
		AdjustDigestLen();
		if (len >= m_digestLen) {
			DoPadding(buf, offset);
			reset();
			return m_digestLen;
		} else {
			DoPadding(m_outputBuf, 0);
			System.arraycopy(m_outputBuf, 0, buf, offset, len);
			reset();
			return len;
		}
	}
	
	/**
	 * 初始化
	 */
	protected abstract void DoInit();

	/**
	 * 执行最后的填充并将结果存储在提供的缓冲区
	 * @param buf 输出的字节流
	 * @param off 输出字节流的偏移量
	 */
	protected abstract void DoPadding(byte[] buf, int off);
	
	/**
	 * 重置摘要算法的状态
	 */
	protected abstract void EngineReset();
	
	/**
	 * 清除内部缓冲区
	 * @return 剩余未处理的字节数量
	 */
	protected final int Flush()
	{
		return m_inputLen;
	}

	/**
	 * 获取区块的缓存
	 * @return
	 */
	protected final byte[] GetBlockBuffer()
	{
		return m_inputBuf;
	}
	
	/**
	 * 获取区块的长度
	 * @return
	 */
	protected long GetBlockCount()
	{
		return m_blockCount;
	}
	
	/**
	 * 获取内部区块的长度
	 * @return
	 */
	protected int GetInternalBlockLength()
	{
		return GetBlockLength();
	}
	
	/**
	 * 处理区块数据
	 * @param data
	 */
	protected abstract void ProcessBlock(byte[] data);

	/**
	 * 重置
	 */
	public void Reset()
	{
		EngineReset();
		m_inputLen = 0;
		m_blockCount = 0;
	}
	
	/**
	 * 更新一个字节
	 */
	public void Update(byte input)
	{
		m_inputBuf[m_inputLen ++] = (byte)input;
		if (m_inputLen == m_blockLen) {
			ProcessBlock(m_inputBuf);
			m_blockCount ++;
			m_inputLen = 0;
		}
	}

	/**
	 * 更新多个字节
	 */
	public void Update(byte[] input)
	{
		Update(input, 0, input.length);
	}
	
	/**
	 * 更新多个字节
	 */
	public void Update(byte[] input, int offset, int len)
	{
		while (len > 0) {
			int copyLen = m_blockLen - m_inputLen;
			if (copyLen > len)
				copyLen = len;
			System.arraycopy(input, offset, m_inputBuf, m_inputLen,
				copyLen);
			offset += copyLen;
			m_inputLen += copyLen;
			len -= copyLen;
			if (m_inputLen == m_blockLen) {
				ProcessBlock(m_inputBuf);
				m_blockCount ++;
				m_inputLen = 0;
			}
		}
	}
}
