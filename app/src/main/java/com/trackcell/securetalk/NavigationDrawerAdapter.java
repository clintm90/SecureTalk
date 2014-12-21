package com.trackcell.securetalk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class NavigationDrawerAdapter extends ArrayAdapter<EnumNavigationDrawer>
{
    private final Context mContext;
    private final List<EnumNavigationDrawer> values;

    public NavigationDrawerAdapter(Context context, List<EnumNavigationDrawer> values)
    {
        super(context, R.layout.model_navigationdrawer, values);
        this.mContext = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.model_navigationdrawer, parent, false);
        rowView.setTag(values.get(position));

        TextView mNavigationDrawerName = (TextView)rowView.findViewById(R.id.model_navigationdrawer_name);
        ImageView mNavigationDrawerPhoto = (ImageView)rowView.findViewById(R.id.model_navigationdrawer_photo);
        mNavigationDrawerName.setText(values.get(position).Name);
        mNavigationDrawerPhoto.setImageDrawable(values.get(position).Photo);

        return rowView;
    }
}