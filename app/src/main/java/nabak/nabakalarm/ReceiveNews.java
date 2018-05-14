package nabak.nabakalarm;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ReceiveNews extends AsyncTask<URL, Integer, Long>{
    private StringBuffer response = new StringBuffer();
    private ArrayList<String> titleList = new ArrayList<String>();
    private TextToSpeech myTTs;

    @Override
    protected Long doInBackground(URL... urls) {
        String clientId = "ilpLIa2JfHBeSABZlnwt";
        String clientSecret = "fasvJLMGQs";
        try{
            String text = URLEncoder.encode("그린팩토리","UTF-8");
            String apiURL = "https://openapi.naver.com/v1/search/news?query="+ text;
            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Naver-Client-Id", clientId);
            con.setRequestProperty("X-Naver-Client-Secret", clientSecret);

            int responseCode = con.getResponseCode();

            BufferedReader br;
            if(responseCode == 200){
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }

            String inputLine;

            while((inputLine = br.readLine()) != null){
                response.append(inputLine+"\n");
            }
            br.close();
            parseJson();


//            Log.i("네이버 api", response.toString());


        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Long aLong) {
        super.onPostExecute(aLong);
        Log.i("naver api",response.toString());
        Log.i("title", titleList.get(0));
        Log.i("title", titleList.get(1));
        Log.i("title", titleList.get(2));
        Log.i("title", titleList.get(3));
        Log.i("title", titleList.get(4));
    }

    void parseJson() throws JSONException {
        JSONObject obj = new JSONObject(response.toString());
        Log.i("obj", obj.toString());
        JSONArray items = obj.getJSONArray("items");

        for(int i = 0; i < items.length(); i++){
            JSONObject tmpObj = items.getJSONObject(i);
            titleList.add(tmpObj.getString("title"));
        }

    }

    public ArrayList<String> getTitleList() {
        return titleList;
    }
}
