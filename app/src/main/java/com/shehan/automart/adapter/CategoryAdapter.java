package com.shehan.automart.adapter;


import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shehan.automart.R;
import com.shehan.automart.model.Category;

import java.util.List;


public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private int selectedPosition = -1;
    private OnCategoryClickListener categoryClickListener;
    private List<Category> categoryList;


    public CategoryAdapter(List<Category> categoryList, OnCategoryClickListener categoryClickListener) {

        this.categoryList = categoryList;
        this.categoryClickListener = categoryClickListener;
    }

    @NonNull
    @Override
    public CategoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_item_category, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        Category category = categoryList.get(position);
        Glide.with(holder.itemView.getContext())
                .load(category.getImage())
                .circleCrop()
                .into(holder.catImage);
        holder.catName.setText(category.getName());

//        if (selectedPosition == position) {
////            holder.ring.setBackgroundResource(R.drawable.home_item_category_select_background);
//        } else {
//            holder.ring.setBackgroundResource(R.drawable.home_item_category_background);
//        }

        holder.itemView.setOnClickListener(v -> {

            int previousPosition = selectedPosition;
            selectedPosition = position;

            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            if (categoryClickListener != null) {
                categoryClickListener.onCategoryClick(position);
            }
        });

        Log.i("category", category.getName());
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView catImage;
        private TextView catName;
        private FrameLayout ring;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            catImage = itemView.findViewById(R.id.category_image);
            catName = itemView.findViewById(R.id.category_name);
            ring = itemView.findViewById(R.id.category_ring);
        }
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(int position);
    }
}

