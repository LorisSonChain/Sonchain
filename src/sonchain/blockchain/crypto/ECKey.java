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
 * ECC-Elliptic Curves Cryptography，椭圆曲线密码编码学，是目前已知的公钥体制中，对每比特所提供加密强度最高的一种体制
 * @author GAIA
 *
 */
public class ECKey implements Serializable {

    /**
     * secp256k1加密的参数.
     */
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
     * 初始化
     *
     */
    public ECKey() {
        this(m_secureRandom);
    }

    /* Convert a Java JCE ECPublicKey into a BouncyCastle ECPoint
     */
    private static ECPoint ExtractPublicKey(final ECPublicKey ecPublicKey) {
      final java.security.spec.ECPoint publicPointW = ecPublicKey.getW();
      final BigInteger xCoord = publicPointW.getAffineX();
      final BigInteger yCoord = publicPointW.getAffineY();

      return CURVE.getCurve().createPoint(xCoord, yCoord);
    }

    /**
     * Generate a new keypair using the given Java Security Provider.
     *
     * All private key operations will use the provider.
     */
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

    /**
     * Generates an entirely new keypair with the given {@link SecureRandom} object.
     *
     * BouncyCastle will be used as the Java Security Provider
     *
     * @param secureRandom -
     */
    public ECKey(SecureRandom secureRandom) {
        this(SpongyCastleProvider.getInstance(), secureRandom);
    }

    /* Test if a generic private key is an EC private key
     *
     * it is not sufficient to check that privKey is a subtype of ECPrivateKey
     * as the SunPKCS11 Provider will return a generic PrivateKey instance
     * a fallback that covers this case is to check the key algorithm
     */
    private static boolean isECPrivateKey(PrivateKey privKey) {
        return privKey instanceof ECPrivateKey || privKey.getAlgorithm().equals("EC");
    }

    /**
     * Pair a private key with a public EC point.
     *
     * All private key operations will use the provider.
     */
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

    /* Convert a BigInteger into a PrivateKey object
     */
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

    /**
     * Pair a private key integer with a public EC point
     *
     * BouncyCastle will be used as the Java Security Provider
     */
    public ECKey(@Nullable BigInteger priv, ECPoint pub) {
        this(
            SpongyCastleProvider.getInstance(),
            privateKeyFromBigInteger(priv),
            pub
        );
    }

    /**
     * Utility for compressing an elliptic curve point. Returns the same point if it's already compressed.
     * See the ECKey class docs for a discussion of point compression.
     *
     * @param uncompressed -
     *
     * @return -
     * @deprecated per-point compression property will be removed in Bouncy Castle
     */
    public static ECPoint compressPoint(ECPoint uncompressed) {
        return CURVE.getCurve().decodePoint(uncompressed.getEncoded(true));
    }

    /**
     * Utility for decompressing an elliptic curve point. Returns the same point if it's already compressed.
     * See the ECKey class docs for a discussion of point compression.
     *
     * @param compressed -
     *
     * @return  -
     * @deprecated per-point compression property will be removed in Bouncy Castle
     */
    public static ECPoint decompressPoint(ECPoint compressed) {
        return CURVE.getCurve().decodePoint(compressed.getEncoded(false));
    }

    /**
     * Creates an ECKey given the private key only.
     *
     * @param privKey -
     *
     *
     * @return  -
     */
    public static ECKey fromPrivate(BigInteger privKey) {
        return new ECKey(privKey, CURVE.getG().multiply(privKey));
    }

    /**
     * Creates an ECKey given the private key only.
     *
     * @param privKeyBytes -
     *
     * @return -
     */
    public static ECKey fromPrivate(byte[] privKeyBytes) {
        return fromPrivate(new BigInteger(1, privKeyBytes));
    }

    /**
     * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
     * generator point by the private key. This is used to speed things up when you know you have the right values
     * already. The compression state of pub will be preserved.
     *
     * @param priv -
     * @param pub -
     *
     * @return  -
     */
    public static ECKey fromPrivateAndPrecalculatedPublic(BigInteger priv, ECPoint pub) {
        return new ECKey(priv, pub);
    }

    /**
     * Creates an ECKey that simply trusts the caller to ensure that point is really the result of multiplying the
     * generator point by the private key. This is used to speed things up when you know you have the right values
     * already. The compression state of the point will be preserved.
     *
     * @param priv -
     * @param pub -
     * @return -
     */
    public static ECKey fromPrivateAndPrecalculatedPublic(byte[] priv, byte[] pub) {
    	check(priv != null, "Private key must not be null");
    	check(pub != null, "Public key must not be null");
        return new ECKey(new BigInteger(1, priv), CURVE.getCurve().decodePoint(pub));
    }

    /**
     * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given point. The
     * compression state of pub will be preserved.
     *
     * @param pub -
     * @return -
     */
    public static ECKey fromPublicOnly(ECPoint pub) {
        return new ECKey(null, pub);
    }

    /**
     * Creates an ECKey that cannot be used for signing, only verifying signatures, from the given encoded point.
     * The compression state of pub will be preserved.
     *
     * @param pub -
     * @return -
     */
    public static ECKey fromPublicOnly(byte[] pub) {
        return new ECKey(null, CURVE.getCurve().decodePoint(pub));
    }

    /**
     * Returns a copy of this key, but with the public point represented in uncompressed form. Normally you would
     * never need this: it's for specialised scenarios or when backwards compatibility in encoded form is necessary.
     *
     * @return  -
     * @deprecated per-point compression property will be removed in Bouncy Castle
     */
    public ECKey decompress() {
        if (!m_pub.isCompressed())
            return this;
        else
            return new ECKey(this.m_provider, this.m_privKey, decompressPoint(m_pub));
    }

    /**
     * @deprecated per-point compression property will be removed in Bouncy Castle
     */
    public ECKey compress() {
        if (m_pub.isCompressed())
            return this;
        else
            return new ECKey(this.m_provider, this.m_privKey, compressPoint(m_pub));
    }

    /**
     * Returns true if this key doesn't have access to private key bytes. This may be because it was never
     * given any private key bytes to begin with (a watching key).
     *
     * @return -
     */
    public boolean isPubKeyOnly() {
        return m_privKey == null;
    }

    /**
     * Returns true if this key has access to private key bytes. Does the opposite of
     * {@link #isPubKeyOnly()}.
     *
     * @return  -
     */
    public boolean hasPrivKey() {
        return m_privKey != null;
    }

    /**
     * Returns public key bytes from the given private key. To convert a byte array into a BigInteger, use <tt>
     * new BigInteger(1, bytes);</tt>
     *
     * @param privKey -
     * @param compressed -
     * @return -
     */
    public static byte[] publicKeyFromPrivate(BigInteger privKey, boolean compressed) {
        ECPoint point = CURVE.getG().multiply(privKey);
        return point.getEncoded(compressed);
    }

    /**
     * Compute an address from an encoded public key.
     *
     * @param pubBytes an encoded (uncompressed) public key
     * @return 20-byte address
     */
    public static byte[] computeAddress(byte[] pubBytes) {
        return HashUtil.sha3omit12(
            Arrays.copyOfRange(pubBytes, 1, pubBytes.length));
    }

    /**
     * Compute an address from a public point.
     *
     * @param pubPoint a public point
     * @return 20-byte address
     */
    public static byte[] computeAddress(ECPoint pubPoint) {
        return computeAddress(pubPoint.getEncoded(/* uncompressed */ false));
    }

    /**
     * Gets the address form of the public key.
     *
     * @return 20-byte address
     */
    public byte[] getAddress() {
        if (m_pubKeyHash == null) {
        	m_pubKeyHash = computeAddress(this.m_pub);
        }
        return m_pubKeyHash;
    }

    /**
     * Compute the encoded X, Y coordinates of a public point.
     *
     * This is the encoded public key without the leading byte.
     *
     * @param pubPoint a public point
     * @return 64-byte X,Y point pair
     */
    public static byte[] pubBytesWithoutFormat(ECPoint pubPoint) {
        final byte[] pubBytes = pubPoint.getEncoded(/* uncompressed */ false);
        return Arrays.copyOfRange(pubBytes, 1, pubBytes.length);
    }

    /**
     * Generates the NodeID based on this key, that is the public key without first format byte
     */
    public byte[] getNodeId() {
        if (m_nodeId == null) {
        	m_nodeId  = pubBytesWithoutFormat(this.m_pub);
        }
        return m_nodeId;
    }

    /**
     * Recover the public key from an encoded node id.
     *
     * @param nodeId a 64-byte X,Y point pair
     */
    public static ECKey fromNodeId(byte[] nodeId) {
    	check(nodeId.length == 64, "Expected a 64 byte node id");
        byte[] pubBytes = new byte[65];
        System.arraycopy(nodeId, 0, pubBytes, 1, nodeId.length);
        pubBytes[0] = 0x04; // uncompressed
        return ECKey.fromPublicOnly(pubBytes);
    }

    /**
     * Gets the encoded public key value.
     *
     * @return 65-byte encoded public key
     */
    public byte[] getPubKey() {
        return m_pub.getEncoded(/* compressed */ false);
    }

    /**
     * Gets the public key in the form of an elliptic curve point object from Bouncy Castle.
     *
     * @return  -
     */
    public ECPoint getPubKeyPoint() {
        return m_pub;
    }

    /**
     * Gets the private key in the form of an integer field element. The public key is derived by performing EC
     * point addition this number of times (i.e. point multiplying).
     *
     *
     * @return  -
     *
     * @throws java.lang.IllegalStateException if the private key bytes are not available.
     */
    public BigInteger getPrivKey() {
        if (m_privKey == null) {
            throw new MissingPrivateKeyException();
        } else if (m_privKey instanceof BCECPrivateKey) {
            return ((BCECPrivateKey) m_privKey).getD();
        } else {
            throw new MissingPrivateKeyException();
        }
    }

    /**
     * Returns whether this key is using the compressed form or not. Compressed pubkeys are only 33 bytes, not 64.
     *
     *
     * @return  -
     */
    public boolean isCompressed() {
        return m_pub.isCompressed();
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("pub:").append(Hex.toHexString(m_pub.getEncoded(false)));
        return b.toString();
    }

    /**
     * Produce a string rendering of the ECKey INCLUDING the private key.
     * Unless you absolutely need the private key it is better for security reasons to just use toString().
     *
     *
     * @return  -
     */
    public String toStringWithPrivate() {
        StringBuilder b = new StringBuilder();
        b.append(toString());
        if (m_privKey != null && m_privKey instanceof BCECPrivateKey) {
            b.append(" priv:").append(Hex.toHexString(((BCECPrivateKey) m_privKey).getD().toByteArray()));
        }
        return b.toString();
    }

    /**
     * Groups the two components that make up a signature, and provides a way to encode to Base64 form, which is
     * how ECDSA signatures are represented when embedded in other data structures in the Ethereum protocol. The raw
     * components can be useful for doing further EC maths on them.
     */
    public static class ECDSASignature {
        /**
         * The two components of the signature.
         */
        public final BigInteger r, s;
        public byte v;

        /**
         * Constructs a signature with the given components. Does NOT automatically canonicalise the signature.
         *
         * @param r -
         * @param s -
         */
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

        /**
         * Will automatically adjust the S component to be less than or equal to half the curve order, if necessary.
         * This is required because for every signature (r,s) the signature (r, -s (mod N)) is a valid signature of
         * the same message. However, we dislike the ability to modify the bits of a Ethereum transaction after it's
         * been signed, as that violates various assumed invariants. Thus in future only one of those forms will be
         * considered legal and the other will be banned.
         *
         * @return  -
         */
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

    /**
     * Signs the given hash and returns the R and S components as BigIntegers
     * and put them in ECDSASignature
     *
     * @param input to sign
     * @return ECDSASignature signature that contains the R and S components
     */
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

    /**
     * Takes the keccak hash (32 bytes) of data and returns the ECDSA signature
     *
     * @param messageHash -
     * @return -
     * @throws IllegalStateException if this ECKey does not have the private part.
     */
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

    /**
     * Given a piece of text and a message signature encoded in base64, returns an ECKey
     * containing the public key that was used to sign it. This can then be compared to the expected public key to
     * determine if the signature was correct.
     *
     * @param messageHash a piece of human readable text that was signed
     * @param signatureBase64 The Ethereum-format message signature in base64
     *
     * @return -
     * @throws SignatureException If the public key could not be recovered or if there was a signature format error.
     */
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

    /**
     * Compute the address of the key that signed the given signature.
     *
     * @param messageHash 32-byte hash of message
     * @param signatureBase64 Base-64 encoded signature
     * @return 20-byte address
     */
    public static byte[] signatureToAddress(byte[] messageHash, String signatureBase64) throws SignatureException {
        return computeAddress(signatureToKeyBytes(messageHash, signatureBase64));
    }

    /**
     * Compute the address of the key that signed the given signature.
     *
     * @param messageHash 32-byte hash of message
     * @param sig -
     * @return 20-byte address
     */
    public static byte[] signatureToAddress(byte[] messageHash, ECDSASignature sig) throws SignatureException {
        return computeAddress(signatureToKeyBytes(messageHash, sig));
    }

    /**
     * Compute the key that signed the given signature.
     *
     * @param messageHash 32-byte hash of message
     * @param signatureBase64 Base-64 encoded signature
     * @return ECKey
     */
    public static ECKey signatureToKey(byte[] messageHash, String signatureBase64) throws SignatureException {
        final byte[] keyBytes = signatureToKeyBytes(messageHash, signatureBase64);
        return ECKey.fromPublicOnly(keyBytes);
    }

    /**
     * Compute the key that signed the given signature.
     *
     * @param messageHash 32-byte hash of message
     * @param sig -
     * @return ECKey
     */
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

    /**
     * <p>Verifies the given ECDSA signature against the message bytes using the public key bytes.</p>
     *
     * <p>When using native ECDSA verification, data must be 32 bytes, and no element may be
     * larger than 520 bytes.</p>
     *
     * @param data Hash of the data to verify.
     * @param signature signature.
     * @param pub The public key bytes to use.
     *
     * @return -
     */
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

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
     *
     * @param data Hash of the data to verify.
     * @param signature signature.
     * @param pub The public key bytes to use.
     *
     * @return  -
     */
    public static boolean verify(byte[] data, byte[] signature, byte[] pub) {
        return verify(data, ECDSASignature.decodeFromDER(signature), pub);
    }

    /**
     * Verifies the given ASN.1 encoded ECDSA signature against a hash using the public key.
     *
     * @param data Hash of the data to verify.
     * @param signature signature.
     *
     * @return -
     */
    public boolean verify(byte[] data, byte[] signature) {
        return ECKey.verify(data, signature, getPubKey());
    }

    /**
     * Verifies the given R/S pair (signature) against a hash using the public key.
     *
     * @param sigHash -
     * @param signature -
     * @return -
     */
    public boolean verify(byte[] sigHash, ECDSASignature signature) {
        return ECKey.verify(sigHash, signature, getPubKey());
    }

    /**
     * Returns true if this pubkey is canonical, i.e. the correct length taking into account compression.
     *
     * @return -
     */
    public boolean isPubKeyCanonical() {
        return isPubKeyCanonical(m_pub.getEncoded(/* uncompressed */ false));
    }

    /**
     * Returns true if the given pubkey is canonical, i.e. the correct length taking into account compression.
     * @param pubkey -
     * @return -
     */
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

    /**
     * <p>Given the components of a signature and a selector value, recover and return the public key
     * that generated the signature according to the algorithm in SEC1v2 section 4.1.6.</p>
     *
     * <p>The recId is an index from 0 to 3 which indicates which of the 4 possible keys is the correct one. Because
     * the key recovery operation yields multiple potential keys, the correct key must either be stored alongside the
     * signature, or you must be willing to try each recId in turn until you find one that outputs the key you are
     * expecting.</p>
     *
     * <p>If this method returns null it means recovery was not possible and recId should be iterated.</p>
     *
     * <p>Given the above two points, a correct usage of this method is inside a for loop from 0 to 3, and if the
     * output is null OR a key that is not the one you expect, you try again with the next recId.</p>
     *
     * @param recId Which possible key to recover.
     * @param sig the R and S components of the signature, wrapped.
     * @param messageHash Hash of the data that was signed.
     * @return 65-byte encoded public key
     */
    @Nullable
    public static byte[] recoverPubBytesFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
    	check(recId >= 0, "recId must be positive");
    	check(sig.r.signum() >= 0, "r must be positive");
    	check(sig.s.signum() >= 0, "s must be positive");
    	check(messageHash != null, "messageHash must not be null");
        // 1.0 For j from 0 to h   (h == recId here and the loop is outside this function)
        //   1.1 Let x = r + jn
        BigInteger n = CURVE.getN();  // Curve order.
        BigInteger i = BigInteger.valueOf((long) recId / 2);
        BigInteger x = sig.r.add(i.multiply(n));
        //   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
        //        specified in Section 2.3.7, where mlen = 鈱�(log2 p)/8鈱� or mlen = 鈱坢/8鈱�.
        //   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
        //        conversion routine specified in Section 2.3.4. If this conversion routine outputs 鈥渋nvalid鈥�, then
        //        do another iteration of Step 1.
        //
        // More concisely, what these points mean is to use X as a compressed public key.
        ECCurve.Fp curve = (ECCurve.Fp) CURVE.getCurve();
        BigInteger prime = curve.getQ();  // Bouncy Castle is not consistent about the letter it uses for the prime.
        if (x.compareTo(prime) >= 0) {
            // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
            return null;
        }
        // Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
        // So it's encoded in the recId.
        ECPoint R = decompressKey(x, (recId & 1) == 1);
        //   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
        if (!R.multiply(n).isInfinity())
            return null;
        //   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
        BigInteger e = new BigInteger(1, messageHash);
        //   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
        //   1.6.1. Compute a candidate public key as:
        //               Q = mi(r) * (sR - eG)
        //
        // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
        //               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
        // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
        // ** is point multiplication and + is point addition (the EC group operator).
        //
        // We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
        // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
        BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
        BigInteger rInv = sig.r.modInverse(n);
        BigInteger srInv = rInv.multiply(sig.s).mod(n);
        BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
        ECPoint.Fp q = (ECPoint.Fp) ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);
        return q.getEncoded(/* compressed */ false);
    }

    /**
     *
     * @param recId Which possible key to recover.
     * @param sig the R and S components of the signature, wrapped.
     * @param messageHash Hash of the data that was signed.
     * @return 20-byte address
     */
    @Nullable
    public static byte[] recoverAddressFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
        final byte[] pubBytes = recoverPubBytesFromSignature(recId, sig, messageHash);
        if (pubBytes == null) {
            return null;
        } else {
            return computeAddress(pubBytes);
        }
    }

    /**
     *
     * @param recId Which possible key to recover.
     * @param sig the R and S components of the signature, wrapped.
     * @param messageHash Hash of the data that was signed.
     * @return ECKey
     */
    @Nullable
    public static ECKey recoverFromSignature(int recId, ECDSASignature sig, byte[] messageHash) {
        final byte[] pubBytes = recoverPubBytesFromSignature(recId, sig, messageHash);
        if (pubBytes == null) {
            return null;
        } else {
            return ECKey.fromPublicOnly(pubBytes);
        }
    }

    /**
     * Decompress a compressed public key (x co-ord and low-bit of y-coord).
     *
     * @param xBN -
     * @param yBit -
     * @return -
     */
    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        X9IntegerConverter x9 = new X9IntegerConverter();
        byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
        compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
        return CURVE.getCurve().decodePoint(compEnc);
    }

    /**
     * Returns a 32 byte array containing the private key, or null if the key is encrypted or public only
     *
     *  @return  -
     */
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