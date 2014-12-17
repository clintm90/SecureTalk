package com.trackcell.securetalk;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ContactListAdapter extends ArrayAdapter<EnumContact>
{

    private final Context mContext;
    private final List<EnumContact> values;

    public ContactListAdapter(Context context, List<EnumContact> values)
    {
        super(context, R.layout.model_contactlist, values);
        this.mContext = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.model_contactlist, parent, false);

        final ImageView contactListIcon = (ImageView)rowView.findViewById(R.id.model_contactList_icon);
        TextView contactListTitle = (TextView)rowView.findViewById(R.id.model_contactList_title);
        TextView contactListDescription = (TextView)rowView.findViewById(R.id.model_contactList_description);
        ImageView contactListArrow = (ImageView)rowView.findViewById(R.id.model_contactList_arrow);

        contactListArrow.setVisibility(values.get(position).ArrowVisibility);

        contactListIcon.setVisibility(values.get(position).PhotoVisibility);

        if(values.get(position).PhotoVisibility == View.VISIBLE)
        {
            Landing.LoadGravatar(mContext, contactListIcon, values.get(position).ID, values.get(position).isPhtoBW);
        }

        //if(values.get(position).Bold)
            //contactListTitle.setTypeface(Typeface.DEFAULT_BOLD);

        contactListTitle.setText(values.get(position).Name);
        contactListDescription.setText(values.get(position).Description);

        if(values.get(position).isPaddingRight)
        {
            contactListTitle.setPadding(0, 7, 40, 0);
            contactListDescription.setPadding(0, 0, 40, 0);
        }
        else
        {
            contactListTitle.setPadding(0, 7, 5, 0);
            contactListDescription.setPadding(0, 0, 5, 0);
        }

        if(values.get(position).ArrowSelectable)
        {
            contactListArrow.setImageDrawable(parent.getResources().getDrawable(R.drawable.ic_action_arrow_valid));
        }

        if(values.get(position).isSingleLine)
        {
            contactListDescription.setMaxLines(1);
        }

        rowView.setTag(values.get(position));

        return rowView;
    }
}