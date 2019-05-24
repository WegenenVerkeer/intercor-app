package macq.intercor.helpers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = "DownloadImageTask";
    private ImageView bmImage;

    public DownloadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        Bitmap bm = null;
        HttpURLConnection url;
        InputStream inputStream;
        try {
            url = (HttpURLConnection) new URL(urldisplay).openConnection();
            int maxStale = 60 * 60 * 24;
            url.addRequestProperty("Cache-Control", "max-stale=" + maxStale);
            // Get from cache
            inputStream = url.getInputStream();
            Log.i(TAG, "Found In Cache");
            bm = BitmapFactory.decodeStream(inputStream);
        } catch (FileNotFoundException error) {
            try {
                HttpURLConnection nonCachedUrlConnection = (HttpURLConnection) new URL(urldisplay).openConnection();
                inputStream = nonCachedUrlConnection.getInputStream();
                Log.i(TAG, "Not Found In Cache");
                bm = BitmapFactory.decodeStream(inputStream);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return bm;
    }

    protected void onPostExecute(Bitmap result) {
        this.bmImage.setImageBitmap(result);
    }
}
