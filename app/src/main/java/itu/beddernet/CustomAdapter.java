/**
 *CustomAdapterFix!!    http://androidadapternotifiydatasetchanged.blogspot.de/
 *                      http://stackoverflow.com/questions/8121476/how-to-setonclicklistener-on-the-button-inside-the-listview
 */

package itu.beddernet;
import java.util.List;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
public class CustomAdapter extends ArrayAdapter {
    static final public String COPA_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";
    static final public String COPA_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";
    LocalBroadcastManager broadcaster;

    private Activity mContext;
    private List<String> mList;
    private LayoutInflater mLayoutInflater = null;
    private ImageButton paperPlaneButton;

    public void sendResult(String message) {
        Intent intent = new Intent(COPA_RESULT);
        if(message != null)
            intent.putExtra(COPA_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

    public CustomAdapter(Activity context, int resource, List objects) {
        super(context, resource, objects);
        mList=objects;
        mContext=context;
        mLayoutInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        broadcaster = LocalBroadcastManager.getInstance(context);
    }

    @Override
    public int getCount() {
        return mList.size();
    }
    @Override
    public Object getItem(int pos) {
        return mList.get(pos);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        CompleteListViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater li = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.mylist, null);
            viewHolder = new CompleteListViewHolder(v);
            v.setTag(viewHolder);
        } else {
            viewHolder = (CompleteListViewHolder) v.getTag();
        }
        viewHolder.mTVItem.setText(mList.get(position));

        /*Here comes the paperflyer :P ( paper plane ;) ) */
        paperPlaneButton = (ImageButton) v.findViewById(R.id.paperflyButton);
        //Stores the number in the PlaneButton
        paperPlaneButton.setTag(position);
        paperPlaneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendResult("#"+view.getTag().toString());
                Log.e("xx","I was clicked!!---!!!---!!!---!!!---!!!");
            }
        });

        return v;
    }
}
class CompleteListViewHolder {
    public TextView mTVItem;
    public CompleteListViewHolder(View base) {
        mTVItem = (TextView) base.findViewById(R.id.Itemname);
    }
}