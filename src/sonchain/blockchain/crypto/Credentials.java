package sonchain.blockchain.crypto;

import sonchain.blockchain.util.Numeric;

/**
 * 证明
 * @author GAIA
 *
 */
public class Credentials {

	/**
	 * 初始化
	 * @param ecKeyPair
	 * @param address
	 */
    private Credentials(ECKeyPair ecKeyPair, String address) {
        this.m_ecKeyPair = ecKeyPair;
        this.m_address = address;
    }

    private final String m_address;
	private final ECKeyPair m_ecKeyPair;

    /**
     * 获取地址
     * @return
     */
    public String GetAddress() {
        return m_address;
    }

    /**
     * 获取Ec键值对
     * @return
     */
    public ECKeyPair GetEcKeyPair() {
        return m_ecKeyPair;
    }

    /**
     * 创建证明
     * @param ecKeyPair
     * @return
     */
    public static Credentials Create(ECKeyPair ecKeyPair) {
        String address = Numeric.prependHexPrefix(Keys.GetAddress(ecKeyPair));
        return new Credentials(ecKeyPair, address);
    }

    /**
     * 创建证明
     * @param privateKey
     * @param publicKey
     * @return
     */
    public static Credentials Create(String privateKey, String publicKey) {
        return Create(new ECKeyPair(Numeric.toBigInt(privateKey), Numeric.toBigInt(publicKey)));
    }

    /**
     * 创建证明
     * @param privateKey
     * @return
     */
    public static Credentials Create(String privateKey) {
        return Create(ECKeyPair.Create(Numeric.toBigInt(privateKey)));
    }

    /**
     * 判断是否相等
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Credentials that = (Credentials) o;
        if (m_ecKeyPair != null ? !m_ecKeyPair.equals(that.m_ecKeyPair) : that.m_ecKeyPair != null) {
            return false;
        }

        return m_address != null ? m_address.equals(that.m_address) : that.m_address == null;
    }

    /**
     * 获取hashCode
     */
    @Override
    public int hashCode() {
        int result = m_ecKeyPair != null ? m_ecKeyPair.hashCode() : 0;
        result = 31 * result + (m_address != null ? m_address.hashCode() : 0);
        return result;
    }
}
