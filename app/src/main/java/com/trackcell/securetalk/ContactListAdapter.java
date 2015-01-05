package com.trackcell.securetalk;

import android.content.Context;
import android.text.Html;
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

        final ImageView mContactListIcon = (ImageView)rowView.findViewById(R.id.model_contactList_icon);
        TextView mContactListTitle = (TextView)rowView.findViewById(R.id.model_contactList_title);
        TextView mContactListDescription = (TextView)rowView.findViewById(R.id.model_contactList_description);
        ImageView mContactListArrow = (ImageView)rowView.findViewById(R.id.model_contactList_arrow);
        TextView mContactListStatus = (TextView)rowView.findViewById(R.id.model_contactList_status);

        mContactListArrow.setVisibility(values.get(position).ArrowVisibility);

        mContactListIcon.setVisibility(values.get(position).PhotoVisibility);

        if(values.get(position).PhotoVisibility == View.VISIBLE)
        {
            Landing.LoadGravatar(mContext, mContactListIcon, values.get(position).ID, values.get(position).isPhotoBW);
        }

        if(values.get(position).isNewMessage)
        {
            mContactListTitle.setText(Html.fromHtml(values.get(position).Name + "&nbsp;<b><font color=\"red\">*</font></b>"));
        }
        else
        {
            mContactListTitle.setText(values.get(position).Name);
        }

        mContactListDescription.setText(values.get(position).Description, TextView.BufferType.SPANNABLE);

        if(values.get(position).isPaddingRight)
        {
            mContactListTitle.setPadding(0, 7, 0, 0);
            mContactListDescription.setPadding(0, 0, 40, 0);
        }
        else
        {
            ViewGroup.LayoutParams params = mContactListTitle.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            mContactListTitle.setLayoutParams(params);
            mContactListTitle.setPadding(0, 7, 5, 0);
            mContactListDescription.setPadding(0, 0, 5, 0);
        }

        if(values.get(position).ArrowSelectable)
        {
            mContactListArrow.setImageDrawable(parent.getResources().getDrawable(R.drawable.ic_action_arrow_valid));
        }

        if(values.get(position).isSingleLine)
        {
            mContactListDescription.setMaxLines(1);
        }

        rowView.setTag(values.get(position));

        return rowView;
    }
}