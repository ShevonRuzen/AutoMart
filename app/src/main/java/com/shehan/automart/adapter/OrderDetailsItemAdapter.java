package com.shehan.automart.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shehan.automart.R;
import com.shehan.automart.model.Order_item;
import com.shehan.automart.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;



public class OrderDetailsItemAdapter extends RecyclerView.Adapter<OrderDetailsItemAdapter.ViewHolder> {
    private List<Order_item> orderItems = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseStorage storage;


    public OrderDetailsItemAdapter(List<Order_item> orderItemList) {
        this.orderItems = orderItemList;

    }

    @NonNull
    @Override
    public OrderDetailsItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_details, parent, false);


        this.db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderDetailsItemAdapter.ViewHolder holder, int position) {
        Order_item orderItem = orderItems.get(position);
        if (orderItem.getProduct_doc_id() != null) {

            db.collection("product")
                    .document(orderItem.getProduct_doc_id())
                    .get()
                    .addOnSuccessListener(ds -> {
                        if (!ds.exists() || ds.toObject(Product.class) == null) return;

                        Product product = ds.toObject(Product.class);
                        assert product != null;
                        holder.name.setText(product.getName() != null ? product.getName() : "");

                        int qty = (int) orderItem.getQty();
                        holder.qty.setText(String.valueOf(qty));

                        holder.subtotal.setText(String.format(Locale.US, "LKR. %,.2f", orderItem.getSubtotal()));

                        StorageReference folderRef = storage.getReference().child(product.getImage());

                        folderRef.listAll().addOnSuccessListener(listResult -> {

                            if (!listResult.getItems().isEmpty()) {

                                StorageReference firstImage = listResult.getItems().get(0);

                                firstImage.getDownloadUrl().addOnSuccessListener(uri -> {
                                    if (!holder.itemView.isAttachedToWindow()) {
                                        return;
                                    }

                                    Glide.with(holder.itemView)
                                            .load(uri)
                                            .centerCrop()
                                            .into(holder.img);
                                });
                            }

                        });


                    });
        }
    }

    @Override
    public int getItemCount() {
        return orderItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView img;
        TextView name, qty, subtotal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            img = itemView.findViewById(R.id.item_order_details_img);
            name = itemView.findViewById(R.id.item_order_details_name);
            qty = itemView.findViewById(R.id.item_order_details_qty);
            subtotal = itemView.findViewById(R.id.item_order_details_subtotal);
        }
    }
}
