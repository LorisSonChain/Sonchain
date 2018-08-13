package sonchain.blockchain.core;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.databind.deser.DataFormatReaders;

/**
 * A lower resolution time_point accurate only to seconds from 1970
 * @author GAIA
 *
 */
public class TimePointSec {
	private long m_utcSeconds = 0;

	public long getUtcSeconds() {
		return m_utcSeconds;
	}

	public void setUtcSeconds(int utcSeconds) {
		m_utcSeconds = utcSeconds;
	}
	
	public TimePointSec(){
		m_utcSeconds = 0;
	}
	
	public TimePointSec(long seconds){
		m_utcSeconds = seconds;
	}

	public TimePointSec(TimePoint t){
		m_utcSeconds = t.time_since_epoch().getCount() / 1000000;
	}
		
	public static TimePointSec maximum(){
		return new TimePointSec(Long.MAX_VALUE);
	}
	
	public static TimePointSec min(){
		return new TimePointSec(0);
	}
	
	public long sec_since_epoch(){
		return m_utcSeconds;
	}
	
	public long millisec_since_epoch(){
		return m_utcSeconds * 1000;
	}
	
	public boolean gr(TimePointSec l, TimePointSec r){
		return l.getUtcSeconds() > r.getUtcSeconds();
	}
	
	public boolean ge(TimePointSec l, TimePointSec r){
		return l.getUtcSeconds() >= r.getUtcSeconds();
	}
	
	public boolean lr(TimePointSec l, TimePointSec r){
		return l.getUtcSeconds() < r.getUtcSeconds();
	}
	
	public boolean le(TimePointSec l, TimePointSec r){
		return l.getUtcSeconds() <= r.getUtcSeconds();
	}
	
	public boolean equals(TimePointSec s){
		return m_utcSeconds == s.getUtcSeconds();
	}
	
	public TimePointSec add(long offset){
		return new TimePointSec(m_utcSeconds + offset);
	}
	
	public TimePointSec add(TimePointSec t, Microseconds m){
		return new TimePointSec(t.getUtcSeconds() + m.getCount());
	}
	
	public TimePointSec minus(long offset){
		return new TimePointSec(m_utcSeconds - offset);
	}
	
	public TimePointSec minus(TimePointSec t, Microseconds m){
		return new TimePointSec(t.getUtcSeconds() - m.getCount());
	}
	
	public Microseconds minus(TimePointSec t, TimePointSec m){
		return new Microseconds(t.getUtcSeconds() - m.getUtcSeconds());
	}
	
	public Microseconds minus(TimePoint t, TimePointSec m){
		return new Microseconds(t.getElapsed().getCount() - m.getUtcSeconds());
	}
	
	public String to_non_delimited_iso_string(){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		Date dt = new Date(millisec_since_epoch());
		return formatter.format(dt);
	}
	
	public String to_iso_string(){
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dt = new Date(millisec_since_epoch());
		return formatter.format(dt);
	}
	
	public static TimePointSec from_iso_string(String s){
		try
		{
	        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			TimePointSec  t = new TimePointSec(formatter.parse(s).getTime() / 1000);
			return t;
		}catch(Exception ex){
			return null;
		}
	}

	  /** return a human-readable approximate time, relative to now()
	   * e.g., "4 hours ago", "2 months ago", etc.
	   */
	public static String get_approximate_relative_time_string(TimePoint event_time, TimePoint relative_to_time, String ago)
	{
		return get_approximate_relative_time_string(new TimePointSec(event_time), new TimePointSec(relative_to_time), ago);		
	}
	
	public static String get_approximate_relative_time_string(TimePointSec event_time, TimePointSec relative_to_time, String ago)
	{
		StringBuilder sb = new StringBuilder();
		if(ago == null || ago.length() == 0){
			ago = "ago";
		}		
		int seconds_ago = (int)(relative_to_time.sec_since_epoch() - event_time.sec_since_epoch());
		if(seconds_ago < 0){
		     ago = " in the future";
		     seconds_ago = -seconds_ago;
		}
		if(seconds_ago < 90){
			sb.append(seconds_ago);
			sb.append(" second");
			sb.append(seconds_ago > 1 ? "s" : "");
			sb.append(ago);	
			return sb.toString();
		}
		int minutes_go = (seconds_ago + 30) / 60;
		if(minutes_go < 90){
			sb.append(minutes_go);
			sb.append(" minute");
			sb.append(minutes_go > 1 ? "s" : "");
			sb.append(ago);		
			return sb.toString();	
		}
		int hours_go = (minutes_go + 30) / 60;
		if(hours_go < 90){
			sb.append(hours_go);
			sb.append(" hour");
			sb.append(hours_go > 1 ? "s" : "");
			sb.append(ago);		
			return sb.toString();	
		}
		int days_ago = (hours_go + 12) / 24;
		if(days_ago < 90){
			sb.append(days_ago);
			sb.append(" day");
			sb.append(days_ago > 1 ? "s" : "");
			sb.append(ago);		
			return sb.toString();	
		}
		int weeks_go = (days_ago + 3) / 7;
		if(weeks_go < 70){
			sb.append(weeks_go);
			sb.append(" week");
			sb.append(weeks_go > 1 ? "s" : "");
			sb.append(ago);		
			return sb.toString();	
		}
		int mongths_go = (days_ago + 15) / 30;
		if(mongths_go < 12){
			sb.append(mongths_go);
			sb.append(" month");
			sb.append(mongths_go > 1 ? "s" : "");
			sb.append(ago);		
			return sb.toString();	
		}
		int years_ago = days_ago / 365;
		sb.append(years_ago);
		sb.append(" year");
		sb.append(mongths_go > 1 ? "s" : "");
		sb.append(ago);		
		if(mongths_go < 12 * 5){
		      int leftover_days = days_ago - (years_ago * 365);
		      int leftover_months = (leftover_days + 15) / 30;
		      if (leftover_months > 0){
		  		sb.append(leftover_months);
		  		sb.append(" month");
		  		sb.append(mongths_go > 1 ? "s" : "");
		      }	
		}
  		sb.append(ago);		
		return sb.toString();
	}
	
	@Override
	public String toString(){
		return to_iso_string();
	}
}
