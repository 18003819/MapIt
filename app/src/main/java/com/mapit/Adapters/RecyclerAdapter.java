package com.mapit.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mapit.Objects.Trip;
import com.mapit.R;
import java.util.List;

import static com.android.volley.VolleyLog.TAG;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ImageViewHolder>
{
    private List<Trip> mTrips;
    private OnItemClickListener mListener;

    public interface OnItemClickListener{
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        mListener = listener;
    }

    public RecyclerAdapter(Context context, List<Trip> trips){
        mTrips = trips;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_recycler_view, parent, false);
        return new ImageViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position)
    {
        holder.sLocation.setText(mTrips.get(position).getStartLocation());
        holder.eLocation.setText(mTrips.get(position).getEndLocation());
        holder.distance.setText(mTrips.get(position).getDistance());
        holder.duration.setText(mTrips.get(position).getDuration());
    }

    @Override
    public int getItemCount() {
        return mTrips.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        public TextView sLocation, eLocation, duration, distance;

        public ImageViewHolder(@NonNull View itemView, final OnItemClickListener listener) {
            super(itemView);
            sLocation = itemView.findViewById(R.id.startLocation);
            eLocation = itemView.findViewById(R.id.endLocation);
            distance = itemView.findViewById(R.id.distance_Recycler_Txt);
            duration = itemView.findViewById(R.id.duration_Recycler_Txt);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(listener != null){
                        int position = getAdapterPosition();
                        Log.d(TAG, "onClick: " + position);
                            listener.onItemClick(position);
                    }
                }
            });
        }
    }
}
