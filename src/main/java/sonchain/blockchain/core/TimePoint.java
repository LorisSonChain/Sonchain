package sonchain.blockchain.core;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimePoint {

	private Microseconds m_elapsed = new Microseconds(0);

	public Microseconds getElapsed() {
		return m_elapsed;
	}

	public void setElapsed(Microseconds elapsed) {
		m_elapsed = elapsed;
	}
	
	public TimePoint(){
	}
	
	public TimePoint(Microseconds e){
		m_elapsed = e;
	}
	
	public static TimePoint now(){
		return new TimePoint(new Microseconds(getmicTime()));
	}
	
	public static TimePoint maximum(){
		return new TimePoint(Microseconds.maximum());
	}
	
	public static TimePoint min(){
		return new TimePoint(new Microseconds());
	}

	@Override
	public String toString(){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dt = new Date(millisec_since_epoch());
		long msec = (m_elapsed.getCount() % 1000000) / 1000 + 1000;
		String strSec = String.valueOf(msec);
		return formatter.format(dt) + "." + strSec.substring(1);
		//return formatter.format(dt);
	}
	
	public static TimePoint from_iso_string(String s){
		int index = s.indexOf(".");
		if(index >= 0){
			String preS = s.substring(0, index);
			String ms = s.substring(index + 1);
			TimePoint ts = new TimePoint(Microseconds.seconds(TimePointSec.from_iso_string(preS).getUtcSeconds()));
			return ts = ts.add(new Microseconds(Long.valueOf(ms) - 1000));
		}
		else{
			return new TimePoint(Microseconds.seconds(TimePointSec.from_iso_string(s).getUtcSeconds()));
		}
	}
	
	public Microseconds time_since_epoch(){
		return m_elapsed;
	}
	
	public long millisec_since_epoch(){
		return m_elapsed.getCount() / 1000;
	}
	
	public long sec_since_epoch(){
		return m_elapsed.getCount() / 1000000;
	}

	public TimePoint add(TimePoint m){
		return new TimePoint(m_elapsed.add(m_elapsed, m.getElapsed()));
	}

	public TimePoint add(Microseconds m){
		return new TimePoint(m_elapsed.add(m_elapsed, m));
	}
	
	public TimePoint minus(TimePoint m){
		return new TimePoint(m_elapsed.minus(m_elapsed, m.getElapsed()));
	}
	
	public TimePoint minus(Microseconds m){
		return new TimePoint(m_elapsed.minus(m_elapsed, m));
	}
	
	public boolean equals(TimePoint t){
		return t.getElapsed().equals(m_elapsed);
	}
	
	public boolean gr(TimePoint t){
		return m_elapsed.getCount() > t.getElapsed().getCount();
	}
	
	public boolean ge(TimePoint t){
		return m_elapsed.getCount() >= t.getElapsed().getCount();
	}
	
	public boolean lr(TimePoint t){
		return m_elapsed.getCount() < t.getElapsed().getCount();
	}
	
	public boolean le(TimePoint t){
		return m_elapsed.getCount() <= t.getElapsed().getCount();
	}
	
	public static Long getmicTime() {
		Long cutime = System.currentTimeMillis() * 1000; // 微秒
		Long nanoTime = System.nanoTime(); // 纳秒
		return cutime + (nanoTime - nanoTime / 1000000 * 1000000) / 1000;
	}
}
