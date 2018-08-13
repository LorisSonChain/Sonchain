package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestBlockHeader {

	@Test
	public void test() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		// 对象转json
		// map<String,String>转json
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("name", "jack");
		map.put("city", "beijin");
		ArrayNode node = mapper.createArrayNode();
		for (Map.Entry<String, String> key : map.entrySet()) {
			ArrayNode node1 = node.arrayNode();
			node1.add(key.getKey());
			node1.add(key.getValue());
			node.add(node1);
		}
		String mjson = mapper.writeValueAsString(node);
		System.out.println(mjson);
		// json转map<String,String>
		HashMap<String, String> mmap = mapper.readValue(mjson, HashMap.class);
		System.out.println(mmap);

	}

	@Test
	public void test1() throws IOException {
		byte[] a = { 23, 34 };
		byte[] b = { 30, 6 };
//		ProducerKey producerKey = new ProducerKey("sda", "uewio", "sjda");
//		List<ProducerKey> listProducerKey = new ArrayList<>();
//		listProducerKey.add(producerKey);
//		ProducerScheduleType producerScheduleType = new ProducerScheduleType(listProducerKey,20);
		List<ProducerKey> pkList = new ArrayList<ProducerKey>();
		ProducerKey pk = new ProducerKey("sd","ghf","dsf");
		ProducerKey pk1 = new ProducerKey("sada","asd","fgdfg");
		pkList.add(pk);
		pkList.add(pk1);
		ProducerScheduleType pst = new ProducerScheduleType();
		pst.setVersion(20);
		pst.setProducers(pkList);
//		System.out.println(listProducerKey);
		BlockHeader blockHeader = new BlockHeader(2,2,"333",pst,"444","6555",23,"7777","8888","0000","34343",new BlockTimestamp(TimePoint.now()),33,b);
		//blockHeader.setTimestamp(new BlockTimestamp(TimePoint.now()));
		//blockHeader.setNewProducers(producerScheduleType);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode blockNode = mapper.createObjectNode();
		blockHeader.toJson(blockNode);
		String content = mapper.writeValueAsString(blockNode);
		System.out.println(content);
		//ArrayNode mode1 = blockNode.arrayNode();
	}

	@Test
	public void test2() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		BlockHeader blockHeader = new BlockHeader();
		String content ="{\"timestamp\":\"2018-08-08 16:49:52.000\",\"producer\":\"8888\",\"confirmed\":2,\"parentHash\":\"0000\",\"merkleTxRoot\":\"7777\",\"actionRoot\":\"444\",\"scheduleVersion\":2,\"producerSignature\":\"333\",\"hash\":\"1e06\",\"version\":33,\"stateRoot\":\"34343\",\"extraData\":\"6555\",\"blockNumber\":23,\"version1\":20,\"producers\":[{\"producername\":\"sd\",\"address\":\"ghf\",\"blockProducerSigningKey\":\"dsf\"},{\"producername\":\"sada\",\"address\":\"asd\",\"blockProducerSigningKey\":\"fgdfg\"}]}\r\n" + 
				"";
		JsonNode node = mapper.readTree(content);
		blockHeader.jsonParse(node);
		System.out.println(node);
	}
}
