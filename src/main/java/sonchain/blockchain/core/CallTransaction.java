package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import sonchain.blockchain.crypto.HashUtil;
import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;
import sonchain.blockchain.vm.LogInfo;

/**
 * Creates a contract function call transaction. Serializes arguments according
 * to the function ABI .
 *
 */
public class CallTransaction {

	private final static ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);

	public static Transaction createRawTransaction(long nonce, long gasPrice, long gasLimit, String toAddress,
			long value, byte[] data) {
		Transaction tx = new Transaction(ByteUtil.longToBytesNoLeadZeroes(nonce), 
				toAddress == null ? null : Hex.decode(toAddress),
						ByteUtil.longToBytesNoLeadZeroes(value), data);
		return tx;
	}

	public static Transaction createCallTransaction(long nonce, long gasPrice, long gasLimit, String toAddress,
			long value, Function callFunc, Object... funcArgs) {

		byte[] callData = callFunc.encode(funcArgs);
		return createRawTransaction(nonce, gasPrice, gasLimit, toAddress, value, callData);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class Param {
		public Boolean m_indexed = false;
		public String m_name = "";
		//TODO
		//public ContractParamType m_type;

//		@JsonGetter("type")
//		public String getType() {
//			return m_type.getName();
//		}
	}

	public enum FunctionType {
		constructor, function, event, fallback
	}

	public static class Function {
		public boolean m_anonymous;
		public boolean m_constant;
		public boolean m_payable;
		public String m_name = "";
		public Param[] m_inputs = new Param[0];
		public Param[] m_outputs = new Param[0];
		public FunctionType m_type;

		private Function() {
		}

		public byte[] encode(Object... args) {
			return ByteUtil.merge(encodeSignature(), encodeArguments(args));
		}

		public byte[] encodeArguments(Object... args) {
			if (args.length > m_inputs.length)
				throw new RuntimeException("Too many arguments: " + args.length + " > " + m_inputs.length);

			int staticSize = 0;
			int dynamicCnt = 0;
			//TODO
			// calculating static size and number of dynamic params
//			for (int i = 0; i < args.length; i++) {
//				Param param = m_inputs[i];
//				if (param.m_type.isDynamicType()) {
//					dynamicCnt++;
//				}
//				staticSize += param.m_type.getFixedSize();
//			}
			//TODO

			byte[][] bb = new byte[args.length + dynamicCnt][];

			int curDynamicPtr = staticSize;
			int curDynamicCnt = 0;
			//TODO
//			for (int i = 0; i < args.length; i++) {
//				if (m_inputs[i].m_type.isDynamicType()) {
//					byte[] dynBB = m_inputs[i].m_type.encode(args[i]);
//					bb[i] = ContractParamType.IntType.encodeInt(curDynamicPtr);
//					bb[args.length + curDynamicCnt] = dynBB;
//					curDynamicCnt++;
//					curDynamicPtr += dynBB.length;
//				} else {
//					bb[i] = m_inputs[i].m_type.encode(args[i]);
//				}
//			}
			//TODO
			return ByteUtil.merge(bb);
		}

		private Object[] decode(byte[] encoded, Param[] params) {
			Object[] ret = new Object[params.length];

			int off = 0;
			//TODO
//			for (int i = 0; i < params.length; i++) {
//				if (params[i].m_type.isDynamicType()) {
//					ret[i] = params[i].m_type.decode(encoded, IntType.decodeInt(encoded, off).intValue());
//				} else {
//					ret[i] = params[i].m_type.decode(encoded, off);
//				}
//				off += params[i].m_type.getFixedSize();
//			}
			//TODO
			return ret;
		}

		public Object[] decode(byte[] encoded) {
			return decode(ArrayUtils.subarray(encoded, 4, encoded.length), m_inputs);
		}

		public Object[] decodeResult(byte[] encodedRet) {
			return decode(encodedRet, m_outputs);
		}

		public String formatSignature() {
			StringBuilder paramsTypes = new StringBuilder();
			//TODO
//			for (Param param : m_inputs) {
//				paramsTypes.append(param.m_type.getCanonicalName()).append(",");
//			}
			//TODO

			return String.format("%s(%s)", m_name, StringUtils.stripEnd(paramsTypes.toString(), ","));
		}

		public byte[] encodeSignatureLong() {
			String signature = formatSignature();
			byte[] sha3Fingerprint = HashUtil.sha3(signature.getBytes());
			return sha3Fingerprint;
		}

		public byte[] encodeSignature() {
			return Arrays.copyOfRange(encodeSignatureLong(), 0, 4);
		}

		@Override
		public String toString() {
			return formatSignature();
		}

		public static Function fromJsonInterface(String json) {
			try {
				return DEFAULT_MAPPER.readValue(json, Function.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public static Function fromSignature(String funcName, String... paramTypes) {
			return fromSignature(funcName, paramTypes, new String[0]);
		}

		public static Function fromSignature(String funcName, String[] paramTypes, String[] resultTypes) {
			Function ret = new Function();
			ret.m_name = funcName;
			ret.m_constant = false;
			ret.m_type = FunctionType.function;
			ret.m_inputs = new Param[paramTypes.length];
			for (int i = 0; i < paramTypes.length; i++) {
				ret.m_inputs[i] = new Param();
				ret.m_inputs[i].m_name = "param" + i;
				//TODO
				//ret.m_inputs[i].m_type = ContractParamType.getType(paramTypes[i]);
				//TODO
			}
			ret.m_outputs = new Param[resultTypes.length];
			for (int i = 0; i < resultTypes.length; i++) {
				ret.m_outputs[i] = new Param();
				ret.m_outputs[i].m_name = "res" + i;
				//TODO
				//ret.m_outputs[i].m_type = ContractParamType.getType(resultTypes[i]);
				//TODO
			}
			return ret;
		}

	}

	public static class Contract {
		public Contract(String jsonInterface) {
			try {
				functions = new ObjectMapper().readValue(jsonInterface, Function[].class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public Function getByName(String name) {
			for (Function function : functions) {
				if (name.equals(function.m_name)) {
					return function;
				}
			}
			return null;
		}

		public Function getConstructor() {
			for (Function function : functions) {
				if (function.m_type == FunctionType.constructor) {
					return function;
				}
			}
			return null;
		}

		private Function getBySignatureHash(byte[] hash) {
			if (hash.length == 4) {
				for (Function function : functions) {
					if (FastByteComparisons.equal(function.encodeSignature(), hash)) {
						return function;
					}
				}
			} else if (hash.length == 32) {
				for (Function function : functions) {
					if (FastByteComparisons.equal(function.encodeSignatureLong(), hash)) {
						return function;
					}
				}
			} else {
				throw new RuntimeException("Function signature hash should be 4 or 32 bytes length");
			}
			return null;
		}

		/**
		 * Parses function and its arguments from transaction invocation binary
		 * data
		 */
		public Invocation parseInvocation(byte[] data) {
			if (data.length < 4)
				throw new RuntimeException("Invalid data length: " + data.length);
			Function function = getBySignatureHash(Arrays.copyOfRange(data, 0, 4));
			if (function == null)
				throw new RuntimeException("Can't find function/event by it signature");
			Object[] args = function.decode(data);
			return new Invocation(this, function, args);
		}

		/**
		 * Parses Solidity Event and its data members from transaction receipt
		 * LogInfo
		 */
		public Invocation parseEvent(LogInfo eventLog) {
			CallTransaction.Function event = getBySignatureHash(eventLog.getTopics().get(0).getData());
			int indexedArg = 1;
			if (event == null)
				return null;
			List<Object> indexedArgs = new ArrayList<>();
			List<Param> unindexed = new ArrayList<>();
			for (Param input : event.m_inputs) {
				if (input.m_indexed) {
					//TODO
					//indexedArgs.add(input.m_type.decode(eventLog.getTopics().get(indexedArg++).getData()));
					//TODO
					continue;
				}
				unindexed.add(input);
			}

			Object[] unindexedArgs = event.decode(eventLog.getData(), unindexed.toArray(new Param[unindexed.size()]));
			Object[] args = new Object[event.m_inputs.length];
			int unindexedIndex = 0;
			int indexedIndex = 0;
			for (int i = 0; i < args.length; i++) {
				if (event.m_inputs[i].m_indexed) {
					args[i] = indexedArgs.get(indexedIndex++);
					continue;
				}
				args[i] = unindexedArgs[unindexedIndex++];
			}
			return new Invocation(this, event, args);
		}

		public Function[] functions;
	}

	/**
	 * Represents either function invocation with its arguments or Event
	 * instance with its data members
	 */
	public static class Invocation {
		public final Contract m_contract;
		public final Function m_function;
		public final Object[] m_args;

		public Invocation(Contract contract, Function function, Object[] args) {
			m_contract = contract;
			m_function = function;
			m_args = args;
		}

		@Override
		public String toString() {
			return "[" + "contract=" + m_contract + (m_function.m_type == FunctionType.event ? ", event=" : ", function=")
					+ m_function + ", args=" + Arrays.toString(m_args) + ']';
		}
	}
}
