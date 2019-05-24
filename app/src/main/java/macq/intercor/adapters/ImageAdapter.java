package macq.intercor.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.ArrayList;

import macq.intercor.R;
import macq.intercor.helpers.DownloadImageTask;
import macq.intercor.helpers.PixelDpConverter;
import macq.intercor.models.Icon;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<Icon> icons;

    public ImageAdapter(Context c, ArrayList<Icon> icons) {
        this.icons = icons;
        mContext = c;
    }

    @Override
    public int getCount() {
        return this.icons.size();
    }

    @Override
    public Icon getItem(int position) {
        return this.icons.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(
                    new GridView.LayoutParams((int)PixelDpConverter.convertDpToPixel(80, mContext),
                            (int)PixelDpConverter.convertDpToPixel(80, mContext)));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackground(mContext.getDrawable(R.drawable.border));
            imageView.setCropToPadding(true);
        } else {
            imageView = (ImageView) convertView;
        }

        new DownloadImageTask(imageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, icons.get(position).getUrlIcon());
        return imageView;
    }
}
