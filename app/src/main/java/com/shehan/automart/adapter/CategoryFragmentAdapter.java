package com.shehan.automart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shehan.automart.R;
import com.shehan.automart.model.Category;

import java.util.List;

public class CategoryFragmentAdapter extends RecyclerView.Adapter<CategoryFragmentAdapter.ViewHolder> {

    private OnCatClickListener categoryClickListener;
    private List<Category> categoryList;
    private int selectedPosition = 0;

    public CategoryFragmentAdapter(List<Category> categoryList, OnCatClickListener onCategoryClickListener) {
        this.categoryClickListener = onCategoryClickListener;
        this.categoryList = categoryList;
    }

    @NonNull
    @Override
    public CategoryFragmentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_name, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryFragmentAdapter.ViewHolder holder, int position) {
        Category cat = categoryList.get(position);
        holder.catName.setText(cat.getName());

        if (selectedPosition == position) {
            holder.catName.setBackgroundResource(R.drawable.item_category_name_selected_background);
        } else {
            holder.catName.setBackgroundResource(R.drawable.item_category_name_background);
        }


        holder.itemView.setOnClickListener(v -> {
            int clickedPosition = holder.getBindingAdapterPosition();
            if (clickedPosition == RecyclerView.NO_POSITION){
                return;
            }

            setSelectedPosition(clickedPosition);

            if (categoryClickListener != null) {
                categoryClickListener.onCatClick(cat, clickedPosition);
            }
        });
    }

    public void setSelectedPosition(int position){
        if (position == RecyclerView.NO_POSITION || position == selectedPosition){
            return;
        }

        int oldPosition = selectedPosition;
        selectedPosition = position;

        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView catName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            catName = itemView.findViewById(R.id.category_category_name);
        }
    }


    public interface OnCatClickListener {
        void onCatClick(Category category, int position);
    }
}
