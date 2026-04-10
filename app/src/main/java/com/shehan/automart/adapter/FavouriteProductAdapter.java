package com.shehan.automart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shehan.automart.R;
import com.shehan.automart.model.Product;

import java.util.List;
import java.util.Locale;



public class FavouriteProductAdapter extends RecyclerView.Adapter<FavouriteProductAdapter.ViewHolder> {

    private List<Product> productList;
    private FirebaseStorage storage;

    private OnProductClickListener productClickListener;
    private OnFavRemoveListener removeListener;


    public FavouriteProductAdapter(List<Product> productList, OnProductClickListener productClickListener, OnFavRemoveListener removeListener) {
        this.productList = productList;
        this.productClickListener = productClickListener;
        this.removeListener = removeListener;
        storage =FirebaseStorage.getInstance();
    }


    @NonNull
    @Override
    public FavouriteProductAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fav_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavouriteProductAdapter.ViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.productName.setText(product.getName());
        holder.productPrice.setText(String.format(Locale.US, "LKR. %,.2f", product.getPrice()));
        holder.productRating.setText("4.5");

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
                            .into(holder.productImage);
                });
            }

        });

        holder.itemView.setOnClickListener(v -> {
            if (productClickListener != null) {
                productClickListener.onProductClick(product);
            }
        });

        holder.remove.setOnClickListener(v -> {
            if (removeListener != null) {
                int adapterPosition = holder.getAbsoluteAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    removeListener.onRemoveClick(adapterPosition);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return productList == null ? 0 : productList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView productImage, remove;
        private TextView productName, productPrice, productRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            productImage = itemView.findViewById(R.id.item_fav_product_image);
            productName = itemView.findViewById(R.id.item_fav_product_name);
            productPrice = itemView.findViewById(R.id.item_fav_product_price);
            productRating = itemView.findViewById(R.id.item_fav_product_rating);
            remove = itemView.findViewById(R.id.item_fav_product_remove);
        }
    }


    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnFavRemoveListener {
        void onRemoveClick(int position);
    }
}
