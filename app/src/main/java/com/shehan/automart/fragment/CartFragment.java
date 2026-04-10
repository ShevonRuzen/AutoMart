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
import com.shehan.automart.adapter.CartAdapter;
import com.shehan.automart.databinding.FragmentCartBinding;
import com.shehan.automart.model.CartItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;



public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private List<CartItems> cartItemsList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth == null || mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        currentUser = mAuth.getCurrentUser();

        if (getActivity() != null && getActivity().findViewById(R.id.home_toolbar) != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
        }

        binding.cartItemRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        db.collection("cart").whereEqualTo("user_id", currentUser.getUid()).get().addOnSuccessListener(qds -> {
            if (qds == null || qds.isEmpty()) {
                // cart empty   // TODO ==> view empty cart layout
                if (binding == null) return;
                binding.cartBottomContainer.setVisibility(View.GONE);
                binding.cartItemRecyclerContainer.setVisibility(View.GONE);
                binding.cartEmptyCartContainer.setVisibility(View.VISIBLE);


                return;
            } else {
                db.collection("cart_items").whereEqualTo("cart_id", qds.getDocuments().get(0).getId()).get().addOnSuccessListener(qds2 -> {
                    if (qds2 == null || qds2.isEmpty()) {
                        // cart empty   // TODO ==> view empty cart layout
                        if (binding == null) return;
                        binding.cartBottomContainer.setVisibility(View.GONE);
                        binding.cartItemRecyclerContainer.setVisibility(View.GONE);
                        binding.cartEmptyCartContainer.setVisibility(View.VISIBLE);
                        return;
                    }


                    if (binding == null) return;
                    binding.cartBottomContainer.setVisibility(View.VISIBLE);
                    binding.cartItemRecyclerContainer.setVisibility(View.VISIBLE);
                    binding.cartEmptyCartContainer.setVisibility(View.GONE);


                    cartItemsList.clear();
                    cartItemsList.addAll(qds2.toObjects(CartItems.class));

                    CartAdapter cartAdapter = getCartAdapter();
                    binding.cartItemRecycler.setAdapter(cartAdapter);
                    updateTotal();

                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this.getContext(), "Something went wrong!", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
        });


        binding.cartBackBtn.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new CategoryFragment())
                        .addToBackStack(null)
                        .commit()
        );

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



        binding.cartProceedToCheckoutBtn.setOnClickListener(v -> {
            if (!cartItemsList.isEmpty()) {
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CheckoutFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @NonNull
    private CartAdapter getCartAdapter() {
        CartAdapter cartAdapter = new CartAdapter(cartItemsList);

        cartAdapter.setOnQtyChangeListener(cartItem -> {
            db.collection("cart_items").document(cartItem.getDocId()).update("qty", cartItem.getQty()).addOnSuccessListener(unused -> updateTotal());

        });

        cartAdapter.setRemoveListener(position -> {
            String docId = cartItemsList.get(position).getDocId();

            db.collection("cart_items").document(docId).delete()
                    .addOnSuccessListener(unused -> {
                        cartItemsList.remove(position);
                        cartAdapter.notifyItemRemoved(position);
                        cartAdapter.notifyItemRangeChanged(position, cartItemsList.size());

                        if (cartItemsList.isEmpty() && binding != null) {
                            binding.cartBottomContainer.setVisibility(View.GONE);
                            binding.cartItemRecyclerContainer.setVisibility(View.GONE);
                            binding.cartEmptyCartContainer.setVisibility(View.VISIBLE);
                        }

                        updateTotal();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed to remove item", Toast.LENGTH_SHORT).show()
                    );
        });
        return cartAdapter;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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

    private void updateTotal() {
        List<String> productIds = new ArrayList<>();
        for (CartItems cartItem : cartItemsList) {
            productIds.add(cartItem.getProduct_id());
        }

        if (productIds.isEmpty()) {
            if (binding != null) {
                binding.cartTotalAmount.setText(String.format(Locale.US, "LKR. %,.2f", 0.0));
            }
            return;
        }

        db.collection("product")
                .whereIn("id", productIds)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        if (binding != null) {
                            binding.cartTotalAmount.setText(String.format(Locale.US, "LKR. %,.2f", 0.0));
                        }
                        return;
                    }

                    Map<String, Double> priceMap = new HashMap<>();
                    queryDocumentSnapshots.getDocuments().forEach(ds -> {
                        String id = ds.getString("id");
                        Double price = ds.getDouble("price");
                        if (id != null && price != null) {
                            priceMap.put(id, price);
                        }
                    });

                    double total = 0;
                    for (CartItems cartItem : cartItemsList) {
                        Double price = priceMap.get(cartItem.getProduct_id());
                        if (price != null) {
                            total += price * cartItem.getQty();
                        }
                    }

                    if (binding != null) {
                        binding.cartTotalAmount.setText(
                                String.format(Locale.US, "LKR. %,.2f", total)
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    if (binding != null) {
                        binding.cartTotalAmount.setText(String.format(Locale.US, "LKR. %,.2f", 0.0));
                    }
                });
    }
}