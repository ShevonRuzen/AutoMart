package com.shehan.automart.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.R;
import com.shehan.automart.model.Order;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;



public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<Order> orderList = new ArrayList<>();
    private OnItemClickListener listener;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public OrderAdapter(List<Order> orderList, OnItemClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public OrderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderAdapter.ViewHolder holder, int position) {
        Order order = orderList.get(position);


        if (mAuth == null || mAuth.getCurrentUser() == null) return;

        holder.order_staus.setText(order.getOrder_status() != null ? order.getOrder_status() : "");
        holder.order_id.setText(order.getOrder_id() != null ? "Order ID: #" + order.getOrder_id() : "Order ID: #");
        if (order.getCreated_date() != null) {
            Timestamp timestamp = order.getCreated_date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String date = dateFormat.format(timestamp.toDate());
            holder.order_date.setText(date);
        }else {
            holder.order_date.setText("");
        }
        holder.order_address.setText(String.format("%s, %s", order.getAddress_home(), order.getAddress_city()));

        db.collection("order_item")
                .whereEqualTo("order_doc_id", order.getOrder_doc_id())
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds == null || qds.isEmpty()) {
                        holder.order_item_count_subtotal.setText("");
                        return;
                    }
                    int count = qds.size();
                    holder.order_item_count_subtotal
                            .setText(new StringBuilder()
                                    .append(count)
                                    .append(" items • ")
                                    .append(String.format(Locale.US, "LKR. %,.2f", order.getTotal_amount())).toString());

                }).addOnFailureListener(e -> {
                    holder.order_item_count_subtotal.setText("");
                });

        holder.order_track_btn.setOnClickListener(v -> {
            if (listener != null) {
                int adapterPosition = holder.getAbsoluteAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION){
                    listener.onItemClick(adapterPosition);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView order_staus, order_id, order_date, order_address, order_item_count_subtotal;

        ImageButton order_track_btn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            order_staus = itemView.findViewById(R.id.item_order_status);
            order_id = itemView.findViewById(R.id.item_order_id);
            order_date = itemView.findViewById(R.id.item_order_time);
            order_address = itemView.findViewById(R.id.item_order_address);
            order_item_count_subtotal = itemView.findViewById(R.id.item_order_item_count_subtotal);
            order_track_btn = itemView.findViewById(R.id.item_order_track_btn);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
