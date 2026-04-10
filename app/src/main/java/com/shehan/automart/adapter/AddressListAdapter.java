package com.shehan.automart.adapter;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shehan.automart.R;
import com.shehan.automart.model.Address;

import java.util.List;



public class AddressListAdapter extends RecyclerView.Adapter<AddressListAdapter.ViewHolde> {
    private List<Address> addressList;
    private OnEditListener editListener;
    private OnRemoveListener removeListener;

    private OnCheckListener checkListener;


    public AddressListAdapter(List<Address> addressList, OnEditListener editListener, OnRemoveListener removeListener, OnCheckListener checkListener) {
        this.addressList = addressList;
        this.editListener = editListener;
        this.removeListener = removeListener;
        this.checkListener= checkListener;
    }

    @NonNull
    @Override
    public AddressListAdapter.ViewHolde onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);

        return new ViewHolde(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressListAdapter.ViewHolde holder, int position) {

        Address address = addressList.get(position);

//        holder.tag.setText(address.getHome_name().substring(0, 1));
        holder.home_name.setText(address.getHome_name());
        holder.line_1.setText(address.getAddress_line1()+", "+address.getAddress_line2());
        holder.city.setText(address.getCity()+" - "+address.getPostal_code());
        if (address.isChecked()){
            holder.checkBox.setChecked(true);
        }else {
            holder.checkBox.setChecked(false);
        }

        holder.edit.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onEdit(position);
            }
        });

        holder.remove.setOnClickListener(v -> {
            if (removeListener != null){
                removeListener.onRemove(position);
            }
        });

        holder.checkBox.setClickable(false);
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION && checkListener != null) {
                checkListener.onCheck(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public class ViewHolde extends RecyclerView.ViewHolder {
        TextView tag, home_name, line_1, line_2, city, postal_code;
        ImageView edit, remove;

        CheckBox checkBox;


        public ViewHolde(@NonNull View itemView) {
            super(itemView);

            home_name = itemView.findViewById(R.id.item_address_home_name);
            line_1 = itemView.findViewById(R.id.item_address_line1);
            city = itemView.findViewById(R.id.item_address_city);
            edit = itemView.findViewById(R.id.item_address_edit);
            remove = itemView.findViewById(R.id.item_address_delete);
            checkBox=itemView.findViewById(R.id.item_address_checkbox);
        }
    }

    public interface OnEditListener {
        void onEdit(int position);
    }

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    public interface OnCheckListener{
        void onCheck(int position);
    }

}
