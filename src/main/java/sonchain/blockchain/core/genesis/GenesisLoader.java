
package sonchain.blockchain.core.genesis;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

import sonchain.blockchain.accounts.AccountState;
import sonchain.blockchain.config.BlockChainConfig;
import sonchain.blockchain.core.BlockHeader;
import sonchain.blockchain.core.Genesis;
import sonchain.blockchain.core.PremineAccount;
import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.db.ByteArrayWrapper;
import sonchain.blockchain.service.DataCenter;
import sonchain.blockchain.trie.SecureTrie;
import sonchain.blockchain.trie.Trie;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Hex;

/**
 * Genesis Loader
 *
 */
public class GenesisLoader {
	
	public static final Logger m_logger = Logger.getLogger(GenesisLoader.class);

	private static Genesis createBlockForJson(GenesisJson genesisJson) {
		m_logger.debug("createBlockForJson start genesisJson:" + genesisJson.toString());
		byte[] minedBy = hexStringToBytesValidate(genesisJson.getMinedBy(), 20, false);
        byte[] mixHash = hexStringToBytesValidate(genesisJson.getMixHash(), 32, false);

		byte[] timestampBytes = hexStringToBytesValidate(genesisJson.getTimestamp(), 8, true);
		long timestamp = ByteUtil.byteArrayToLong(timestampBytes);

		byte[] parentHash = hexStringToBytesValidate(genesisJson.getParentHash(), 32, false);
		byte[] extraData = hexStringToBytesValidate(genesisJson.getExtraData(), 32, true);

		Genesis genesis = new Genesis(parentHash, minedBy, 0, timestamp, extraData, mixHash);
		m_logger.debug("createBlockForJson end genesis:" + genesis.toString());
		return genesis;
	}

	/**
	 * generatePreMine
	 * @param blockchainNetConfig
	 * @param allocs
	 * @return
	 */
	private static Map<ByteArrayWrapper, PremineAccount> generatePreMine(BlockChainConfig blockchainNetConfig,
			Map<String, AllocatedAccount> allocs) {
		m_logger.debug("generatePreMine start.");
		Map<ByteArrayWrapper, PremineAccount> premine = new HashMap<>();
		for (String key : allocs.keySet()) {
			byte[] address = ByteUtil.hexStringToBytes(key);
			AllocatedAccount alloc = allocs.get(key);
			m_logger.debug("PremineAccount address:" + key + "[balance]:" + alloc.getBalance().toString());
			PremineAccount state = new PremineAccount();
			AccountState accountState = new AccountState(blockchainNetConfig.getCommonConstants().getInitialNonce(),
					parseHexOrDec(alloc.getBalance()));			
//			if (alloc.getNonce() != null) {
//				accountState = accountState.withNonce(parseHexOrDec(alloc.getNonce()));
//			}//			
//			if (alloc.getCode() != null) {
//				byte[] codeBytes = ByteUtil.hexStringToBytes(alloc.getCode());
//				accountState = accountState.withCodeHash(HashUtil.sha3(codeBytes));
//				state.m_code = codeBytes;
//			}
			state.m_accountState = accountState;
			premine.put(ByteUtil.wrap(address), state);
			m_logger.debug("generatePreMine end.");
		}
		return premine;
	}

	public static byte[] generateRootHash(Map<ByteArrayWrapper, PremineAccount> premine) {

		m_logger.debug("generateRootHash start.");
		Trie<byte[]> state = new SecureTrie((byte[]) null);
		for (ByteArrayWrapper key : premine.keySet()) {
			state.put(key.getData(), premine.get(key).m_accountState.getEncoded());
		}
		byte[] rootHash  = state.getRootHash();
		m_logger.debug("generateRootHash end rootHash:" + Hex.toHexString(rootHash));
		return rootHash;
	}

	private static byte[] hexStringToBytesValidate(String hex, int bytes, boolean notGreater) {
		byte[] ret = ByteUtil.hexStringToBytes(hex);
		if (notGreater) {
			if (ret.length > bytes) {
				throw new RuntimeException("错误长度: " + hex + ", 预期长度 < " + bytes + " bytes");
			}
		} else {
			if (ret.length != bytes) {
				throw new RuntimeException("错误长度: " + hex + ", 预期长度 " + bytes + " bytes");
			}
		}
		return ret;
	}

	/**
	 * Load Genesis
	 */
	public static Genesis loadGenesis(InputStream resourceAsStream) {
		m_logger.debug("loadGenesis start.");
		GenesisJson genesisJson = loadGenesisJson(resourceAsStream);
		Genesis genesis = parseGenesis(genesisJson);
		m_logger.debug("loadGenesis end genesis:" + genesis.toString());
		return genesis;
	}

	/**
	 * Load GenesisJson
	 * @param config
	 * @param classLoader
	 * @return
	 * @throws RuntimeException
	 */
	public static GenesisJson loadGenesisJson()
			throws RuntimeException {
		m_logger.debug("loadGenesisJson start.");
		String genesisFile = DataCenter.m_config.m_genesisFilePath;
		if (genesisFile != null) {
			try (InputStream is = new FileInputStream(new File(genesisFile))) {
				GenesisJson genesisJson =  loadGenesisJson(is);
				m_logger.debug("loadGenesisJson end genesisJson:" + genesisJson.toString());
				return genesisJson;
			} catch (Exception e) {
				showLoadError("loadGenesisJson error path： " + genesisFile, genesisFile, genesisFile);
			}
		}
		return null;
	}

	/**
	 * Load GenesisJson
	 * @param genesisJsonIS
	 * @return
	 * @throws RuntimeException
	 */
	public static GenesisJson loadGenesisJson(InputStream genesisJsonIS) throws RuntimeException {
		String json = null;
		try {
			m_logger.debug("loadGenesisJson start.");
			json = new String(ByteStreams.toByteArray(genesisJsonIS));
			ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
					.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
			GenesisJson genesisJson = mapper.readValue(json, GenesisJson.class);
			m_logger.debug("loadGenesisJson end genesisJson:" + genesisJson.toString());
			return genesisJson;
		} catch (Exception e) {
			Utils.showErrorAndExit("loadGenesisJson error: " + e.getMessage(), json);
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static Genesis parseGenesis(GenesisJson genesisJson)
			throws RuntimeException {
		try {
			m_logger.debug("parseGenesis start.");
			Genesis genesis = createBlockForJson(genesisJson);
			genesis.setPremine(generatePreMine(DataCenter.m_config, genesisJson.getAlloc()));
			byte[] rootHash = generateRootHash(genesis.getPremine());
			genesis.setStateRoot(rootHash);
			m_logger.debug("parseGenesis end. genesis:" + genesis.toString());
			return genesis;
		} catch (Exception e) {
			e.printStackTrace();
			Utils.showErrorAndExit("Problem parsing genesis", e.getMessage());
		}
		return null;
	}
	
	private static BigInteger parseHexOrDec(String rawValue) {
		if (rawValue != null) {
			return rawValue.startsWith("0x") ? ByteUtil.bytesToBigInteger(
					ByteUtil.hexStringToBytes(rawValue)) : new BigInteger(rawValue);
		} else {
			return BigInteger.ZERO;
		}
	}
	
	private static byte[] prepareNonce(byte[] nonceUnchecked) {
		if (nonceUnchecked.length > 8) {
			throw new RuntimeException(String.format("非法的 nonce, 正确的长度为： %s ", BlockHeader.NONCE_LENGTH));
		} else if (nonceUnchecked.length == 8) {
			return nonceUnchecked;
		}
		byte[] nonce = new byte[BlockHeader.NONCE_LENGTH];
		int diff = BlockHeader.NONCE_LENGTH - nonceUnchecked.length;
		for (int i = diff; i < BlockHeader.NONCE_LENGTH; ++i) {
			nonce[i] = nonceUnchecked[i - diff];
		}
		return nonce;
	}

	private static void showLoadError(String message, String genesisFile, String genesisResource) {
		Utils.showErrorAndExit(message, "Config option 'genesisFile': " + genesisFile,
				"Config option 'genesis': " + genesisResource);
	}
}
