package sonchain.blockchain.crypto;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.modes.SICBlockCipher;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import sonchain.blockchain.config.Constants;
import sonchain.blockchain.crypto.jce.ECKeyAgreement;
import sonchain.blockchain.crypto.jce.ECKeyFactory;
import sonchain.blockchain.crypto.jce.ECKeyPairGenerator;
import sonchain.blockchain.crypto.jce.ECSignatureFactory;
import sonchain.blockchain.crypto.jce.SpongyCastleProvider;
import sonchain.blockchain.util.ByteUtil;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.annotation.Nullable;
import javax.crypto.KeyAgreement;

import static sonchain.blockchain.util.BIUtil.isLessThan;
import static sonchain.blockchain.util.ByteUtil.bigIntegerToBytes;

/**
 *
 */
public class ECKey implements Serializable {

    public static final ECDomainParameters CURVE;
    public static final ECParameterSpec CURVE_SPEC;
    public static final BigInteger HALF_CURVE_ORDER;

    private static final SecureRandom m_secureRandom;
    private static final long serialVersionUID = -728224901792295832L;

    static {
        // All clients must agree on the curve to use by agreement. uses secp256k1.
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
        CURVE_SPEC = new ECParameterSpec(params.getCurve(), params.getG(), params.getN(), params.getH());
        HALF_CURVE_ORDER = params.getN().shiftRight(1);
        m_secureRandom = new SecureRandom();
    }

    private final PrivateKey m_privKey;
    protected final ECPoint m_pub;    
    private final Provider m_provider;

    // Transient because it's calculated on demand.
    transient private byte[] m_pubKeyHash;
    transient private byte[] m_nodeId;

    /**
     * Init
     *
     */
    public ECKey() {
        this(m_secureRandom);
    }
    
    private static ECPoint ExtractPublicKey(final ECPublicKey ecPublicKey) {
      final java.security.spec.ECPoint publicPointW = ecPublicKey.getW();
      final BigInteger xCoord = publicPointW.getAffineX();
      final BigInteger yCoord = publicPointW.getAffineY();

      return CURVE.getCurve().createPoint(xCoord, yCoord);
    }
    
    public ECKey(Provider provider, SecureRandom secureRandom) {
        this.m_provider = provider;

        final KeyPairGenerator keyPairGen = ECKeyPairGenerator.getInstance(provider, secureRandom);
        final KeyPair keyPair = keyPairGen.generateKeyPair();

        this.m_privKey = keyPair.getPrivate();

        final PublicKey pubKey = keyPair.getPublic();
        if (pubKey instanceof BCECPublicKey) {
        	m_pub = ((BCECPublicKey) pubKey).getQ();
        } else if (pubKey instanceof ECPublicKey) {
        	m_pub = ExtractPublicKey((ECPublicKey) pubKey);
        } else {
            throw new AssertionError(
                "Expected Provider " + provider.getName() +
                " to produce a subtype of ECPublicKey, found " + pubKey.getClass());
        }
    }

    public ECKey(SecureRandom secureRandom) {
        this(SpongyCastleProvider.getInstance(), secureRandom);
    }
    
    private static boolean isECPrivateKey(PrivateKey privKey) {
        return privKey instanceof ECPrivateKey || privKey.getAlgorithm().equals("EC");
    }

    public ECKey(Provider provider, @Nullable PrivateKey privKey, ECPoint pub) {
        this.m_provider = provider;

        if (privKey == null || isECPrivateKey(privKey)) {
            this.m_privKey = privKey;
        } else {
            throw new IllegalArgumentException(
                "Expected EC private key, given a private key object with class " +
                privKey.getClass().toString() +
                " and algorithm " + privKey.getAlgorithm());
        }

        if (pub == null) {
            throw new IllegalArgumentException("Public key may not be null");
        } else {
            this.m_pub = pub;
        }
    }
    
    private static PrivateKey privateKeyFromBigInteger(BigInteger priv) {
        if (priv == null) {
            return null;
        } else {
            try {
                return ECKeyFactory
                    .getInstance(SpongyCastleProvider.getInstance())
                    .generatePrivate(new ECPrivateKeySpec(priv, CURVE_SPEC));
            } catch (InvalidKeySpecException ex) {
                throw new AssertionError("Assumed correct key spec statically");
            }
        }
    }

    public ECKey(@Nullable BigInteger priv, ECPoint pub) {
        this(
            SpongyCastleProvider.getInstance(),
            privateKeyFromBigInteger(priv),
            pub
        );
    }

    public static ECPoint compressPoint(ECPoint uncompressed) {
        return CURVE.getCurve().decodePoint(uncompressed.getEncoded(true));
    }

    public static ECPoint decompressPoint(ECPoint compressed) {
        return CURVE.getCurve().decodePoint(compressed.getEncoded(false));
    }

    public static ECKey fromPrivate(BigInteger privKey) {
        return new ECKey(privKey, CURVE.getG().multiply(privKey));
    }

    public static ECKey fromPrivate(byte[] privKeyBytes) {
        return fromPrivate(new BigInteger(1, privKeyBytes));
    }

    public static ECKey fromPrivateAndPrecalculatedPublic(BigInteger priv, ECPoint pub) {
        return new ECKey(priv, pub);
    }

    public static ECKey fromPrivateAndPrecalculatedPublic(byte[] priv, byte[] pub) {
    	check(priv != null, "Private key must not be null");
    	check(pub != null, "Public key must not be null");
        return new ECKey(new BigInteger(1, priv), CURVE.getCurve().decodePoint(pub));
    }

    public static ECKey fromPublicOnly(ECPoint pub) {
        return new ECKey(null, pub);
    }

    public static ECKey fromPublicOnly(byte[] pub) {
        return new ECKey(null, CURVE.getCurve().decodePoint(pub));
    }

    public ECKey decompress() {
        if (!m_pub.isCompressed())
            return this;
        else
            return new ECKey(this.m_provider, this.m_privKey, decompressPoint(m_pub));
    }
    
    public ECKey compress() {
        if (m_pub.isCompressed())
            return this;
        else
            return new ECKey(this.m_provider, this.m_privKey, compressPoint(m_pub));
    }
    
    public boolean isPubKeyOnly() {
        return m_privKey == null;
    }
    
    public boolean hasPrivKey() {
        return m_privKey != null;
    }

    public static byte[] publicKeyFromPrivate(BigInteger privKey, boolean compressed) {
        ECPoint point = CURVE.getG().multiply(privKey);
        return point.getEncoded(compressed);
    }

    public static byte[] computeAddress(byte[] pubBytes) {
        return HashUtil.sha3omit12(
            Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
    }

    public static byte[] computeAddress(ECPoint pubPoint) {
        return computeAddress(pubPoint.getEncoded(/* uncompressed */ false));
    }

    public byte[] getAddress() {
        if (m_pubKeyHash == null) {
        	m_pubKeyHash = computeAddress(this.m_pub);
        }
        return m_pubKeyHash;
    }

    public static byte[] pubBytesWithoutFormat(ECPoint pubPoint) {
        final byte[] pubBytes = pubPoint.getEncoded(/* uncompressed */ false);
        return Arrays.copyOfRange(pubBytes, 1, pubBytes.length);
    }

    public byte[] getNodeId() {
        if (m_nodeId == null) {
        	m_nodeId  = pubBytesWithoutFormat(this.m_pub);
        }
        return m_nodeId;
    }
    
    public static ECKey fromNodeId(byte[] nodeId) {
    	check(nodeId.length == 64, "Expected a 64 byte node id");
        byte[] pubBytes = new byte[65];
        System.arraycopy(nodeId, 0, pubBytes, 1, nodeId.length);
        pubBytes[0] = 0x04; // uncompressed
        return ECKey.fromPublicOnly(pubBytes);
    }

    public byte[] getPubKey() {
        return m_pub.getEncoded(/* compressed */ false);
    }

    public ECPoint getPubKeyPoint() {
        return m_pub;
    }

    public BigInteger getPrivKey() {
        if (m_privKey == null) {
            throw new MissingPrivateKeyException();
        } else if (m_privKey instanceof BCECPrivateKey) {
            return ((BCECPrivateKey) m_privKey).getD();
        } else {
            throw new MissingPrivateKeyException();
        }
    }

    public boolean isCompressed() {
        return m_pub.isCompressed();
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("pub:").append(Hex.toHexString(m_pub.getEncoded(false)));
        return b.toString();
    }

    public String toStringWithPrivate() {
        StringBuilder b = new StringBuilder();
        b.append(toString());
        if (m_privKey != null && m_privKey instanceof BCECPrivateKey) {
            b.append(" priv:").append(Hex.toHexString(((BCECPrivateKey) m_privKey).getD().toByteArray()));
        }
        return b.toString();
    }
    
    public static class ECDSASignature {
        /**
         * The two components of the signature.
         */
        public final BigInteger r, s;
        public byte v;

        public ECDSASignature(BigInteger r, BigInteger s) {
            this.r = r;
            this.s = s;
        }

        /**
         *t
         * @param r
         * @param s
         * @return -
         */
        private static ECDSASignature fromComponents(byte[] r, byte[] s) {
            return new ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));
        }

        /**
         *
         * @param r -
         * @param s -
         * @param v -
         * @return -
         */
        public static ECDSASignature fromComponents(byte[] r, byte[] s, byte v) {
            ECDSASignature signature = fromComponents(r, s);
            signature.v = v;
            return signature;
        }

        public boolean validateComponents() {
            return validateComponents(r, s, v);
        }

        public static boolean validateComponents(BigInteger r, BigInteger s, byte v) {

            if (v != 27 && v != 28) {
            	return false;
            }

            if (isLessThan(r, BigInteger.ONE)) {
            	return false;
            }
            if (isLessThan(s, BigInteger.ONE)){
            	return false;
            }

            if (!isLessThan(r, Constants.getSECP256K1N())){
            	return false;
            }
            if (!isLessThan(s, Constants.getSECP256K1N())) {
            	return false;
            }
            return true;
        }

        public static ECDSASignature decodeFromDER(byte[] bytes) {
            ASN1InputStream decoder = null;
            try {
                decoder = new ASN1InputStream(bytes);
                DLSequence seq = (DLSequence) decoder.readObject();
                if (seq == null)
                    throw new RuntimeException("Reached past end of ASN.1 stream.");
                ASN1Integer r, s;
                try {
                    r = (ASN1Integer) seq.getObjectAt(0);
                    s = (ASN1Integer) seq.getObjectAt(1);
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException(e);
                }
                // OpenSSL deviates from the DER spec by interpreting these values as unsigned, though they should not be
                // Thus, we always use the positive versions. See: http://r6.ca/blog/20111119T211504Z.html
                return new ECDSASignature(r.getPositiveValue(), s.getPositiveValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (decoder != null)
                    try { decoder.close(); } catch (IOException x) {}
            }
        }
        
        public ECDSASignature toCanonicalised() {
            if (s.compareTo(HALF_CURVE_ORDER) > 0) {
                // The order of the curve is the number of valid points that exist on that curve. If S is in the upper
                // half of the number of valid points, then bring it back to the lower half. Otherwise, imagine that
                //    N = 10
                //    s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
                //    10 - 8 == 2, giving us always the latter solution, which is canonical.
                return new ECDSASignature(r, CURVE.getN().subtract(s));
            } else {
                return this;
            }
        }

        /**
         *
         * @return -
         */
        public String toBase64() {
            byte[] sigData = new byte[65];  // 1 header + 32 bytes for R + 32 bytes for S
            sigData[0] = v;
            System.arraycopy(bigIntegerToBytes(this.r, 32), 0, sigData, 1, 32);
            System.arraycopy(bigIntegerToBytes(this.s, 32), 0, sigData, 33, 32);
            return new String(Base64.encode(sigData), Charset.forName("UTF-8"));
        }

        public byte[] toByteArray() {
            final byte fixedV = this.v >= 27
                    ? (byte) (this.v - 27)
                    :this.v;

            return ByteUtil.merge(
                    ByteUtil.bigIntegerToBytes(this.r),
                    ByteUtil.bigIntegerToBytes(this.s),
                    new byte[]{fixedV});
        }

        public String toHex() {
            return Hex.toHexString(toByteArray());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ECDSASignature signature = (ECDSASignature) o;

            if (!r.equals(signature.r)) return false;
            if (!s.equals(signature.s)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = r.hashCode();
            result = 31 * result + s.hashCode();
            return result;
        }
    }

    public ECDSASignature doSign(byte[] input) {
        if (input.length != 32) {
            throw new IllegalArgumentException("Expected 32 byte input to ECDSA signature, not " + input.length);
        }
        // No decryption of private key required.
        if (m_privKey == null)
            throw new MissingPrivateKeyException();
        if (m_privKey instanceof BCECPrivateKey) {
            ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
            ECPrivateKeyParameters privKeyParams = new ECPrivateKeyParameters(((BCECPrivateKey) m_privKey).getD(), CURVE);
            signer.init(true, privKeyParams);
            BigInteger[] components = signer.generateSignature(input);
            return new ECDSASignature(components[0], components[1]).toCanonicalised();
        } else {
            try {
                final Signature ecSig = ECSignatureFactory.getRawInstance(m_provider);
                ecSig.initSign(m_privKey);
                ecSig.update(input);
                final byte[] derSignature = ecSig.sign();
                return ECDSASignature.decodeFromDER(derSignature).toCanonicalised();
            } catch (SignatureException | InvalidKeyException ex) {
                throw new RuntimeException("ECKey signing error", ex);
            }
        }
    }

    public ECDSASignature sign(byte[] messageHash) {
        ECDSASignature sig = doSign(messageHash);
        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        byte[] thisKey = this.m_pub.getEncoded(/* compressed */ false);
        for (int i = 0; i < 4; i++) {
            byte[] k = ECKey.recoverPubBytesFromSignature(i, sig, messageHash);
            if (k != null && Arrays.equals(k, thisKey)) {
                recId = i;
                break;
            }
        }
        if (recId == -1)
            throw new RuntimeException("Could not construct a recoverable key. This should never happen.");
        sig.v = (byte) (recId + 27);
        return sig;
    }
    
    public static byte[] signatureToKeyBytes(byte[] messageHash, String signatureBase64) throws SignatureException {
        byte[] signatureEncoded;
        try {
            signatureEncoded = Base64.decode(signatureBase64);
        } catch (RuntimeException e) {
            // This is what you get back from Bouncy Castle if base64 doesn't decode :(
            throw new SignatureException("Could not decode base64", e);
        }
        // Parse the signature bytes into r/s and the selector value.
        if (signatureEncoded.length < 65)
            throw new SignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.length);

        return signatureToKeyBytes(
            messageHash,
            ECDSASignature.fromComponents(
                Arrays.copyOfRange(signatureEncoded, 1, 33),
                Arrays.copyOfRange(signatureEncoded, 33, 65),
                (byte) (signatureEncoded[0] & 0xFF)));
    }

    public static byte[] signatureToKeyBytes(byte[] messageHash, ECDSASignature sig) throws SignatureException {
    	check(messageHash.length == 32, "messageHash argument has length " + messageHash.length);
        int header = sig.v;
        // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
        //                  0x1D = second key with even y, 0x1E = second key with odd y
        if (header < 27 || header > 34)
            throw new SignatureException("Header byte out of range: " + header);
        if (header >= 31) {
            header -= 4;
        }
        int recId = header - 27;
        byte[] key = ECKey.recoverPubBytesFromSignature(recId, sig, messageHash);
        if (key == null)
            throw new SignatureException("Could not recover public key from signature");
        return key;
    }
    
    public static byte[] signatureToAddress(byte[] messageHash, String signatureBase64) throws SignatureException {
        return computeAddress(signatureToKeyBytes(messageHash, signatureBase64));
    }
    
    public static byte[] signatureToAddress(byte[] messageHash, ECDSASignature sig) throws SignatureException {
        return computeAddress(signatureToKeyBytes(messageHash, sig));
    }
    public static ECKey signatureToKey(byte[] messageHash, String signatureBase64) throws SignatureException {
        final byte[] keyBytes = signatureToKeyBytes(messageHash, signatureBase64);
        return ECKey.fromPublicOnly(keyBytes);
    }
    
    public static ECKey signatureToKey(byte[] messageHash, ECDSASignature sig) throws SignatureException {
        final byte[] keyBytes = signatureToKeyBytes(messageHash, sig);
        return ECKey.fromPublicOnly(keyBytes);
    }

    public BigInteger keyAgreement(ECPoint otherParty) {
        if (m_privKey == null) {
            throw new MissingPrivateKeyException();
        } else if (m_privKey instanceof BCECPrivateKey) {
            final ECDHBasicAgreement agreement = new ECDHBasicAgreement();
            agreement.init(new ECPrivateKeyParameters(((BCECPrivateKey) m_privKey).getD(), CURVE));
            return agreement.calculateAgreement(new ECPublicKeyParameters(otherParty, CURVE));
        } else {
            try {
                final KeyAgreement agreement = ECKeyAgreement.getInstance(this.m_provider);
                agreement.init(this.m_privKey);
                agreement.doPhase(
                    ECKeyFactory.getInstance(this.m_provider)
                        .generatePublic(new ECPublicKeySpec(otherParty, CURVE_SPEC)),
                        /* lastPhase */ true);
                return new BigInteger(1, agreement.generateSecret());
            } catch (IllegalStateException | InvalidKeyException | InvalidKeySpecException ex) {
                throw new RuntimeException("ECDH key agreement failure", ex);
            }
        }
    }

    public static boolean verify(byte[] data, ECDSASignature signature, byte[] pub) {
        ECDSASigner signer = new ECDSASigner();
        ECPublicKeyParameters params = new ECPublicKeyParameters(CURVE.getCurve().decodePoint(pub), CURVE);
        signer.init(false, params);
        try {
            return signer.verifySignature(data, signature.r, signature.s);
        } catch (NullPointerException npe) {
            // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures.
            // Those signatures are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
            //logger.error("Caught NPE inside bouncy castle", npe);
            return false;
        }
    }

    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        return verify(data, ECDSASignature.decodeFromDER(signature), pub);
    }

    public boolean verify(byte[] data, byte[] signature) {
        return ECKey.verify(data, signature, getPubKey());
    }

    public boolean verify(byte[] sigHash, ECDSASignature signature) {
        return ECKey.verify(sigHash, signature, getPubKey());
    }

    public boolean isPubKeyCanonical() {
        return isPubKeyCanonical(m_pub.getEncoded(/* uncompressed */ false));
    }

    public static boolean isPubKeyCanonical(byte[] pubkey) {
        if (pubkey[0] == 0x04) {
            // Uncompressed pubkey
            if (pubkey.length != 65)
                return false;
        } else if (pubkey[0] == 0x02 || pubkey[0] == 0x03) {
            // Compressed pubkey
            if (pubkey.length != 33)
                return false;
        } else
            return false;
        return true;
    }
    
    @Nullable
    public static byte[] recoverPubBytesFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
    	check(recId >= 0, "recId must be positive");
    	check(sig.r.signum() >= 0, "r must be positive");
    	check(sig.s.signum() >= 0, "s must be positive");
    	check(messageHash != null, "messageHash must not be null");
        BigInteger n = CURVE.getN();  // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = sig.r.add(i.multiply(n));
        ECCurve.Fp curve = (ECCurve.Fp) CURVE.getCurve();
        BigInteger prime = curve.getQ();  // Bouncy Castle is not consistent about the letter it uses for the prime.
        if (x.compareTo(prime) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
            return null;
        }
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        if (!R.multiply(n).isInfinity())
            return null;
        BigInteger e = new BigInteger(1, messageHash);
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = sig.r.modInverse(n);
        BigInteger srInv = rInv.multiply(sig.s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint.Fp q = (ECPoint.Fp) ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);
        return q.getEncoded(/* compressed */ false);
    }

    @Nullable
    public static byte[] recoverAddressFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
        final byte[] pubBytes = recoverPubBytesFromSignature(recId, sig, messageHash);
        if (pubBytes == null) {
            return null;
        } else {
            return computeAddress(pubBytes);
        }
    }
    
    @Nullable
    public static ECKey recoverFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
        final byte[] pubBytes = recoverPubBytesFromSignature(recId, sig, messageHash);
        if (pubBytes == null) {
            return null;
        } else {
            return ECKey.fromPublicOnly(pubBytes);
        }
    }

    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }

    @Nullable
    public byte[] getPrivKeyBytes() {
        if (m_privKey == null) {
            return null;
        } else if (m_privKey instanceof BCECPrivateKey) {
            return bigIntegerToBytes(((BCECPrivateKey) m_privKey).getD(), 32);
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ECKey)) return false;

        ECKey ecKey = (ECKey) o;

        if (m_privKey != null && !m_privKey.equals(ecKey.m_privKey)) return false;
        if (m_pub != null && !m_pub.equals(ecKey.m_pub)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getPubKey());
    }

    @SuppressWarnings("serial")
    public static class MissingPrivateKeyException extends RuntimeException {
    }

    private static void check(boolean test, String message) {
        if (!test) throw new IllegalArgumentException(message);
    }
}