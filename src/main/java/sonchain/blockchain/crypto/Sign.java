package sonchain.blockchain.crypto;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Arrays;

import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;

import sonchain.blockchain.crypto.ECKey.ECDSASignature;
import sonchain.blockchain.util.Assertions;
import sonchain.blockchain.util.Numeric;

public class Sign {

	private static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256k1");
	static final ECDomainParameters CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(),
			CURVE_PARAMS.getN(), CURVE_PARAMS.getH());
	static final BigInteger HALF_CURVE_ORDER = CURVE_PARAMS.getN().shiftRight(1);

	public static SignatureData signMessage(byte[] message, ECKeyPair keyPair) {
		return signMessage(message, keyPair, true);
	}

	public static SignatureData signMessage(byte[] message, ECKeyPair keyPair, boolean isHashed) {
		BigInteger publicKey = keyPair.GetPublicKey();
		byte[] messageHash;
		if (isHashed) {
			messageHash = HashUtil.sha3(message);
		} else {
			messageHash = message;
		}

		ECDSASignature sig = keyPair.Sign(messageHash);
		// Now we have to work backwards to figure out the recId needed to
		// recover the signature.
		int recId = -1;
		for (int i = 0; i < 4; i++) {
			BigInteger k = recoverFromSignature(i, sig, messageHash);
			if (k != null && k.equals(publicKey)) {
				recId = i;
				break;
			}
		}
		if (recId == -1) {
			throw new RuntimeException("Could not construct a recoverable key. This should never happen.");
		}

		int headerByte = recId + 27;

		// 1 header + 32 bytes for R + 32 bytes for S
		byte v = (byte) headerByte;
		byte[] r = Numeric.toBytesPadded(sig.r, 32);
		byte[] s = Numeric.toBytesPadded(sig.s, 32);

		return new SignatureData(v, r, s);
	}
	
	public static BigInteger recoverFromSignature(int recId, ECDSASignature sig, byte[] message) {
		Assertions.verifyPrecondition(recId >= 0, "recId must be positive");
		Assertions.verifyPrecondition(sig.r.signum() >= 0, "r must be positive");
		Assertions.verifyPrecondition(sig.s.signum() >= 0, "s must be positive");
		Assertions.verifyPrecondition(message != null, "message cannot be null");
		BigInteger n = CURVE.getN(); // Curve order.
		BigInteger i = BigInteger.valueOf((long) recId / 2);
		BigInteger x = sig.r.add(i.multiply(n));
		BigInteger prime = SecP256K1Curve.q;
		if (x.compareTo(prime) >= 0) {
			// Cannot have point co-ordinates larger than this as everything
			// takes place modulo Q.
			return null;
		}
		ECPoint R = decompressKey(x, (recId & 1) == 1);
		if (!R.multiply(n).isInfinity()) {
			return null;
		}
		BigInteger e = new BigInteger(1, message);
		BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
		BigInteger rInv = sig.r.modInverse(n);
		BigInteger srInv = rInv.multiply(sig.s).mod(n);
		BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
		ECPoint q = ECAlgorithms.sumOfTwoMultiplies(CURVE.getG(), eInvrInv, R, srInv);

		byte[] qBytes = q.getEncoded(false);
		// We remove the prefix
		return new BigInteger(1, Arrays.copyOfRange(qBytes, 1, qBytes.length));
	}

	/** Decompress a compressed public key (x co-ord and low-bit of y-coord). */
	private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
		X9IntegerConverter x9 = new X9IntegerConverter();
		byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.getCurve()));
		compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
		return CURVE.getCurve().decodePoint(compEnc);
	}

	public static BigInteger signedMessageToKey(byte[] message, SignatureData signatureData) throws SignatureException {

		byte[] r = signatureData.getR();
		byte[] s = signatureData.getS();
		Assertions.verifyPrecondition(r != null && r.length == 32, "r must be 32 bytes");
		Assertions.verifyPrecondition(s != null && s.length == 32, "s must be 32 bytes");

		int header = signatureData.getV() & 0xFF;
		// The header byte: 0x1B = first key with even y, 0x1C = first key with
		// odd y,
		// 0x1D = second key with even y, 0x1E = second key with odd y
		if (header < 27 || header > 34) {
			throw new SignatureException("Header byte out of range: " + header);
		}

		ECDSASignature sig = new ECDSASignature(new BigInteger(1, signatureData.getR()),
				new BigInteger(1, signatureData.getS()));

		byte[] messageHash = HashUtil.sha3(message);
		int recId = header - 27;
		BigInteger key = recoverFromSignature(recId, sig, messageHash);
		if (key == null) {
			throw new SignatureException("Could not recover public key from signature");
		}
		return key;
	}

	public static BigInteger publicKeyFromPrivate(BigInteger privKey) {
		ECPoint point = publicPointFromPrivate(privKey);

		byte[] encoded = point.getEncoded(false);
		return new BigInteger(1, Arrays.copyOfRange(encoded, 1, encoded.length)); // remove
																					// prefix
	}
	
	private static ECPoint publicPointFromPrivate(BigInteger privKey) {
		/*
		 * TODO: FixedPointCombMultiplier currently doesn't support scalars
		 * longer than the group order, but that could change in future
		 * versions.
		 */
		if (privKey.bitLength() > CURVE.getN().bitLength()) {
			privKey = privKey.mod(CURVE.getN());
		}
		return new FixedPointCombMultiplier().multiply(CURVE.getG(), privKey);
	}

	public static class SignatureData {
		private final byte v;
		private final byte[] r;
		private final byte[] s;

		public SignatureData(byte v, byte[] r, byte[] s) {
			this.v = v;
			this.r = r;
			this.s = s;
		}

		public byte getV() {
			return v;
		}

		public byte[] getR() {
			return r;
		}

		public byte[] getS() {
			return s;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			SignatureData that = (SignatureData) o;

			if (v != that.v) {
				return false;
			}
			if (!Arrays.equals(r, that.r)) {
				return false;
			}
			return Arrays.equals(s, that.s);
		}

		@Override
		public int hashCode() {
			int result = (int) v;
			result = 31 * result + Arrays.hashCode(r);
			result = 31 * result + Arrays.hashCode(s);
			return result;
		}
	}
}
