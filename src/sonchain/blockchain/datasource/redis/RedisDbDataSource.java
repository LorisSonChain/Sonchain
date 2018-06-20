package sonchain.blockchain.datasource.redis;

import java.util.Map;
import java.util.Set;

import sonchain.blockchain.datasource.DbSource;

public class RedisDbDataSource implements DbSource<byte[]> {

	@Override
	public void updateBatch(Map<byte[], byte[]> rows) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(byte[] key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean flush() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] get(byte[] key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(byte[] key, byte[] val) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isAlive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<byte[]> keys() throws RuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

}
