package sonchain.blockchain.consensus;

import sonchain.blockchain.base.Binary;

public interface ISerializable {
	int getSize();
	void serialize(Binary writer);
	void deserialize(Binary reader);
}
