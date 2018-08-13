package sonchain.blockchain.core;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestSignedTransaction {

	@Test 
	public void test1() throws IOException 
	{
		byte[] a = {23,123}; 
		byte[] b = {45,23}; 
		byte[] c = {67,34}; 
		TransactionHeader th = new TransactionHeader();
		th.setRefBlockHeight(0);
		SignedTransaction st = new SignedTransaction();
		st.setSignature(null);
		st.setRefBlockHeight(23);
		st.setHash(c);
		st.setReceiveAddress(a);
		st.setVersion(23);
		ObjectMapper mapper = new ObjectMapper();
	    ObjectNode transNode = mapper.createObjectNode();
	    st.toJson(transNode);
	    String content = mapper.writeValueAsString (transNode);
	    System.out.println(content);
	}
	@Test 
	public void test2() throws IOException 
	{
		TransactionHeader th = new TransactionHeader();
		th.setRefBlockHeight(0);
		ObjectMapper mapper = new ObjectMapper();
		SignedTransaction st = new SignedTransaction();
		String content = "{\"blockHeight\":23,\"hash\":\"4322\",\"nonce\":\"00\",\"privateNote\":\"\",\"receiveAddress\":\"177b\",\"senderAddress\":\"00\",\"timeStamp\":0,\"version\":23,\"value\":\"00\",\"signature\":\"\"}";
		JsonNode node = mapper.readTree(content); 
		System.out.println(node);
		st.jsonParse(node);
	}
}
