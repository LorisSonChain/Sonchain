package sonchain.blockchain.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.util.ByteUtil;
import sonchain.blockchain.util.FastByteComparisons;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Defines both the order, account name, and signing keys of the active set of
 * producers.
 * 
 * @author GAIA
 *
 */
public class ProducerScheduleType implements IJson {

	public static final Logger m_logger = Logger.getLogger(ProducerScheduleType.class);

	public ProducerScheduleType() {
	}

	private List<ProducerKey> m_producers = Collections.synchronizedList(new ArrayList<ProducerKey>());
	private int m_version = 0;

	public List<ProducerKey> getProducers() {
		return m_producers;
	}

	public void setProducers(List<ProducerKey> producers) {
		m_producers = producers;
	}

	public int getVersion() {
		return m_version;
	}

	public void setVersion(int version) {
		m_version = version;
	}

	public ProducerScheduleType(List<ProducerKey> m_producers, int m_version) {
		super();
		this.m_producers = m_producers;
		this.m_version = m_version;
	}

	public String getProducerKeyByAccount(String account) {
		for (ProducerKey producer : m_producers) {
			if (account.equals(producer.getProducerName())) {
				return producer.getBlockProducerSigningKey();
			}
		}
		return null;
	}

	public String getProducerKeyByAddress(String address) {
		for (ProducerKey producer : m_producers) {
			if (address.equals(producer.getAddress())) {
				return producer.getBlockProducerSigningKey();
			}
		}
		return null;
	}

	@Override
	public void toJson(ObjectNode blockHeaderNode){
		blockHeaderNode.put("version", m_version);
        m_logger.debug("toJson version\t\t\t: " + m_version);
        ArrayNode producers = blockHeaderNode.arrayNode();
        int size = m_producers.size();
        for(int i = 0; i < size; i ++){
        	ObjectNode producerNode = blockHeaderNode.objectNode();
        	m_producers.get(i).toJson(producerNode);
        	producers.add(producerNode);
        }
        blockHeaderNode.set("producers", producers);
	}

	@Override
	public synchronized void jsonParse(JsonNode node) {
		try {
			m_version = node.get("version1").asInt();
			m_logger.debug("jsonParse version1\t\t\t: " + m_version);
			if(m_producers == null){
				m_producers = new CopyOnWriteArrayList<>();
			}
			JsonNode producers = node.get("producers");
			for (JsonNode producer : producers) {
				ProducerKey key = new ProducerKey();
				key.jsonParse(producer);
				m_producers.add(key);
			}
		} catch (Exception e) {
			m_logger.error(e);
			throw new RuntimeException("Error on parsing Json", e);
		}
	}

	@Override
	public String toJson() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			ObjectNode node = mapper.createObjectNode();
			toJson(node);
			String jsonStr = mapper.writeValueAsString(node);
			m_logger.debug(" ProducerScheduleType Json String is :" + jsonStr);
			return jsonStr;
		} catch (Exception ex) {
			m_logger.error(" ProducerScheduleType toJson error:" + ex.getMessage());
			return "";
		}
	}

	@Override
	public synchronized void jsonParse(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.readTree(json);
			jsonParse(node);
		} catch (IOException ex) {
			m_logger.error(" ProducerScheduleType jsonParse error:" + ex.getMessage());
		}
	}

	@Override
	public String toString() {
		return toStringWithSuffix("\n");
	}

	private String toStringWithSuffix(final String suffix) {
		StringBuilder toStringBuff = new StringBuilder();
		toStringBuff.append("  version=").append(m_version).append(suffix);
		if (m_producers != null) {
			toStringBuff.append("  ProducerKeys=[").append(suffix);
			int count = m_producers.size();
			for (int i = 0; i < count; i++) {
				toStringBuff.append(m_producers.get(i).toString()).append(suffix);
			}
			toStringBuff.append("  ];").append(suffix);
		}
		return toStringBuff.toString();
	}
}
