package sonchain.blockchain.core;


import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestTransaction {
    public void test1() throws IOException 
    {
		Transaction trans = new Transaction();
		byte[] a = {23,123}; 
		byte[] b = {45,23}; 
		byte[] c = {67,34}; 
		trans.setNonce(a);
		trans.setValue(b);
		trans.setVersion(100);
		trans.setReceiveAddress(b);
		ObjectMapper mapper = new ObjectMapper();
	    ObjectNode transNode = mapper.createObjectNode();
	    trans.toJson(transNode);
	    String content = mapper.writeValueAsString (transNode);
	    System.out.println(content);
    }
	@Test 
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		Transaction trans = new Transaction();
		String content = ("{\"blockHeight\":0,\"hash\":\"00\",\"inputData\":\"00\",\"nonce\":\"177b\",\"privateNote\":\"\",\"receiveAddress\":\"2d17\",\"senderAddress\":\"00\",\"remark\":\"\",\"timeStamp\":0,\"version\":100,\"value\":\"2d17\",\"v\":0,\"r\":0,\"s\":0}");
	    JsonNode node = mapper.readTree(content); 
		System.out.println(node);
		trans.jsonParse(node);
	}
}
