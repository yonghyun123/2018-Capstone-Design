package nabak.nabakalarm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.nabak.movingview.MovingView;
import com.nbpcorp.mobilead.sdk.MobileAdListener;
import com.nbpcorp.mobilead.sdk.MobileAdView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class AlarmReceiver extends Activity implements TextToSpeech.OnInitListener {

    private String mRingTone = "";
    private String mNewsKeyword = "";
    private MediaPlayer mMediaPlayer = null;
    private Vibrator vibe = null;
    private PowerManager.WakeLock wl = null;
    private PowerManager pm;
    private LineChart lineChart;
    private StringBuffer response = new StringBuffer();
    private StringBuffer ttsResponse = new StringBuffer();
    private ArrayList<String> titleList = new ArrayList<String>();

    private TextToSpeech myTTs;

    // Notification Manager ???
    private GestureDetector mGestures = null;
    private NotificationManager nm = null;

    //??? ?????
    private AlarmManager mManager = null;

    //textView
    private TextView mNewsText;
    private SensorData mSensorData;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_result);
        mSensorData = ((SensorData) getApplicationContext());


        pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        if (!pm.isScreenOn()) {
            wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "NabakAlarm");
            wl.acquire();
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//										WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        }
        mNewsText = (TextView) findViewById(R.id.TestViewNews);
        mRingTone = getIntent().getStringExtra("ringtone");
        mNewsKeyword = getIntent().getStringExtra("newsKeyword");
        int vibrate = getIntent().getIntExtra("vibrate", 0);

        //make chart
        lineChart = (LineChart) findViewById(R.id.chart);


        myTTs = new TextToSpeech(this, this);

        if (mRingTone == null || mRingTone.equals("")) {
//			mRingTone = RingtoneManager.getValidRingtoneUri(this).toString();
            if (mRingTone == null) mRingTone = "";


        } else {
            showNotification(R.drawable.title_icon, "가", mRingTone, vibrate);
        }

        HtmlAsyncTask htmlAsyncTask = new HtmlAsyncTask();
        htmlAsyncTask.execute();
        mNewsText.setTextSize(20f);
        mNewsText.setText("당신은 코골이 의심이 없습니다.");
        mNewsText.setTextColor(Color.BLACK);
        makeChart();


//		Log.i("Reciever Activity1",mSensorData.getmSensorData().get(0));
//		Log.i("Reciever Activity2",mSensorData.getmSensorData().get(1));
//		Log.i("Reciever Activity3",mSensorData.getmSensorData().get(2));
//		Log.i("Reciever Activity4",mSensorData.getmSensorData().get(3));
//		Log.i("Reciever Activity5",mSensorData.getmSensorData().get(4));

    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////
	/*
		created by yonghyun
		2018.05.20
		make chart
	 */
    private void makeChart() {

        final List<Entry> entries = new ArrayList<Entry>();
        String msgForNnoise = "";
        int count_noise = 0;
        Date ctime;
        SimpleDateFormat hour = new SimpleDateFormat("hh");
        SimpleDateFormat min = new SimpleDateFormat("mm");
        int current_hour, current_min;
        int result, result_hour, result_min;

        if (mSensorData.getmSensorData().size() == 0) {
            entries.add(new Entry(1, 40));
            entries.add(new Entry(2, 50));
            entries.add(new Entry(3, 60));
            entries.add(new Entry(4, 40));
            entries.add(new Entry(5, 43));
            entries.add(new Entry(6, 20));
            entries.add(new Entry(7, 10));

        } else {
            ctime = new Date();
            current_hour = Integer.parseInt(hour.format(ctime));
            current_min = Integer.parseInt(min.format(ctime));
            for (int i = 0; i < mSensorData.getmSensorData().size(); i++) {
                float temp = Float.parseFloat(mSensorData.getmSensorData().get(i));
                if (temp > 35) entries.add(new Entry((float) i, temp));
                else count_noise++;
            }
            result = mSensorData.getmSensorData().size();
            result_hour = result / 60;    //result_hour 시간 result_min 분 동안 수면.
            result_min = result * 60;
        }

        String[] mValues;

        if (count_noise < 3)
            msgForNnoise = "당신의 코골이는 정상입니다.";
        else if (count_noise < 10)
            msgForNnoise = "당신의 코골이는 약한 수준입니다.";
        else if (count_noise < 20)
            msgForNnoise = "당신의 코골이는 보통 수준입니다.";
        else
            msgForNnoise = "당신의 코골이는 심각합니다.";
        mNewsText.setText(msgForNnoise);


        LineDataSet lineDataSet = new LineDataSet(entries, "시");
        lineDataSet.setLineWidth(2);
        lineDataSet.setCircleRadius(6);
        lineDataSet.setCircleColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setCircleColorHole(Color.BLUE);
        lineDataSet.setColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawHorizontalHighlightIndicator(false);
        lineDataSet.setDrawHighlightIndicators(false);
        lineDataSet.setDrawValues(false);

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        Legend legend = lineChart.getLegend();
        legend.setTextColor(Color.parseColor("#ffffff"));
        legend.setTextSize(Float.parseFloat("20f"));

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.disableAxisLineDashedLine();
//      xAxis.enableGridDashedLine(8, 24, 0);

        YAxis yLAxis = lineChart.getAxisLeft();
        yLAxis.setTextColor(Color.WHITE);

        YAxis yRAxis = lineChart.getAxisRight();
        yRAxis.setDrawLabels(false);
        yRAxis.setDrawAxisLine(false);
        yRAxis.setDrawGridLines(false);

        Description description = new Description();
        description.setText("");

        lineChart.setDoubleTapToZoomEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setDescription(description);
        lineChart.animateY(2000, Easing.EasingOption.EaseInCubic);
        lineChart.invalidate();
    }


    private void ttsGreater21(String str) {
        myTTs.speak(str, TextToSpeech.QUEUE_FLUSH, null);
    }


    private void showNotification(int statusBarIconID, String statusBatTextID, String ringtone, int vibrate) {
        nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        playSound(Uri.parse(mRingTone));

        if (vibrate == 1) {
            vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] pattern = {200, 2000, 100, 1700, 200, 2000, 100, 1700, 200, 2000, 100, 1700};
            vibe.vibrate(pattern, 2);
        }

        show(); //alert
    }

    protected void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알람을 종료하시겠습니까?");
        builder.setPositiveButton("",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
//						clearAlarm();
                        if (mMediaPlayer != null) {
                            mMediaPlayer.stop();
                        }
                        if (vibe != null) {
                            vibe.cancel();
                        }
                        nm.cancel(12345);

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void playSound(Uri alert) {
        if (mMediaPlayer != null) return;

        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, alert);
            final AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.prepare();
                mMediaPlayer.setLooping(true);
                mMediaPlayer.start();
            }
        } catch (IOException e) {

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestures != null) {
            return mGestures.onTouchEvent(event);
        } else {
            return super.onTouchEvent(event);
        }
    }


    /*
    created by yonghyun 2018.5.4
    description: using naver search API in background thread
    if don't use alarm keyword, default input is ???????
     */
    class HtmlAsyncTask extends AsyncTask<URL, Integer, Long> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Long doInBackground(URL... urls) {
            String clientId = "ilpLIa2JfHBeSABZlnwt";
            String clientSecret = "fasvJLMGQs";
            try {
                String text = null;
                if (mNewsKeyword != null) {
                    text = URLEncoder.encode(mNewsKeyword, "UTF-8");
                } else {
                    text = URLEncoder.encode("korea", "UTF-8");
                }
                String apiURL = "https://openapi.naver.com/v1/search/news?query=" + text;
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);

                int responseCode = con.getResponseCode();

                BufferedReader br;
                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                }

                String inputLine;

                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine + "\n");
                }
                br.close();
                parseJson();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }

        protected void onPostExecute(Long result) {
            super.onPostExecute(result);
//
//			mNewsText.setText(ttsResponse);
            ttsGreater21(ttsResponse.toString());
        }

        private void parseJson() throws JSONException {
            JSONObject obj = new JSONObject(response.toString());
            JSONArray items = obj.getJSONArray("items");

            for (int i = 0; i < items.length(); i++) {
                JSONObject tmpObj = items.getJSONObject(i);
                titleList.add(tmpObj.getString("title"));
                if (tmpObj.getString("title").contains("&") || tmpObj.getString("title").contains(";"))
                    continue;
                ttsResponse.append(i + 1 + "번        째 뉴스      " + getString(tmpObj.getString("title")) + "  ");
            }
            Log.i("ttsResponse", ttsResponse.toString());
        }

        private String getString(String str) {

            String match = "[^\uAC00-\uD7A3xfe0-9a-zA-Z\\s]";
            str = str.replaceAll(match, " ");
            return str;
        }
    }

    @Override
    //TTS init method
    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS) {
            myTTs.setLanguage(Locale.KOREAN);
            ttsGreater21(ttsResponse.toString());
        } else {
            Log.i("TTS", "fail!!!!!!!!!!!");
        }
    }


    //
    @Override
    protected void onResume() {
        super.onResume();
    }

    //
    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (myTTs != null) {
            myTTs.stop();
            myTTs.shutdown();
        }
        Utility.startFirstAlarm(this);
    }
}




	