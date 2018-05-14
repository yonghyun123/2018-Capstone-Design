package nabak.nabakalarm;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.TabHost;

import com.nbpcorp.mobilead.sdk.MobileAdListener;
import com.nbpcorp.mobilead.sdk.MobileAdView;

public class NabakAlarmActivity extends TabActivity{
    /** Called when the activity is first created. */
	private MobileAdView adView = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    
        TabHost tabHost = getTabHost();    //tab을 가져온다.
              
        LayoutInflater.from(this).inflate(R.layout.main, tabHost.getTabContentView(), true);

        /** TabHost 에 포함된 Tab의 색깔을 모두 바꾼다, 개별적용 */
        
//        tabHost.addTab(tabHost.newTabSpec("낮잠")
//                .setIndicator("낮잠",
//                		getResources().getDrawable(R.drawable.afteral))
//                .setContent(new Intent(this, afternoonAlarm.class)));
        tabHost.addTab(tabHost.newTabSpec("알람")
                .setIndicator("알람", 
                		getResources().getDrawable(R.drawable.alar))
        		.setContent(new Intent(this, alarm.class)));
        /*
        tabHost.addTab(tabHost.newTabSpec("스탑워치")
                .setIndicator("스탑워치", 
                		getResources().getDrawable(R.drawable.stopwatch))
                .setContent(R.id.StopWatch));
                */

    }
    
    @Override
	protected void onDestroy() {
    	super.onDestroy();
    }

    
}

