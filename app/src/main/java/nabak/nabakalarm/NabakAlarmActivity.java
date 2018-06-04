package nabak.nabakalarm;

import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Color;
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
        tabHost.getTabWidget().getChildTabViewAt(0).setBackgroundColor(Color.parseColor("#292929"));

    }
    
    @Override
	protected void onDestroy() {
    	super.onDestroy();
    }

    
}

