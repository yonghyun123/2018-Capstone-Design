package nabak.nabakalarm;


import java.util.Calendar;
import java.util.Locale;

import com.nbpcorp.mobilead.sdk.MobileAdListener;
import com.nbpcorp.mobilead.sdk.MobileAdView;

import kankan.wheel.widget.NumericWheelAdapter;
import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class alarmSet extends Activity {
    // 알람 관련 멤버 변수 ////

    //ringtone 변수
    private String strRingTone = "";
    private String newsKeyword = "";

    //알람 메니저
    private AlarmManager mManager = null;
    //시각 설정 클래스
    private TimePicker mTime;
    //달력
    public Calendar calendar;
    //진동
    private int mVibrate = 0;
    //TextToSpeech


    // Time changed flag
    private boolean timeChanged = false;
    //
    private boolean timeScrolled = false;

    private int mAlarmHour = 12;
    private int mAlarmMinute = 0;
    //private GregorianCalendar mCalendar;

    private long db_id = -1;
    ///////////////////// notification member value(통지 관련 멤버 변수) ///////////////////////
    private NotificationManager mNotification;

    //select news or rington
    private RadioGroup mRadioGroup;
    private RadioButton mSelectNew;
    private RadioButton mSelectRing;

    private TextView ringtoneView;
    private EditText newsView;
    private RelativeLayout layout;



    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarmset);

        calendar = Calendar.getInstance();
        // radiogroup 생성

        mSelectNew = (RadioButton)findViewById(R.id.selectNews);
        mSelectRing = (RadioButton)findViewById(R.id.selectRing);
        mRadioGroup = (RadioGroup)findViewById(R.id.select_radio);
        ringtoneView = (TextView)findViewById(R.id.ringtone);
        newsView = (EditText)findViewById(R.id.newsText);
        layout = (RelativeLayout)findViewById(R.id.alarm_total);
        newsView.setVisibility(View.GONE);
        //radioGroup listener
        mRadioGroup.setOnCheckedChangeListener(radioGroupButtonChangeListener);

        //create by yonghyun 2017.5.5
        //keyboard down

        //통지 매니저를 흭득
        mNotification = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //알람 매니저를 취득
        mManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        final WheelView hours = (WheelView) findViewById(R.id.hour);
        hours.setAdapter(new NumericWheelAdapter(0, 23));
        hours.setLabel("hours");
        hours.setCyclic(true);


        final WheelView mins = (WheelView) findViewById(R.id.mins);
        mins.setAdapter(new NumericWheelAdapter(0, 59, "%02d"));
        mins.setLabel("mins");
        mins.setCyclic(true);


        ringtoneView.setOnClickListener(ringSelectBtnListener);

        //셋 버튼을 등록 (확인 버튼)
        Button button = (Button) findViewById(R.id.set);
        button.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                newsKeyword = newsView.getText().toString();
                setAlarm();
            }
        });

        //시각 설정 클래스로 현재 시각을 설정
        mTime = (TimePicker) findViewById(R.id.time_picker);
        int curHours = calendar.get(Calendar.HOUR_OF_DAY);
        int curMinutes = calendar.get(Calendar.MINUTE);
        hours.setCurrentItem(curHours);
        mins.setCurrentItem(curMinutes);

        //진동모드나 사일런트 모드일 때도 벨 울리게
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT //: 사일런트 모드일 경우(값0)
                || mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) { //: 진동모드일 경우(값1))
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
            mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
        }

        // add listeners
        addChangingListener(mins, "min");
        addChangingListener(hours, "hour");

        OnWheelChangedListener wheelListener = new OnWheelChangedListener() {

            public void onChanged(WheelView wheel, int oldValue, int newValue) {


                if (!timeScrolled) {
                    timeChanged = true;    //
                    mTime.setCurrentHour(hours.getCurrentItem());
                    mTime.setCurrentMinute(mins.getCurrentItem());
                    timeChanged = false;
                }
            }
        };

        hours.addChangingListener(wheelListener);
        mins.addChangingListener(wheelListener);
        OnWheelScrollListener scrollListener = new OnWheelScrollListener() {

            public void onScrollingStarted(WheelView wheel) {
                timeScrolled = true;//
            }

            public void onScrollingFinished(WheelView wheel) {
                timeScrolled = false;
                timeChanged = true; //
                mTime.setCurrentHour(hours.getCurrentItem());
                mTime.setCurrentMinute(mins.getCurrentItem());
                timeChanged = false;
            }
        };

        hours.addScrollingListener(scrollListener);
        mins.addScrollingListener(scrollListener);

        db_id = getIntent().getLongExtra("id", -1);
        if (db_id != -1) {
            mAlarmHour = getIntent().getIntExtra("hour", 12);
            mAlarmMinute = getIntent().getIntExtra("min", 33);
            mVibrate = getIntent().getIntExtra("vib", 0);
            strRingTone = getIntent().getStringExtra("ring");
            int day = getIntent().getIntExtra("day", 0);
            // 시간설정
            hours.setCurrentItem(mAlarmHour);
            mins.setCurrentItem(mAlarmMinute);

            //진동
            if (mVibrate == 0) {
                ((CheckBox) findViewById(R.id.alarm_set_vibrate)).setChecked(false);
            } else {
                ((CheckBox) findViewById(R.id.alarm_set_vibrate)).setChecked(true);
            }
            // ring tone
            if (strRingTone != null) {
                showRingTone(Uri.parse(strRingTone));
            }
            // 요일
            if ((day & 0x0001) == 0x0001) ((CheckBox) findViewById(R.id.sun)).setChecked(true);
            if ((day & 0x0002) == 0x0002) ((CheckBox) findViewById(R.id.mon)).setChecked(true);
            if ((day & 0x0004) == 0x0004) ((CheckBox) findViewById(R.id.tue)).setChecked(true);
            if ((day & 0x0008) == 0x0008) ((CheckBox) findViewById(R.id.wed)).setChecked(true);
            if ((day & 0x0010) == 0x0010) ((CheckBox) findViewById(R.id.thur)).setChecked(true);
            if ((day & 0x0020) == 0x0020) ((CheckBox) findViewById(R.id.fri)).setChecked(true);
            if ((day & 0x0040) == 0x0040) ((CheckBox) findViewById(R.id.sat)).setChecked(true);

        }

        View.OnClickListener myClickListener = new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                hideKeypad();
                switch (view.getId()){
                    case R.id.selectNews: break;
                    case R.id.selectRing: break;
                    case R.id.select_radio: break;
                    case R.id.ringtone: break;
                    case R.id.alarm_total: break;
                }
            }
        };
        mRadioGroup.setOnClickListener(myClickListener);
        mSelectNew.setOnClickListener(myClickListener);
        mSelectRing.setOnClickListener(myClickListener);

        layout.setOnClickListener(myClickListener);
    }


    public void hideKeypad(){
        View view = getCurrentFocus();
        if(view != null){
            InputMethodManager imm = (InputMethodManager)getBaseContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromInputMethod(view.getWindowToken(),0);

        }
    }


    //radioGroup Listener
    private RadioGroup.OnCheckedChangeListener radioGroupButtonChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int i) {
            if(i == R.id.selectNews){
                ringtoneView.setVisibility(View.GONE);
                strRingTone = null;
                newsView.setVisibility(View.VISIBLE);


            } else if(i == R.id.selectRing){
                newsView.setText("");
                newsKeyword = null;
                ringtoneView.setVisibility(View.VISIBLE);
                newsView.setVisibility(View.GONE);

            }
        }
    };

    protected void onDestroy() {
        super.onDestroy();

    }


    //알람 최종 설정
    private void setAlarm() {
        // TODO Auto-generated method stub

        //진동 설정
        if (((CheckBox) findViewById(R.id.alarm_set_vibrate)).isChecked()) {

            mVibrate = 1;
        } else {

            mVibrate = 0;
        }

        //////////////////요일////////////////
        int apday = 0;

        if (((CheckBox) findViewById(R.id.sun)).isChecked()) {
            apday |= 0x0001;
        }
        if (((CheckBox) findViewById(R.id.mon)).isChecked()) {
            apday |= 0x0002;
        }
        if (((CheckBox) findViewById(R.id.tue)).isChecked()) {
            apday |= 0x0004;
        }
        if (((CheckBox) findViewById(R.id.wed)).isChecked()) {
            apday |= 0x0008;
        }
        if (((CheckBox) findViewById(R.id.thur)).isChecked()) {
            apday |= 0x0010;
        }
        if (((CheckBox) findViewById(R.id.fri)).isChecked()) {
            apday |= 0x0020;
        }
        if (((CheckBox) findViewById(R.id.sat)).isChecked()) {
            apday |= 0x0040;
        }

        if (apday == 0) {
            Toast.makeText(getBaseContext(), "요일을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        //
        mAlarmHour = mTime.getCurrentHour();
        mAlarmMinute = mTime.getCurrentMinute();

        ///// db에 저장 /////
        DBAdapter db = new DBAdapter(alarmSet.this);
        db.open();
        // 알람 저장
        //db id가 -1이면 새로 설정 아니면 기존에 있던것을 수정
        if (db_id == -1) {
            db.addAlarm(1, apday, mAlarmHour, mAlarmMinute, mVibrate, strRingTone, newsKeyword);
        } else {
            db.modifyAlarm(db_id, 1, apday, mAlarmHour, mAlarmMinute, mVibrate, strRingTone, newsKeyword);
        }
        db.close();
        ////////////////////////////////////////
        Utility.startFirstAlarm(this);
        finish();

    }

    // Ringtone Manager 에게 선택 메세지 전송
    private OnClickListener ringSelectBtnListener = new OnClickListener() {

        public void onClick(View v) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
            startActivityForResult(intent, 123);
        }
    };

    //ringtone manager 벨소리 선택 메니져
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123 && resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                strRingTone = uri.toString();
                // ringtone 이름을 표시
                showRingTone(uri);
            }
        }
    }

    private void showRingTone(Uri uri) {
        Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
        if (ringtone != null) {
            String value = "벨소리" + "\n" + ringtone.getTitle(this);
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(value);
            ssb.setSpan(new ForegroundColorSpan(0xFFf4A460), 3, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ((TextView) findViewById(R.id.ringtone)).setText(ssb);
        } else {
        }

    }

    /**
     * Adds changing listener for wheel that updates the wheel label
     *
     * @param wheel the wheel
     * @param label the wheel label
     */
    private void addChangingListener(final WheelView wheel, final String label) {
        wheel.addChangingListener(new OnWheelChangedListener() {
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                wheel.setLabel(newValue != 1 ? label + "s" : label);
            }
        });
    }

}



