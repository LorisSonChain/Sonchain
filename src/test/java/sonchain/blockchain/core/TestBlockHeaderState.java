package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestBlockHeaderState {

	@Test
	public void test1() throws IOException {

		byte[] a = { 23, 34 };
		byte[] b = { 30, 6 };
		ProducerKey producerKey = new ProducerKey("sda", "uewio", "sjda");
		List<ProducerKey> listProducerKey = new ArrayList<>();
		listProducerKey.add(producerKey);
		ProducerScheduleType producerScheduleType = new ProducerScheduleType(listProducerKey, 23);
		BlockHeader blockHeader = new BlockHeader();
		blockHeader.setStateRoot("11111111");
		blockHeader.setConfirmed(2);
		blockHeader.setStateRoot("2222222");
		blockHeader.setTimestamp(new BlockTimestamp(TimePoint.now()));
		blockHeader.setNewProducers(producerScheduleType);
		Map<String, Integer> map = new HashMap<String, Integer>();
		map.put("sda", 23);
		Map<String, Integer> map11 = new HashMap<String, Integer>();
		map.put("11111", 11);
		HeaderConfirmation arl = new HeaderConfirmation(a, "李四", "111111");
		List<Integer> intI = new ArrayList<Integer>();
		List<HeaderConfirmation> hclist = new ArrayList<HeaderConfirmation>();
		intI.add(12);
		intI.add(23);
		intI.add(43);
		hclist.add(arl);
		BlockHeaderState bhs = new BlockHeaderState(a, 23, blockHeader, 1, 2, 3, 4, a, producerScheduleType,
				producerScheduleType, map, map11, "ddsf", intI, hclist);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode blockNode = mapper.createObjectNode();
		bhs.toJson(blockNode);
		String content = mapper.writeValueAsString(blockNode);
		System.out.println(content);
	}

	@Test
	public void test2() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		BlockHeaderState BlockHeaderState = new BlockHeaderState();
		String content = "{\"hash\":\"1722\",\"pendingScheduleHash\":\"1722\",\"blockNum\":23,\"blockSigningKey\":"
				+ "\"ddsf\",\"dposProposedIrreversibleBlocknum\":1,\"dposIrreversibleBlocknum\":2,"
				+ "\"bftIrreversibleBlocknum\":3,\"pendingScheduleLibNum\":4,\"header\":{"
				+ "\"timestamp\":\"2018-08-07 17:10:14.000\",\"producer\":\"\",\"confirmed\":2,"
				+ "\"parentHash\":\"\",\"merkleTxRoot\":\"\",\"actionRoot\":\"\",\"scheduleVersion\":0,"
				+ "\"newProducers\":{\"version\":23,\"producers\":[{\"producername\":\"sda\",\"address\":"
				+ "\"uewio\",\"blockProducerSigningKey\":\"sjda\"}]},\"producerSignature\":\"\",\"hash\":"
				+ "\"c20963c3c3c10c01fd67783e3e27765467d44dd27ad092e3efb445740c2cb8ed\",\"version\":0,"
				+ "\"stateRoot\":\"2222222\",\"extraData\":\"\",\"blockNumber\":0},\"pendingSchedule\":"
				+ "{\"version\":23,\"producers\":[{\"producername\":\"sda\",\"address\":"
				+ "\"uewio\",\"blockProducerSigningKey\":\"sjda\"}]},\"activeSchedule\":{"
				+ "\"version\":23,\"producers\":[{\"producername\":\"sda\",\"address\":\"uewio\",\"blockProducerSigningKey"
				+ "\":\"sjda\"}]},\"confirmCount\":[12,23,43],\"confirmations\":[{\"blockHash\":\"1722\","
				+ "\"producerSignature\":\"111111\",\"producer\":\"李四\"}],\"producerToLastProduced\":[[\"sda\",23]],"
				+ "\"producerToLastImpliedIrb\":[[\"11111\",11]]}";
		JsonNode node = mapper.readTree(content);
		BlockHeaderState.jsonParse(node);
		System.out.println(node);
	}

}
