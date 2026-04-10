package com.shehan.automart.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shehan.automart.R;
import com.shehan.automart.model.CartItems;
import com.shehan.automart.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private List<CartItems> cartItemsList;
    private FirebaseFirestore db;
    private Map<String, Product> cartItemProducts = new HashMap<>();
    private Map<String, String> availabilityList = new HashMap<>();
    private Map<String, String> sizeList = new HashMap<>();
    private FirebaseStorage storage;
    private OnQtyChangeListener onQtyChangeListener;
    private RemoveListener removeListener;


    public CartAdapter(List<CartItems> cartItemsList) {
        this.cartItemsList = cartItemsList;

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        loadProducts();
        loadAvailability();
        loadColor();

    }

    public void setOnQtyChangeListener(OnQtyChangeListener onQtyChangeListener) {
        this.onQtyChangeListener = onQtyChangeListener;

    }

    public void setRemoveListener(RemoveListener removeListener) {
        this.removeListener = removeListener;

    }

    private void loadColor() {
        db.collection("color").get().addOnSuccessListener(qds -> {
            if (qds == null || qds.isEmpty()) return;

            qds.getDocuments().forEach(ds -> {
                sizeList.put(ds.get("id").toString(), ds.get("color").toString());
            });

            notifyDataSetChanged();
        });
    }

    private void loadProducts() {
        // get product list
        cartItemProducts.clear();

        List<String> productIds = new ArrayList<>();
        for (CartItems cartItem : cartItemsList) {
            productIds.add(cartItem.getProduct_id());
        }

        if (productIds == null || productIds.isEmpty()) return;

        db.collection("product").whereIn("id", productIds).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) return;

            queryDocumentSnapshots.getDocuments().forEach(ds -> {
                for (CartItems cartItem : cartItemsList) {
                    if (cartItem.getProduct_id().equals(ds.get("id"))) {
                        cartItemProducts.put(cartItem.getProduct_id(), ds.toObject(Product.class));
                    }
                }
            });
            notifyDataSetChanged();
        });
    }

    private void loadAvailability() {
        // get availability list
        db.collection("status").get().addOnSuccessListener(qds -> {
            if (qds == null || qds.isEmpty()) return;

            qds.getDocuments().forEach(ds -> {
                availabilityList.put(ds.get("id").toString(), ds.get("type").toString());
            });
            notifyDataSetChanged();
        });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItems cartItem = cartItemsList.get(position);

        Product product = cartItemProducts.get(cartItem.getProduct_id());


        if (product == null) {
            holder.productName.setText("Loading...");
            holder.subtotal.setText("LKR. 0.00");
            holder.color.setText("");
            holder.image.setImageDrawable(null);
            return;
        }


        holder.qty.setText(String.valueOf(((int) cartItem.getQty())));
        holder.productName.setText(product.getName());
        holder.subtotal.setText(String.format("LKR. %,.2f", product.getPrice() * cartItem.getQty()));
        holder.color.setText(sizeList.get(product.getColor_id()));


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
                            .into(holder.image);
                });
            }

        });

        int currentPosition = holder.getAbsoluteAdapterPosition();
        if (currentPosition == RecyclerView.NO_POSITION) {
            return;
        }

        holder.minusBtn.setOnClickListener(v -> {
            if (cartItem.getQty() > 1) {
                cartItem.setQty(cartItem.getQty() - 1);
                notifyItemChanged(currentPosition);
                if (onQtyChangeListener != null) {
                    onQtyChangeListener.onQtyChange(cartItem);
                }
            }
        });

        holder.plusBtn.setOnClickListener(v -> {
            if (cartItem.getQty() < 100) {
                cartItem.setQty(cartItem.getQty() + 1);
                notifyItemChanged(currentPosition);
                if (onQtyChangeListener != null) {
                    onQtyChangeListener.onQtyChange(cartItem);
                }
            }
        });

        holder.removeBtn.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onRemove(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView image, removeBtn;
        private TextView productName, subtotal, color, qty;
        private Button plusBtn, minusBtn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.item_cart_image);
            removeBtn = itemView.findViewById(R.id.item_cart_remove);
            productName = itemView.findViewById(R.id.item_cart_product_name);
            subtotal = itemView.findViewById(R.id.item_cart_subtotal);
            color = itemView.findViewById(R.id.item_cart_color);
            qty = itemView.findViewById(R.id.item_cart_quantity);
            plusBtn = itemView.findViewById(R.id.item_cart_qty_plus);
            minusBtn = itemView.findViewById(R.id.item_cart_qty_minus);
        }
    }


    public interface OnQtyChangeListener {
        void onQtyChange(CartItems cartItem);
    }

    public interface RemoveListener {
        void onRemove(int position);
    }
}
