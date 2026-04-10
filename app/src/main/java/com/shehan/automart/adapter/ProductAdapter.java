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
import com.shehan.automart.helper.FavouriteManager;
import com.shehan.automart.model.Product;

import java.util.List;
import java.util.Locale;



public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> productList;
    private FirebaseStorage storage;

    private OnProductClickListener productClickListener;
    private OnFavClickListener favClickListener;


    public ProductAdapter(List<Product> productList, OnProductClickListener productClickListener, OnFavClickListener favClickListener) {
        this.productList = productList;
        storage = FirebaseStorage.getInstance();
        this.productClickListener = productClickListener;
        this.favClickListener = favClickListener;
    }

    public void updateList(List<Product> newList){
        this.productList.clear();
        this.productList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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


        if (FavouriteManager.isFav(holder.itemView.getContext(), product.getId())) {
            holder.fav.setImageResource(R.drawable.favorite_24px);
        } else {
            holder.fav.setImageResource(R.drawable.favorite_24px_outline);
        }

        holder.itemView.setOnClickListener(v -> {
            if (productClickListener != null) {
                productClickListener.onProductClick(product);
            }
        });

        holder.fav.setOnClickListener(v -> {
            boolean isNowFav = FavouriteManager.toggleFav(holder.itemView.getContext(), product.getId());
            if (isNowFav) {
                holder.fav.setImageResource(R.drawable.favorite_24px);
            } else {
                holder.fav.setImageResource(R.drawable.favorite_24px_outline);
            }

            if (favClickListener != null) {
                favClickListener.onFavClick(product);
            }
        });

    }

    @Override
    public int getItemCount() {
        return productList == null ? 0 : productList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView productImage, fav;
        private TextView productName, productPrice, productRating;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.item_product_image);
            productName = itemView.findViewById(R.id.item_product_name);
            productPrice = itemView.findViewById(R.id.item_product_price);
            productRating = itemView.findViewById(R.id.item_product_rating);
            fav = itemView.findViewById(R.id.item_product_fav);
        }
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnFavClickListener {
        void onFavClick(Product product);
    }
}
