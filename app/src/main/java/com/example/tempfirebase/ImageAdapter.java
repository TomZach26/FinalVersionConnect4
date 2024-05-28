package com.example.tempfirebase;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private int nrTiles;
    public ImageAdapter(Context c, int nrOfTiles) {
        mContext = c;
        this.nrTiles = nrOfTiles;
    }

    public int getCount() {
        return nrTiles;
    }
    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(95, 95));

            imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int pad = 10;
            imageView.setPadding(pad, pad, pad, pad);
        } else {
            imageView = (ImageView) convertView;
        }
        imageView.setImageResource(R.drawable.empty_t);
        return imageView;
    }





}