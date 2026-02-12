package com.example.nivgilboaapp;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class YouTubeVideoAdapter extends RecyclerView.Adapter<YouTubeVideoAdapter.VH> {

    public interface OnPickListener {
        void onPick(YouTubeVideoItem item);
    }

    private final List<YouTubeVideoItem> items;
    private final OnPickListener listener;

    public YouTubeVideoAdapter(List<YouTubeVideoItem> items, OnPickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_youtube_video, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        YouTubeVideoItem item = items.get(position);
        h.tvTitle.setText(item.title);
        h.tvChannel.setText(item.channelTitle);
        h.itemView.setOnClickListener(v -> listener.onPick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvChannel;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvChannel = itemView.findViewById(R.id.tvChannel);
        }
    }
}
