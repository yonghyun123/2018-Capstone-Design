package nabak.nabakalarm;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;


public class ChartMarker extends MarkerView {
    private TextView tvContent;

    public ChartMarker(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = (TextView)findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {

        if (e instanceof CandleEntry) {

            CandleEntry ce = (CandleEntry) e;
            tvContent.setText("" + Utils.formatNumber(ce.getHigh(), 0, true));
        } else {
            String heartRate = Utils.formatNumber(e.getY(),0,true);
            setHeartMarker(Integer.parseInt(heartRate));
        }

        super.refreshContent(e, highlight);
    }

    public void setHeartMarker(int heartRate){
        String comments = "";
         if (heartRate > 145) {
            comments = "심장 강화 피트니스 구간 ";
        } else if (heartRate > 120) {
            comments = "지방 연소 구간 ";
        } else if (heartRate > 80) {
            comments = "안정적인 구간 ";
        } else {
            comments = "서맥 구간 ";
        }
        tvContent.setText(comments + Utils.formatNumber(heartRate,0,true));
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}
