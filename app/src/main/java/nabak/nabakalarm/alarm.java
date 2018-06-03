package nabak.nabakalarm;




import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.nbpcorp.mobilead.sdk.MobileAdListener;
import com.nbpcorp.mobilead.sdk.MobileAdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class alarm extends Activity{
	ImageButton imagebutton;
	
	private MyCursorAdapter adapter;
	private ListView list;
	private DBAdapter db;
	private Cursor currentCursor;
	//
	private static int colID;
	private static int colONOFF;
	private static int colHOUR;
	private static int colMINUTE;
	private static int colDAY;
	private static int colRING;
	private static int colVIB;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm);
		
		//////////
		//
        db = new DBAdapter(this);
        db.open();
		//
        currentCursor = db.fetchAllAlarm();
        //
        list = (ListView)findViewById(R.id.list);
   		list.setOnItemClickListener(itemClickListener);
   		list.setOnTouchListener(TouchListener);
   		String[] from = new String[] {DBAdapter.ALARM_HOUR, DBAdapter.ALARM_MINUTE};
   		int[] to = new int[] {R.id.alarm_row_time, R.id.alarm_row_day};
   		//
   		adapter = new MyCursorAdapter(list.getContext(), R.layout.alarm_row, currentCursor, from, to);
   		list.setAdapter(adapter);  
        // column index
        colID = currentCursor.getColumnIndex("_id"); 
        colONOFF = currentCursor.getColumnIndex(DBAdapter.ALARM_ON);
        colDAY = currentCursor.getColumnIndex(DBAdapter.ALARM_APDAY);
        colHOUR = currentCursor.getColumnIndex(DBAdapter.ALARM_HOUR); 
        colMINUTE = currentCursor.getColumnIndex(DBAdapter.ALARM_MINUTE);
        colRING = currentCursor.getColumnIndex(DBAdapter.ALARM_RINGTONE);
        colVIB = currentCursor.getColumnIndex(DBAdapter.ALARM_VIBRATE);
		//
		//////////
		
		imagebutton = (ImageButton)findViewById(R.id.addAlarm);
		imagebutton.setOnClickListener(new ImageButton.OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 Intent intent = new Intent(alarm.this, alarmSet.class);
				 startActivity(intent);

			}		
		});
		Button bluetoothbtn = (Button)findViewById(R.id.bluetooth);
		bluetoothbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v){
				Intent intent2 = new Intent(alarm.this, Bluetooth.class);
				startActivity(intent2);
			}
		});
	}
	//
	@Override
	protected void onResume() {
		super.onResume();
		//
        currentCursor = db.fetchAllAlarm();

		adapter.notifyDataSetChanged();
	}
	
    @Override
	protected void onPause() {
    	super.onPause();
    }
    @Override
	protected void onDestroy() {
    	super.onDestroy();
    }
///////////////////////////////////////////////////////////////////////////
//for column action - 터치된 위치를 알아냄
///////////////////////////////////////////////////////////////////////////
	private static int QuickMenuEvent = 0; 
	private static float CheckedColumn_x = 0;
	
    OnTouchListener TouchListener = new OnTouchListener() {
    	@Override
		public boolean onTouch (View view, MotionEvent event) { 
    		// 여기서 view 는 ListItem 이 아닌  리스트 자체임
    		CheckedColumn_x = event.getX();
    		//CheckedColumn_y = event.getY();
    		QuickMenuEvent = event.getAction();

    		return false;
    	}
    };
/////////////////////////////////////////////////////////////////////////////////////////////////////	
	//
	AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?>list, View view, int position, long id) {
			currentCursor.moveToPosition(position);
			ImageView icon_view = (ImageView)view.findViewById(R.id.toggleButton1);
			ImageView delete_view = (ImageView)view.findViewById(R.id.alarm_delete);

			//
			if (QuickMenuEvent == MotionEvent.ACTION_UP) {
				if (icon_view.getLeft() < CheckedColumn_x && CheckedColumn_x < icon_view.getRight()) {
					long db_id = currentCursor.getLong(colID);
					int on = currentCursor.getInt(colONOFF);
					//
					if (on == 0) on = 1; 
					else on = 0;
					//
					db.modifyAlarmOn(db_id, on);
					currentCursor = db.fetchAllAlarm();
					adapter.notifyDataSetChanged();
					//
				//	calendar = Calendar.getInstance();
					if(on == 1){	
						icon_view.setImageResource(R.drawable.clock_on);
						Toast.makeText(getBaseContext(), "알람이 설정 되었습니다. " 
								, 
								Toast.LENGTH_SHORT).show();
					} else {
						icon_view.setImageResource(R.drawable.clock_off);
						Toast.makeText(alarm.this, "알람이 해제됐습니다.", Toast.LENGTH_SHORT).show();
					}
					
				} else if (delete_view.getLeft() < CheckedColumn_x && CheckedColumn_x < delete_view.getRight()) {
					new AlertDialog.Builder(alarm.this)
    				.setMessage("삭제하시겠습니까?")
    				.setCancelable(false)
    				.setPositiveButton("예",
    						new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									long db_id = currentCursor.getLong(colID);
									db.delAlarm("" + db_id);
									currentCursor = db.fetchAllAlarm();
									adapter.notifyDataSetChanged();
									Utility.cancelAlarm(alarm.this);
									Utility.startFirstAlarm(alarm.this);
								}
							})
					.setNegativeButton("아니요", null)
					.show();
				} else{
					long db_id = currentCursor.getLong(colID);
					
					 // 최고 우선 순위 알람에만 적용됨 수정할 부분
					 Intent intent = new Intent(alarm.this, alarmSet.class);
					 intent.putExtra("id", db_id);
					 intent.putExtra("day", currentCursor.getInt(colDAY));
					 intent.putExtra("hour", currentCursor.getInt(colHOUR));
					 intent.putExtra("min", currentCursor.getInt(colMINUTE));
					 intent.putExtra("ring", currentCursor.getString(colRING));
					 intent.putExtra("vib", currentCursor.getInt(colVIB));

					 startActivity(intent);
				}
			
			}
		}
	};

////////////////////////////////////////////////////////////////////////////////////
//list adapter	
////////////////////////////////////////////////////////////////////////////////////
	private class MyCursorAdapter extends SimpleCursorAdapter {
		Context my_context;
		private int mRowLayout;

		MyCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
			super(context, layout, c, from, to);
			my_context = context;
			mRowLayout = layout;
		}
		
		@Override
		public int getCount() {
			return currentCursor.getCount();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			currentCursor.moveToPosition(position); ///////////////
			ViewHolder viewHolder;
			
			if (convertView == null) {
				LayoutInflater inflater=(LayoutInflater)my_context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);         
				convertView = inflater.inflate(mRowLayout, parent, false);
				viewHolder = new ViewHolder();     
      	        viewHolder.icon = (ImageView)convertView.findViewById(R.id.toggleButton1);
      	        viewHolder.time = (TextView)convertView.findViewById(R.id.alarm_row_time);
      	        viewHolder.day = (TextView)convertView.findViewById(R.id.alarm_row_day);
      	        //
      	        convertView.setTag(viewHolder);
			} else {
      			viewHolder = (ViewHolder)convertView.getTag();
			}
			//
			viewHolder.time.setText(getTimeString(currentCursor.getInt(colHOUR), currentCursor.getInt(colMINUTE)));
			
			int day = currentCursor.getInt(colDAY);
			String strDay="";
			if((day & 0x01) == 0x01){ strDay = "일"; }
			if((day & 0x02) == 0x02){ strDay += "월"; }
			if((day & 0x04) == 0x04){ strDay += "화"; }
			if((day & 0x08) == 0x08){ strDay += "수"; }
			if((day & 0x10) == 0x10){ strDay += "목"; }
			if((day & 0x20) == 0x20){ strDay += "금"; }
			if((day & 0x40) == 0x40){ strDay += "토"; }
			//
			viewHolder.day.setText(strDay);
			//
			int on = currentCursor.getInt(colONOFF);
			
			if(on == 1)	viewHolder.icon.setImageResource(R.drawable.clock_on);
			else viewHolder.icon.setImageResource(R.drawable.clock_off); 
			//
			return convertView;
		}
		
    	private class ViewHolder {
    	 	ImageView icon;
    	 	TextView time;
    	 	TextView day;
    	};
	}
	
    public String getTimeString(int h, int m) {
    	 Calendar cal = Calendar.getInstance();
     	 cal.set(Calendar.HOUR_OF_DAY, h);
     	 cal.set(Calendar.MINUTE, m);
     	 SimpleDateFormat dayformat = new SimpleDateFormat("HH:mm");
     	 dayformat.setCalendar(cal);
     	 Date date = cal.getTime();
     	 return dayformat.format(date);
  }
}
