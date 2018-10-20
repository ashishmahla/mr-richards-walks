package in.scarecrow.mrrichardswalks.models;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.model.Marker;

import java.util.List;

import in.scarecrow.mrrichardswalks.R;

/**
 * Adapter template created by Ashish Mahla
 */

public class WayPointAdapter extends RecyclerView.Adapter<WayPointAdapter.MyViewHolder> {

    private WayPointClickListener mWayPointClickListener;
    private List<Marker> mWayPointList;

    public WayPointAdapter(List<Marker> mWayPointList, WayPointClickListener mWayPointClickListener) {
        this.mWayPointList = mWayPointList;
        this.mWayPointClickListener = mWayPointClickListener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.model_waypoint, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
        final Marker waypoint = mWayPointList.get(position);
        holder.name.setText(waypoint.getTitle());

        if (position == 0) {
            holder.pin.setColorFilter(Color.GREEN);
        } else if (position == getItemCount() - 1) {
            holder.pin.setColorFilter(Color.RED);
        } else {
            holder.pin.setColorFilter(Color.BLUE);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mWayPointList.size();
    }

    public interface WayPointClickListener {
        void onWayPointClick(View view, int position);
    }

    class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView pin, close;
        TextView name;

        MyViewHolder(View itemView) {
            super(itemView);

            // find and init all layout views here
            pin = itemView.findViewById(R.id.iv_pin);
            close = itemView.findViewById(R.id.iv_remove_waypoint);
            name = itemView.findViewById(R.id.tv_wp_name);

            if (mWayPointClickListener != null) {
                close.setOnClickListener(this);
            }
        }

        @Override
        public void onClick(View v) {
            mWayPointClickListener.onWayPointClick(v, getLayoutPosition());
        }
    }
}