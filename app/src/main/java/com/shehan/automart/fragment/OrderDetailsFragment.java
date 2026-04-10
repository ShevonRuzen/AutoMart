package com.shehan.automart.fragment;


import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;
import com.shehan.automart.adapter.OrderDetailsItemAdapter;
import com.shehan.automart.databinding.FragmentOrderDetailsBinding;
import com.shehan.automart.model.Order;
import com.shehan.automart.model.Order_item;
import com.shehan.automart.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class OrderDetailsFragment extends Fragment {

    private String order_doc_id;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FragmentOrderDetailsBinding binding;
    private List<Order_item> orderItems = new ArrayList<>();
    private User dbUser;
    private Order order;

    private static final String CHANNEL_ID = "Auto Mart";

    private String lastKnownStatus = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            order_doc_id = getArguments().getString("order_doc_id");
        }

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            user = mAuth.getCurrentUser();
        }
        db = FirebaseFirestore.getInstance();


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentOrderDetailsBinding.inflate(inflater, container, false);

        if (isAdded() && getActivity() != null){
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
        }

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // back btn
        binding.orderDetailsBackBtn.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isAdded() && getActivity() != null){
                    getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).addToBackStack(null).commit();


                    NavigationView sidenav = getActivity().findViewById(R.id.side_navigation);
                    sidenav.setCheckedItem(R.id.side_nav_home);

                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    bottomNav.setSelectedItemId(R.id.bottom_nav_home);
                }
            }
        });


//        load order details
        getOrderDetails();

//        toggle btn
        binding.orderDetailsOrderItemDropdown.setOnClickListener(v -> {
            if (isAdded() && binding != null) {
                if (binding.orderDetailsOrderItemLayoutContainer.getVisibility() == View.GONE) {
                    binding.orderDetailsOrderItemLayoutContainer.setVisibility(View.VISIBLE);
                    binding.orderDetailsOrderItemDropdown.setRotation(0f);
                } else {
                    binding.orderDetailsOrderItemLayoutContainer.setVisibility(View.GONE);
                    binding.orderDetailsOrderItemDropdown.setRotation(180f);
                }
            }
        });

    }

    private void getOrderDetails() {
        if (user == null) {
            Toast.makeText(this.getContext(), "User not found!", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        } else {
            user.getUid();
        }

        if (order_doc_id.isEmpty()) {
            return;
        }
        db.collection("order")
                .document(order_doc_id).get()
                .addOnSuccessListener(ds -> {

                    if (!ds.exists()) {
                        Toast.makeText(this.getContext(), "Order not found!", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                        return;
                    }

                    order = ds.toObject(Order.class);
                    if (order == null) {
                        Toast.makeText(this.getContext(), "Order not found!", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                        return;
                    }

                    statusListener();
                    getOrderItems(order);

                }).addOnFailureListener(e -> {
                    Toast.makeText(this.getContext(), "Order not found!", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                    return;
                });

    }

    private void getOrderItems(Order order) {
        db.collection("order_item")
                .whereEqualTo("order_doc_id", order_doc_id)
                .get()
                .addOnSuccessListener(qs -> {

                    if (qs == null || qs.isEmpty()) {
                        Toast.makeText(this.getContext(), "Order items not found!", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                        return;
                    }

                    orderItems.clear();
                    qs.forEach(qds -> {
                        Order_item item = qds.toObject(Order_item.class);
                        orderItems.add(item);
                    });

                    getUserDetails();

                }).addOnFailureListener(e -> {

                    Toast.makeText(this.getContext(), "Order not found!", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                    return;
                });

    }

    private void getUserDetails() {
        if (user == null) {
            Toast.makeText(this.getContext(), "User not found!", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        } else {
            user.getUid();
        }

        db.collection("user")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(ds -> {

                    if (!ds.exists() || ds.toObject(User.class) == null) {
                        Toast.makeText(this.getContext(), "User not found!", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                        return;
                    }

                    dbUser = ds.toObject(User.class);
                    loadDetails();

                    binding.orderDetailsOrderItemLayout.setLayoutManager(new LinearLayoutManager(this.getContext(), RecyclerView.VERTICAL, false));
                    binding.orderDetailsOrderItemLayout.setAdapter(new OrderDetailsItemAdapter(orderItems));

                }).addOnFailureListener(e -> {
                    Toast.makeText(this.getContext(), "User not found!", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                    return;
                });
    }


    private void loadDetails() {

        binding.orderDetailsOrderId.setText(order.getOrder_id() != null ? "Order ID: #" + order.getOrder_id() : "Order ID: #");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String date = "";
        if (order.getCreated_date() != null) {
            date = dateFormat.format(order.getCreated_date().toDate());
        } else {
            date = "";
        }

        binding.orderDetailsOrderDate.setText(date);

        // payments details
        binding.orderDetailsOrderSubtotal
                .setText(String.format(Locale.US, "LKR. %,.2f", (order.getTotal_amount()) - order.getShipping_fee()));
        binding.orderDetailsOrderShipping
                .setText(String.format(Locale.US, "LKR. %,.2f", order.getShipping_fee()));
        binding.orderDetailsOrderTotal
                .setText(String.format(Locale.US, "LKR. %,.2f", order.getTotal_amount()));


        // delivery details
        binding.orderDetailsOrderUserName.setText(dbUser.getName() != null ? dbUser.getName() : "");
        String address = (order.getAddress_home() != null ? order.getAddress_home() : "") + ", "
                + (order.getAddress_line_1() != null ? order.getAddress_line_1() : "") + ", "
                + (order.getAddress_line_2() != null ? order.getAddress_line_2() : "") + ", "
                + (order.getAddress_city() != null ? order.getAddress_city() : "");

        binding.orderDetailsOrderUserMobile.setText(dbUser.getPhoneNumber() == null ? "" : dbUser.getPhoneNumber());

        binding.orderDetailsOrderUserAddress.setText(address);
    }

    private void statusListener() {
        if (order.getOrder_doc_id() == null) return;

        db.collection("order").document(order.getOrder_doc_id()).addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value == null || !value.exists()) return;
            if (binding == null) return;

            String status = value.getString("order_status");
            if (status == null) return;

            binding.orderDetailsOrderStatus.setText(status);
            updateProgress(status);

            if (lastKnownStatus == null) {
                lastKnownStatus = status;
                return;
            }

            if (!status.equals(lastKnownStatus)) {
                lastKnownStatus = status;
            }
        });
    }


    private String getNotificationMessage(String status) {
        switch (status) {
            case "PENDING":
                return "Your order has been placed successfully.";
            case "PROCESSING":
                return "Your order is now being prepared.";
            case "PICKED_UP":
                return "Your order has been picked up and is on the way.";
            case "DELIVERED":
                return "Your order has been delivered.";
            default:
                return "Your order status changed to " + status;
        }
    }

    private void updateProgress(String status) {

        binding.orderDetailsOrderPendingBar.setProgress(0);
        binding.orderDetailsOrderPrepBar.setProgress(0);
        binding.orderDetailsOrderPickBar.setProgress(0);

        if (status == null) return;

        switch (status) {
            case "PENDING":
                setActive(binding.orderDetailsOrderPendingIcon);
                break;
            case "PROCESSING":
                setActive(binding.orderDetailsOrderPendingIcon);
                binding.orderDetailsOrderPendingBar.setProgressCompat(100, true);
                setActive(binding.orderDetailsOrderPrepIcon);
                break;
            case "PICKED_UP":
                setActive(binding.orderDetailsOrderPendingIcon);
                setActive(binding.orderDetailsOrderPrepIcon);
                binding.orderDetailsOrderPendingBar.setProgress(100);
                binding.orderDetailsOrderPrepBar.setProgressCompat(100, true);
                setActive(binding.orderDetailsOrderPickIcon);
                break;
            case "DELIVERED":
                setActive(binding.orderDetailsOrderPendingIcon);
                setActive(binding.orderDetailsOrderPrepIcon);
                setActive(binding.orderDetailsOrderPickIcon);
                binding.orderDetailsOrderPendingBar.setProgress(100);
                binding.orderDetailsOrderPrepBar.setProgress(100);
                binding.orderDetailsOrderPickBar.setProgressCompat(100, true);
                setActive(binding.orderDetailsOrderCompleteIcon);
                break;
        }


    }


    private void setActive(ImageView icon) {

        if (!isAdded() || getContext() == null) return;
        int primary = getContext().getColor(R.color.md_theme_primary);
        int white = getContext().getColor(R.color.md_theme_surfaceContainer);

        // background = progress color
        icon.setBackgroundTintList(ColorStateList.valueOf(primary));

        // icon = white (inverse)
        icon.setImageTintList(ColorStateList.valueOf(white));
    }

    private void navigateToLogin() {
        if (isAdded() && getActivity() != null) {
            Intent intent = new Intent(this.getContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }


}
