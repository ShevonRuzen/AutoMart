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
import com.shehan.automart.model.Product;
import com.shehan.automart.model.SpecialMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeaturedItemAdapter extends RecyclerView.Adapter<FeaturedItemAdapter.ViewHolder> {
    private List<SpecialMenu> featuredList = new ArrayList<>();
    private FirebaseStorage storage;
    private OnclickListener listener;
    private FirebaseFirestore db;

    public FeaturedItemAdapter(List<SpecialMenu> featuredList, OnclickListener listener) {
        this.featuredList = featuredList;
        storage = FirebaseStorage.getInstance();
        this.listener = listener;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public FeaturedItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeaturedItemAdapter.ViewHolder holder, int position) {
        SpecialMenu sm = featuredList.get(position);

        db.collection("product").document(sm.getProduct_doc_id()).get().addOnSuccessListener(ds -> {
            if (ds.exists() && ds.getData() != null){
                Product product = ds.toObject(Product.class);
                if (product != null){
                    holder.name.setText(product.getName());
                    holder.price.setText(String.format(Locale.US, "LKR. %,.2f", product.getPrice()));
                    holder.dec.setText(product.getDescription());
                    holder.rating.setText("4.5");

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


                    holder.itemView.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onItemClick(product);
                        }
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return featuredList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView img;

        TextView name, dec, price, rating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.item_featured_image);
            name = itemView.findViewById(R.id.item_featured_name);
            dec = itemView.findViewById(R.id.item_featured_des);
            price = itemView.findViewById(R.id.item_featured_price);
            rating = itemView.findViewById(R.id.item_product_rating);
        }
    }

    public interface OnclickListener {
        void onItemClick(Product product);
    }
}
