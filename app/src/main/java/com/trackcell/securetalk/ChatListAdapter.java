package com.trackcell.securetalk;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ChatListAdapter extends ArrayAdapter<EnumChat>
{
    private int[] colors = new int[] { 0x30FF0000, 0x300000FF };
    private final Context context;
    private final List<EnumChat> values;

    public ChatListAdapter(Context context, List<EnumChat> values)
    {
        super(context, R.layout.model_chatlist, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.model_chatlist, parent, false);
        rowView.setTag(values.get(position));

        ImageView mChatListOther = (ImageView)rowView.findViewById(R.id.model_chatList_other);
        TextView mChatListTitle = (TextView)rowView.findViewById(R.id.model_chatList_title);
        TextView mChatListTimestamp = (TextView)rowView.findViewById(R.id.model_chatList_timestamp);
        ImageView mChatListPhoto = (ImageView)rowView.findViewById(R.id.model_chatlist_photo);

        mChatListTitle.setText(values.get(position).Title);

        String elapsedTime = (String) DateUtils.getRelativeTimeSpanString(values.get(position).Timestamp);
        mChatListTimestamp.setText(elapsedTime);

        if(values.get(position).isPhoto)
        {
            mChatListPhoto.setVisibility(View.VISIBLE);
            mChatListPhoto.setImageDrawable(new BitmapDrawable(this.context.getResources(), ThumbnailUtils.extractThumbnail(values.get(position).Photo, 50, 50)));
        }

        if(values.get(position).Error)
        {
            mChatListOther.setImageDrawable(parent.getResources().getDrawable(R.drawable.ic_info_error));
            //chatListOther.setText("Erreur de l'envoi");
            //chatListOther.setTextColor(parent.getResources().getColor(R.color.red));
        }

        if(values.get(position).isMe)
        {
            rowView.setBackgroundResource(R.drawable.balloon);
            //((RelativeLayout)rowView).setGravity(Gravity.END);
        }
        else
        {
            rowView.setBackgroundResource(R.drawable.balloon_right);
        }

        return rowView;
    }
}