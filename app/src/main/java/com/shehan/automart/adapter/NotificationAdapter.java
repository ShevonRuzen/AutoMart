package com.shehan.automart.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shehan.automart.R;
import com.shehan.automart.model.Notification;

import java.util.List;



public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notifications;
    private OnItemClickListener onItemClickListener;
    private OnRemoveClickListener onRemoveClickListener;


    public NotificationAdapter(List<Notification> notifications, OnItemClickListener onItemClickListener, OnRemoveClickListener onRemoveClickListener) {
        this.notifications = notifications;
        this.onItemClickListener = onItemClickListener;
        this.onRemoveClickListener = onRemoveClickListener;
    }

    @NonNull
    @Override
    public NotificationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        if (notification == null) return;

        if (notification.getTitle().equals("Order Status Updated") && notification.isRead()) {
            holder.unread.setVisibility(View.GONE);
            holder.read.setVisibility(View.VISIBLE);
        } else if (notification.getTitle().equals("Order Status Updated") && !notification.isRead()) {
            holder.unread.setVisibility(View.VISIBLE);
            holder.read.setVisibility(View.GONE);
        }

        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());
        holder.order_id.setText("Order ID: #" + notification.getOrder_id());

        if (notification.getCreated_at() != null) {
            long timeMillis = notification.getCreated_at().toDate().getTime();

            CharSequence relativeTime = android.text.format.DateUtils.getRelativeTimeSpanString(
                    timeMillis,
                    System.currentTimeMillis(),
                    android.text.format.DateUtils.MINUTE_IN_MILLIS
            );
            holder.time.setText(relativeTime);
        } else {
            holder.time.setText("");
        }


        if (notification.getTitle().equals("Order Status Updated")) {
            loadIcon(holder, notification.getStatus());
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int adapterPosition = holder.getAbsoluteAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(adapterPosition);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onRemoveClickListener != null) {
                int adapterPosition = holder.getAbsoluteAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onRemoveClickListener.onRemoveClick(adapterPosition);
                }
            }
            return true;
        });

    }

    private void loadIcon(ViewHolder holder, String status) {
        switch (status) {
            case "PENDING":
                holder.icon.setImageResource(R.drawable.pending);
                break;
            case "PROCESSING":
                holder.icon.setImageResource(R.drawable.handyman_24px);
                break;
            case "PICKED_UP":
                holder.icon.setImageResource(R.drawable.delivery_truck_speed_24px);
                break;
            case "DELIVERED":
                holder.icon.setImageResource(R.drawable.delivered);
                break;
            default:
                holder.icon.setImageResource(R.drawable.pending);
        }
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView icon, unread, read;
        TextView title, message, order_id, time;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.item_notification_icon);
            unread = itemView.findViewById(R.id.item_notification_unread);
            read = itemView.findViewById(R.id.item_notification_read);
            title = itemView.findViewById(R.id.item_notification_title);
            message = itemView.findViewById(R.id.item_notification_des);
            order_id = itemView.findViewById(R.id.item_notification_order_id);
            time = itemView.findViewById(R.id.item_notification_time);

        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnRemoveClickListener {
        void onRemoveClick(int position);
    }
}
