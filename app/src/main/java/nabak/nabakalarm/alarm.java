package nabak.nabakalarm;




import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import com.nbpcorp.mobilead.sdk.MobileAdListener;
import com.nbpcorp.mobilead.sdk.MobileAdView;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
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


//	bluetooth communication
	private static final int REQUEST_ENABLE_BT = 10;
	private int mPariedDeviceCount = 0;
	private Set<BluetoothDevice> mDevices;
	// 폰의 블루투스 모듈을 사용하기 위한 오브젝트.
	private BluetoothAdapter mBluetoothAdapter;
	/**
	 BluetoothDevice 로 기기의 장치정보를 알아낼 수 있는 자세한 메소드 및 상태값을 알아낼 수 있다.
	 연결하고자 하는 다른 블루투스 기기의 이름, 주소, 연결 상태 등의 정보를 조회할 수 있는 클래스.
	 현재 기기가 아닌 다른 블루투스 기기와의 연결 및 정보를 알아낼 때 사용.
	 */
	private BluetoothDevice mRemoteDevie;
	// 스마트폰과 페어링 된 디바이스간 통신 채널에 대응 하는 BluetoothSocket
	private BluetoothSocket mSocket = null;
	private OutputStream mOutputStream = null;
	private InputStream mInputStream = null;
	private String mStrDelimiter = "\n";
	private char mCharDelimiter =  '\n';



	private Thread mWorkerThread = null;
	private byte[] readBuffer;
	private int readBufferPosition;

	private Handler mHandler;
	private ConnectedThread mConnectedThread;
	public SensorData mSensor;


	EditText mEditReceive, mEditSend;
	Button mButtonSend;

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


		//bluetooth 통신 버튼
		mSensor = ((SensorData)getApplicationContext());
		Button bluetoothbtn = (Button)findViewById(R.id.bluetooth);
		bluetoothbtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v){
				checkBluetooth();
			}
		});

		mHandler = new Handler(){
			public void handleMessage(android.os.Message msg){
				if(msg.what == 2){
					String readMessage = null;
					try {
						readMessage = new String((byte[]) msg.obj, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
//                    Log.i("readMessage!!!!",readMessage);
				}

				if(msg.what == 3){
					if(msg.arg1 == 1)
						Log.i("Connected to Device: ", (String)(msg.obj));
					else
						Log.i("Connection Failed", (String)(msg.obj));
				}
			}
		};
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

	//bluetooth check method
	void checkBluetooth() {
		/**
		 * getDefaultAdapter() : 만일 폰에 블루투스 모듈이 없으면 null 을 리턴한다.
		 이경우 Toast를 사용해 에러메시지를 표시하고 앱을 종료한다.
		 */
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null ) {  // 블루투스 미지원
			Toast.makeText(getApplicationContext(), "기기가 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
			finish();  // 앱종료
		}
		else { // 블루투스 지원
			/** isEnable() : 블루투스 모듈이 활성화 되었는지 확인.
			 *               true : 지원 ,  false : 미지원
			 */
			if (!mBluetoothAdapter.isEnabled()) { // 블루투스 지원하며 비활성 상태인 경우.
				Toast.makeText(getApplicationContext(), "현재 블루투스가 비활성 상태입니다.", Toast.LENGTH_LONG).show();
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				// REQUEST_ENABLE_BT : 블루투스 활성 상태의 변경 결과를 App 으로 알려줄 때 식별자로 사용(0이상)
				/**
				 startActivityForResult 함수 호출후 다이얼로그가 나타남
				 "예" 를 선택하면 시스템의 블루투스 장치를 활성화 시키고
				 "아니오" 를 선택하면 비활성화 상태를 유지 한다.
				 선택 결과는 onActivityResult 콜백 함수에서 확인할 수 있다.
				 */
				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			} else { // 블루투스 지원하며 활성 상태인 경우.
				Log.i("체크 블루투스","checkbluetooth!!!!!");
				selectDevice();
			}
		}
	}
	// 블루투스 장치의 이름이 주어졌을때 해당 블루투스 장치 객체를 페어링 된 장치 목록에서 찾아내는 코드.
	BluetoothDevice getDeviceFromBondedList(String name) {
		// BluetoothDevice : 페어링 된 기기 목록을 얻어옴.
		BluetoothDevice selectedDevice = null;
		// getBondedDevices 함수가 반환하는 페어링 된 기기 목록은 Set 형식이며,
		// Set 형식에서는 n 번째 원소를 얻어오는 방법이 없으므로 주어진 이름과 비교해서 찾는다.
		for(BluetoothDevice deivce : mDevices) {
			// getName() : 단말기의 Bluetooth Adapter 이름을 반환
			if(name.equals(deivce.getName())) {
				selectedDevice = deivce;
				break;
			}
		}
		return selectedDevice;
	}

	void connectToSelectedDevice(String selectedDeviceName) {
		// BluetoothDevice 원격 블루투스 기기를 나타냄.
		mRemoteDevie = getDeviceFromBondedList(selectedDeviceName);
		// java.util.UUID.fromString : 자바에서 중복되지 않는 Unique 키 생성.
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");


		try {
			// 소켓 생성, RFCOMM 채널을 통한 연결.
			// createRfcommSocketToServiceRecord(uuid) : 이 함수를 사용하여 원격 블루투스 장치와 통신할 수 있는 소켓을 생성함.
			// 이 메소드가 성공하면 스마트폰과 페어링 된 디바이스간 통신 채널에 대응하는 BluetoothSocket 오브젝트를 리턴함.
			mSocket = mRemoteDevie.createRfcommSocketToServiceRecord(uuid);
			mSocket.connect(); // 소켓이 생성 되면 connect() 함수를 호출함으로써 두기기의 연결은 완료된다.
			Log.i("커넥투 디바이스","connectToSelectDevcie!!!!!");

			// 데이터 송수신을 위한 스트림 얻기.
			// BluetoothSocket 오브젝트는 두개의 Stream을 제공한다.
			// 1. 데이터를 보내기 위한 OutputStrem
			// 2. 데이터를 받기 위한 InputStream
			mOutputStream = mSocket.getOutputStream();
			mInputStream = mSocket.getInputStream();

			// 데이터 수신 준비.

			mConnectedThread = new ConnectedThread(mSocket);
			mConnectedThread.start();

		}catch(Exception e) { // 블루투스 연결 중 오류 발생
			Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
			finish();  // App 종료
		}
	}

	// 블루투스 지원하며 활성 상태인 경우.
	void selectDevice() {
		// 블루투스 디바이스는 연결해서 사용하기 전에 먼저 페어링 되어야만 한다
		// getBondedDevices() : 페어링된 장치 목록 얻어오는 함수.
		mDevices = mBluetoothAdapter.getBondedDevices();
		mPariedDeviceCount = mDevices.size();
		Log.i("셀렉트 디바이스","selectDevice!!!!!");
		if(mPariedDeviceCount == 0 ) { // 페어링된 장치가 없는 경우.
			Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
			finish(); // App 종료.
		}
		// .페어링된 장치가 있는 경우
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("블루투스 장치 선택");

		// 각 디바이스는 이름과(서로 다른) 주소를 가진다. 페어링 된 디바이스들을 표시한다.
		List<String> listItems = new ArrayList<String>();
		for(BluetoothDevice device : mDevices) {
			// device.getName() : 단말기의 Bluetooth Adapter 이름을 반환.
			listItems.add(device.getName());
		}
		listItems.add("취소");  // 취소 항목 추가.


		// CharSequence : 변경 가능한 문자열.
		// toArray : List형태로 넘어온것 배열로 바꿔서 처리하기 위한 toArray() 함수.
		final CharSequence[] items = listItems.toArray(new CharSequence[listItems.size()]);
		// toArray 함수를 이용해서 size만큼 배열이 생성 되었다.
		listItems.toArray(new CharSequence[listItems.size()]);

		builder.setItems(items, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item) {
				// TODO Auto-generated method stub
				if(item == mPariedDeviceCount) { // 연결할 장치를 선택하지 않고 '취소' 를 누른 경우.
					Toast.makeText(getApplicationContext(), "연결할 장치를 선택하지 않았습니다.", Toast.LENGTH_LONG).show();
					finish();
				}
				else { // 연결할 장치를 선택한 경우, 선택한 장치와 연결을 시도함.
					connectToSelectedDevice(items[item].toString());
				}
			}

		});

		builder.setCancelable(false);  // 뒤로 가기 버튼 사용 금지.
		AlertDialog alert = builder.create();
		alert.show();
	}

	//bluetooth connectixon class
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[1024];  // buffer store for the stream
			int bytes; // bytes returned from read()
			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream
					bytes = mmInStream.available();
					if (bytes != 0) {
						buffer = new byte[1024];
						SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
						Arrays.fill(buffer, (byte)0x00);

						bytes = mmInStream.available(); // how many bytes are ready to be read?
						bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
						String tempStr = new String(buffer,"utf-8");
						StringTokenizer st = new StringTokenizer(tempStr,"\n");
						tempStr = st.nextToken();
						if(tempStr != null){
							mSensor.setmSensorData(tempStr);
						}

						Log.i("tempSTR!!!",tempStr);
						if(buffer !=null) {
							mHandler.obtainMessage(2, bytes, -1, buffer)
									.sendToTarget(); // Send the obtained bytes to the UI activity
						}
					}
				} catch (IOException e) {
					e.printStackTrace();

					break;
				}
			}
		}
	}

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
