package com.shehan.automart.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;
import com.shehan.automart.adapter.OrderAdapter;
import com.shehan.automart.databinding.FragmentOrderBinding;
import com.shehan.automart.model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



public class OrderFragment extends Fragment {

    private FragmentOrderBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean emptyOrders;
    private List<Order> orderList = new ArrayList<>();

    public OrderFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        if (mAuth == null || mAuth.getCurrentUser() == null) {
            navigateLogin();
        }
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrderBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (isAdded() && getActivity() != null){
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
        }

        binding.orderItemRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        if (currentUser == null || currentUser.getUid() == null) {
            navigateLogin();
            return;
        }

        db.collection("order")
                .whereEqualTo("user_doc_id", currentUser.getUid())
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds == null || qds.isEmpty()) {
                        // empty orders
                        emptyOrders = true;
                        setEmptyOrdersContainerVisible(emptyOrders);
                    } else {
                        emptyOrders = false;
                        setEmptyOrdersContainerVisible(emptyOrders);

                        orderList.clear();
                        orderList.addAll(qds.toObjects(Order.class));
                        OrderAdapter orderAdapter = new OrderAdapter(orderList, new OrderAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(int position) {
                                Bundle bundle = new Bundle();
                                bundle.putString("order_doc_id",orderList.get(position).getOrder_doc_id());

                                OrderDetailsFragment orderDetailsFragment = new OrderDetailsFragment();
                                orderDetailsFragment.setArguments(bundle
                                );
                                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container,orderDetailsFragment).addToBackStack(null).commit();
                            }
                        });
                        binding.orderItemRecycler.setAdapter(orderAdapter);
                    }

                }).addOnFailureListener(e -> {
                    emptyOrders = true;
                    setEmptyOrdersContainerVisible(emptyOrders);
                    Toast.makeText(requireContext(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                });

        binding.orderEmptyOrderStartShoppingBtn.setOnClickListener(v -> {

            if (isAdded() && getActivity() != null){
                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                NavigationView sidenav = getActivity().findViewById(R.id.side_navigation);
                sidenav.setCheckedItem(R.id.side_nav_home);

                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                bottomNav.setSelectedItemId(R.id.bottom_nav_home);


//                getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

            }
        });

        Objects.requireNonNull(getActivity()).getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
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

        binding.orderBackBtn.setOnClickListener(v->{
            if (isAdded() && getActivity() != null) {
                if (isAdded() && getActivity() != null){
                    getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                    NavigationView sidenav = getActivity().findViewById(R.id.side_navigation);
                    sidenav.setCheckedItem(R.id.side_nav_home);

                    BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                    bottomNav.setSelectedItemId(R.id.bottom_nav_home);

                }
            }
        });

    }

    private void navigateLogin() {
        Intent intent = new Intent(this.getContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
        return;
    }


    private void setEmptyOrdersContainerVisible(boolean isEmptyOrders) {

        if (isEmptyOrders) {
            binding.orderEmptyOrderContainer.setVisibility(View.VISIBLE);
            binding.orderItemRecyclerContainer.setVisibility(View.GONE);
        } else {
            binding.orderEmptyOrderContainer.setVisibility(View.GONE);
            binding.orderItemRecyclerContainer.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        if (mAuth == null || currentUser == null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return;
        }
    }

}