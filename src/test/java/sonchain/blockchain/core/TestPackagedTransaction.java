package sonchain.blockchain.core;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestPackagedTransaction {

	@Test
    public void test1() throws IOException 
    {
		byte[] a = {23,123}; 
		byte[] b = {45,23}; 
		byte[] c = {67,34};
		SignedTransaction st = new SignedTransaction(c, a, b, a);
		PackedTransaction pt = new PackedTransaction(st, CompressionType.fromString("1"));
		ObjectMapper mapper = new ObjectMapper();
	    ObjectNode transNode = mapper.createObjectNode();
	    pt.toJson(transNode);
	    String content = mapper.writeValueAsString (transNode);
	    System.out.println(content);
    }
	@Test 
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		String content ="{\"blockHeight\":0,\"hash\":\"00\",\"nonce\":\"4322\",\"privateNote\":\"\",\"receiveAddress\":\"177b\",\"senderAddress\":\"00\",\"timeStamp\":0,\"version\":0,\"value\":\"2d17\",\"signature\":\"\",\"compression\":\"None\"}";
	    JsonNode node = mapper.readTree(content); 
		System.out.println(node);
		PackedTransaction pt = new PackedTransaction();
		pt.jsonParse(node);
	}
}
