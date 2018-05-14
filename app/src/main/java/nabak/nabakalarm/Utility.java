package nabak.nabakalarm;

import java.util.Calendar;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class Utility {
	static final int alarmSetId = 123;

    // 
    // 현재 언어 설정이 한국어인지 판단
    //
    public static boolean useKoreanLanguage(Context context) {
   	 Locale lc = context.getResources().getConfiguration().locale;
   	 String language = lc.getLanguage();
        
        if (language.equals("ko")) { // 일본어 : ja,  영어 : en
       	 return true;
        } else {
       	 return false;
        }
    }

	 // DB 에 있는 첫번째 알람을 설정
    public  static void startFirstAlarm(Context context) {
    	int apday;
    	int onoff;
    	int day;
    	int hour;
    	int min;
    	int vib;
    	String ring;
    	String news;
    	
       Calendar calendar = Calendar.getInstance();
       int c_day = calendar.get(Calendar.DAY_OF_WEEK);	    
       int c_hour = calendar.get(Calendar.HOUR_OF_DAY);
       int c_min = calendar.get(Calendar.MINUTE);
       //minimum  가장 가가운 id를 찾는 변수
       int m_day = 100;
       int m_hour = 100;
       int m_min = 100;
       int m_vib = 0;
       String m_ring = null;
       String m_news = null;
       //
       long m_id;
    	// 오늘의 년-월-일
    	// 시간이 경과한 알람을 DB에서 제거
       DBAdapter db = new DBAdapter(context);
       if (db == null) return;
        
       db.open();
       Cursor c = db.fetchAllAlarm();
        
       if (c.moveToFirst()) {	// 첫번째로 이동
       	do {
       		onoff =  c.getInt(c.getColumnIndex(DBAdapter.ALARM_ON));
       		if (onoff == 1) {
       			day =  c.getInt(c.getColumnIndex(DBAdapter.ALARM_APDAY));
       			hour = c.getInt(c.getColumnIndex(DBAdapter.ALARM_HOUR));
       			min = c.getInt(c.getColumnIndex(DBAdapter.ALARM_MINUTE));
       			vib = c.getInt(c.getColumnIndex(DBAdapter.ALARM_VIBRATE));
       			ring = c.getString(c.getColumnIndex(DBAdapter.ALARM_RINGTONE));
       			news = c.getString(c.getColumnIndex(DBAdapter.ALARM_NEWS));
       			//
       			for (int i = 0; i < 7; i++) {
       				if ((day & 0x01) == 0x01) {	        					
       					apday = i+1;
       					if ((apday < c_day) 
       							|| ((apday == c_day) && (hour < c_hour))
       							|| ((apday == c_day) && (hour == c_hour) && (min <= c_min))){ 
       						apday += 7; 
       					}
       					
       					if (m_day > apday){
       						m_day = apday;
       						m_hour = hour;
       						m_min = min;
       						m_id = c.getLong(c.getColumnIndex("_id"));
       						m_vib = vib;
       						m_ring = ring;
       						m_news = news;
       					} else if ((m_day == apday ) && (m_hour > hour)){
       						m_hour = hour;
       						m_min = min;
       						m_id = c.getLong(c.getColumnIndex("_id"));
       						m_vib = vib;
       						m_ring = ring;
       						m_news = news;
       					} else if ((m_day == apday) && (m_hour == hour) && (m_min > min)){
       						m_min = min;
       						m_id = c.getLong(c.getColumnIndex("_id"));
       						m_vib = vib;
       						m_ring = ring;
       						m_news = news;
       					}
       				}
       				day = day >> 1;
       			}
       		}
       	} while (c.moveToNext());
    	}
       if( m_day != 100){
       	Intent intent = new Intent(context, alarmReceiver2.class);
       	intent.putExtra("ringtone", m_ring);
       	intent.putExtra("vibrate", m_vib);
       	intent.putExtra("newsKeyword", m_news);
		//PendingIntent sender = PendingIntent.getBroadcast(afternoonAlarm.this, 0, intent, 0);
       	calendar.add(Calendar.DAY_OF_MONTH, m_day - c_day);
			calendar.set(Calendar.HOUR_OF_DAY, m_hour);
	        calendar.set(Calendar.MINUTE, m_min);
	        calendar.set(Calendar.SECOND, 0);
	
       	PendingIntent sender = PendingIntent.getActivity(context, alarmSetId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		AlarmManager mManager = null;
       	mManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
       	mManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 0, sender);
       	Toast.makeText(context, "알람 설정 시간" + calendar.get(Calendar.YEAR)+ "년 "
			+ (calendar.get(Calendar.MONTH)+1) + "월 "
			+ calendar.get(Calendar.DAY_OF_MONTH) + "일 "
			+ calendar.get(Calendar.DAY_OF_WEEK)  + "요일 "
			+ calendar.get(Calendar.HOUR_OF_DAY) + "시 "
			+ calendar.get(Calendar.MINUTE)+ "분 ",
			Toast.LENGTH_SHORT).show();

       }

       db.close();
    } 

  //알람 해제
  	public static void cancelAlarm(Context context) {
  		Intent intent = new Intent(context, alarmReceiver2.class);
        PendingIntent sender = PendingIntent.getActivity(context, alarmSetId, intent, PendingIntent.FLAG_CANCEL_CURRENT );
          
          // And cancel the alarm.
         AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
         am.cancel(sender);
  		 Toast.makeText(context, "알람이 해제됐습니다.", Toast.LENGTH_SHORT).show();
  	}

}
