package sonchain.blockchain.core;


public enum CompressionType {

    None(0),
    Gzip(1);

    private int m_value = 0;

    private CompressionType(int value) {
        this.m_value = value;
    }

    public int Value() {
        return m_value;
    }

    @Override
    public String toString() {
    	if(m_value == 0){
    		return "None";
    	}
    	else if(m_value == 1){
    		return "Gzip";
    	}
    	return "";
    }
    
    public static CompressionType fromString(String str){
    	if(str.equals("Gzip")){
    		return CompressionType.Gzip;
    	}
		return CompressionType.None;
    }
}
