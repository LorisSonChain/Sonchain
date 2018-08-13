package sonchain.blockchain.core;

public class Microseconds {
	private long m_count = 0;

	public long getCount() {
		return m_count;
	}

	public void setCount(long count) {
		m_count = count;
	}
	
	public Microseconds(){
	}	
	
	public Microseconds(long count){
		m_count = count;
	}
	
	public static Microseconds maximum(){
		return new Microseconds(Long.MAX_VALUE);
	}
	
	public long to_seconds(){
		return m_count / 1000000;
	}
	
	public Microseconds add(Microseconds l, Microseconds r){
		return new Microseconds(l.getCount() + r.getCount());
	}
	
	public Microseconds minus(Microseconds l, Microseconds r){
		return new Microseconds(l.getCount() - r.getCount());
	}
	
	public boolean equals(Microseconds s){
		return m_count == s.getCount();
	}
	
	public boolean gr(Microseconds l, Microseconds r){
		return l.getCount() > r.getCount();
	}
	
	public boolean ge(Microseconds l, Microseconds r){
		return l.getCount() >= r.getCount();
	}
	
	public boolean lr(Microseconds l, Microseconds r){
		return l.getCount() < r.getCount();
	}
	
	public boolean le(Microseconds l, Microseconds r){
		return l.getCount() <= r.getCount();
	}
	
	public static Microseconds seconds(long s){
		return new  Microseconds(s * 1000000);
	}
	
	public static Microseconds milliseconds(long s)
	{
		return new Microseconds(s * 1000);
	}
	
	public static Microseconds minutes(long m)
	{
		return seconds(m * 60);
	}
	
	public static Microseconds hours(long h)
	{
		return minutes(h * 60);
	}

	public static Microseconds days(long d)
	{
		return hours(d * 24);
	}
}
