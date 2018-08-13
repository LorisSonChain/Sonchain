package sonchain.blockchain.datasource.redis;

import java.util.Map;
import java.util.Set;

import sonchain.blockchain.datasource.base.DbSource;

public class RedisDbDataSource implements DbSource<String> {

	@Override
	public void updateBatch(Map<String, String> rows) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(String key) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean flush() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String get(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void put(String key, String val) {
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
	public Set<String> keys() throws RuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

}
