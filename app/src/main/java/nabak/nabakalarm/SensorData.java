package nabak.nabakalarm;

import android.app.Application;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class SensorData extends Application{
    private ArrayList<String> mSensorData = new ArrayList<String>();
    private int[] mNoiseCnt;


    public ArrayList<String> getmSensorData() {
        return mSensorData;
    }

    public int[] getmNoiseCnt(){
        return mNoiseCnt;
    }

    public void setmSensorData(String sensorData){
        this.mSensorData.add(sensorData);
    }

    public void setmNoiseCnt(int[] noiseCnt){
        this.mNoiseCnt = noiseCnt;
    }
}
