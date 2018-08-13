package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestProducerScheduleType {

	@Test
	 public void test1() throws IOException 
    {
		List<ProducerKey> pkList = new ArrayList<ProducerKey>();
		ProducerKey pk = new ProducerKey("sd","ghf","dsf");
		ProducerKey pk1 = new ProducerKey("sada","asd","fgdfg");
		pkList.add(pk);
		pkList.add(pk1);
		ProducerScheduleType pst = new ProducerScheduleType();
		pst.setVersion(20);
		pst.setProducers(pkList);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode blockNode = mapper.createObjectNode();
		pst.toJson(blockNode);
		String content = mapper.writeValueAsString (blockNode);
		System.out.println(content);
    }
 
	@Test
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		ProducerScheduleType pst = new ProducerScheduleType();
		String content = "{\"version\":20,\"producers\":[{\"producername\":\"sd\",\"address\":\"ghf\",\"blockProducerSigningKey\":\"dsf\"},{\"producername\":\"sada\",\"address\":\"asd\",\"blockProducerSigningKey\":\"fgdfg\"}]}"; 
	    JsonNode node = mapper.readTree(content); 
	    pst.jsonParse(node);
	}
}
