package sonchain.blockchain.crypto.jce;

import java.security.Provider;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class SpongyCastleProvider {

	private static class Holder {
		private static final Provider INSTANCE;
		static {
			Provider p = Security.getProvider("SC");

			if(p != null)
			{
				INSTANCE = p;
			}
			else
			{
				INSTANCE = new BouncyCastleProvider();
			}

			INSTANCE.put("MessageDigest.GTC-KECCAK-256", "sonchain.blockchain.crypto.cryptohash.Keccak256");
			INSTANCE.put("MessageDigest.GTC-KECCAK-512", "sonchain.blockchain.crypto.cryptohash.Keccak512");
		}
	}

	public static Provider getInstance() {
		return Holder.INSTANCE;
	}
}
