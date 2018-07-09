package sonchain.blockchain.crypto;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Arrays;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;

import sonchain.blockchain.crypto.ECKey.ECDSASignature;
import sonchain.blockchain.util.Numeric;

/**
 *
 */
public class ECKeyPair {
	/**
	 */
	private final BigInteger m_privateKey;
	/**
	 * publicKey
	 */
    private final BigInteger m_publicKey;

    /**
     * ECKeyPair
     * @param privateKey
     * @param publicKey
     */
    public ECKeyPair(BigInteger privateKey, BigInteger publicKey) {
        this.m_privateKey = privateKey;
        this.m_publicKey = publicKey;
    }

    public BigInteger GetPrivateKey() {
        return m_privateKey;
    }
    
    public BigInteger GetPublicKey() {
        return m_publicKey;
    }

    
    public static ECKeyPair Create(KeyPair keyPair) {
        BCECPrivateKey privateKey = (BCECPrivateKey) keyPair.getPrivate();
        BCECPublicKey publicKey = (BCECPublicKey) keyPair.getPublic();
        BigInteger privateKeyValue = privateKey.getD();
        // Ethereum does not use encoded public keys like bitcoin - see
        // https://en.bitcoin.it/wiki/Elliptic_Curve_Digital_Signature_Algorithm for details
        // Additionally, as the first bit is a constant prefix (0x04) we ignore this value
        byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);
        BigInteger publicKeyValue =
                new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));
        return new ECKeyPair(privateKeyValue, publicKeyValue);
    }

    public static ECKeyPair Create(BigInteger privateKey) {
        return new ECKeyPair(privateKey, Sign.publicKeyFromPrivate(privateKey));
    }

    public static ECKeyPair Create(byte[] privateKey) {
        return Create(Numeric.toBigInt(privateKey));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ECKeyPair ecKeyPair = (ECKeyPair) o;

        if (m_privateKey != null
                ? !m_privateKey.equals(ecKeyPair.m_privateKey) : ecKeyPair.m_privateKey != null) {
            return false;
        }

        return m_publicKey != null
                ? m_publicKey.equals(ecKeyPair.m_publicKey) : ecKeyPair.m_publicKey == null;
    }
    
    @Override
    public int hashCode() {
        int result = m_privateKey != null ? m_privateKey.hashCode() : 0;
        result = 31 * result + (m_publicKey != null ? m_publicKey.hashCode() : 0);
        return result;
    }

    /**
     * Sign a hash with the private key of this key pair.
     * @param transactionHash   the hash to sign
     * @return  An {@link ECDSASignature} of the hash
     */
    public ECDSASignature Sign(byte[] transactionHash) {
        ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(m_privateKey, ECKey.CURVE);
        signer.init(true, privKey);
        BigInteger[] components = signer.generateSignature(transactionHash);
        return new ECDSASignature(components[0], components[1]).toCanonicalised();
    }
}
