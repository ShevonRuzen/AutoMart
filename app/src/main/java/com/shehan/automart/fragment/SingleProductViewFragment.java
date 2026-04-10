package com.shehan.automart.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;
import com.shehan.automart.databinding.FragmentSingleProductViewBinding;
import com.shehan.automart.model.Availability;
import com.shehan.automart.model.Cart;
import com.shehan.automart.model.CartItems;
import com.shehan.automart.model.Color;
import com.shehan.automart.model.Product;

import java.util.Locale;



public class SingleProductViewFragment extends Fragment {

    private String productId;
    private FragmentSingleProductViewBinding binding;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean isAvailable = false;

    private Button addToCartButton;

    private int qty = 1;
    private double price;

    public SingleProductViewFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSingleProductViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
        }


        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        addToCartButton = binding.singleProductAddToCartBtn;

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();


        if (mAuth.getCurrentUser() != null) {
            currentUser = mAuth.getCurrentUser();
        }

        db.collection("product").whereEqualTo("id", productId).get()
                .addOnSuccessListener(qds -> {
                    if (!isAdded() || binding == null || qds == null || qds.isEmpty()) {
                        return;
                    }

                    Product product = qds.getDocuments().get(0).toObject(Product.class);
                    if (product == null) {
                        return;
                    }

                    price = product.getPrice();

                    binding.singleProductName.setText(product.getName());
                    binding.singleProductDescription.setText(product.getDescription());
                    binding.singleProductPrice.setText(
                            String.format(Locale.US, "LKR. %,.2f", price)
                    );

                    StorageReference folderRef = storage.getReference().child(product.getImage());
                    folderRef.listAll().addOnSuccessListener(listResult -> {
                        if (!isAdded() || binding == null) {
                            return;
                        }

                        if (!listResult.getItems().isEmpty()) {
                            StorageReference firstImage = listResult.getItems().get(0);
                            firstImage.getDownloadUrl().addOnSuccessListener(uri -> {
                                if (!isAdded() || binding == null) {
                                    return;
                                }

                                Glide.with(binding.singleProductImage)
                                        .load(uri)
                                        .centerCrop()
                                        .into(binding.singleProductImage);
                            });
                        }
                    });

                    if (product.getStatus_id() != null && !product.getStatus_id().isEmpty()) {
                        db.collection("status")
                                .whereEqualTo("id", product.getStatus_id())
                                .get()
                                .addOnSuccessListener(qs -> {
                                    if (!isAdded() || binding == null || qs == null || qs.isEmpty()) {
                                        return;
                                    }

                                    Availability availability = qs.getDocuments().get(0).toObject(Availability.class);
                                    if (availability != null && "Available".equals(availability.getType())) {
                                        isAvailable = true;
                                    } else {
                                        isAvailable = false;
                                    }

                                    updateAddToCartBtn();
                                });
                    }

                    if (product.getColor_id() != null && !product.getColor_id().isEmpty()) {
                        db.collection("color").whereEqualTo("id", product.getColor_id()).get().addOnSuccessListener(qs -> {
                            if (!isAdded() || binding == null | qs.isEmpty() || qs == null) {
                                return;
                            }

                            Color color = qs.getDocuments().get(0).toObject(Color.class);
                            if (color != null) {
                                binding.singleProductColor.setText(color.getColor());
                            }
                        });
                    }


                });

        binding.singleProductBackBtn.setOnClickListener(v ->
                getParentFragmentManager().popBackStack()
        );

        binding.singleProductQtyMinusBtn.setOnClickListener(v -> {
            if (qty > 1) {
                qty--;
                binding.singleProductQty.setText(String.valueOf(qty));
            }
        });

        binding.singleProductPlusBtn.setOnClickListener(v -> {
            if (qty < 100) {
                qty++;
                binding.singleProductQty.setText(String.valueOf(qty));
            }
        });

        binding.singleProductQty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();

                if (!text.isEmpty()) {
                    try {
                        qty = Integer.parseInt(text);
                        if (qty < 1) qty = 1;
                        if (qty > 100) qty = 100;
                    } catch (NumberFormatException e) {
                        qty = 1;
                    }
                } else {
                    qty = 1;
                }

                updateAddToCartBtn();
            }
        });


//        add to cart btn function
        addToCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAvailable) {
                    addToCart();
                }
            }
        });
    }



    private void addToCart() {

        if (currentUser == null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            return;

        } else {
            db.collection("cart").whereEqualTo("user_id", currentUser.getUid()).get().addOnSuccessListener(qds -> {

                if (qds == null || qds.isEmpty()) {  // user has no cart

                    // create a new cart
                    Cart newCart = new Cart(currentUser.getUid());
                    db.collection("cart").add(newCart).addOnSuccessListener(dr -> {

                        if (dr.getId() == null || dr.getId().isEmpty()) {
                            Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String cartDocID = dr.getId();
                        CartItems newCartItems = CartItems.builder().cart_id(cartDocID).product_id(productId).qty(qty).build();
                        db.collection("cart_items").add(newCartItems).addOnSuccessListener(documentReference -> {
                            Toast.makeText(getContext(), "Item added to the cart", Toast.LENGTH_SHORT).show();
                            moveToCategoryFragment();

                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to add item to cart", Toast.LENGTH_SHORT).show();
                        });

                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to create cart", Toast.LENGTH_SHORT).show();
                    });

                } else { // user has a cart
                    String cartId = qds.getDocuments().get(0).getId();

                    if (cartId == null || cartId.isEmpty()) {
                        Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                        return;

                    } else {
                        db.collection("cart_items").whereEqualTo("cart_id", cartId).whereEqualTo("product_id",productId).get().addOnSuccessListener(qs -> {

                            if (qs == null || qs.isEmpty()){  ///  product is not in cart

                                // add new cart item
                                CartItems cartItem = CartItems.builder().cart_id(cartId).product_id(productId).qty(qty).build();
                                db.collection("cart_items").add(cartItem).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Toast.makeText(getContext(), "Item added to the cart", Toast.LENGTH_SHORT).show();
                                        moveToCategoryFragment();
                                    }
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to add item to cart", Toast.LENGTH_SHORT).show();
                                });

                            }else { // product is already in cart

                                // update cart item
                                CartItems cartItems = qs.getDocuments().get(0).toObject(CartItems.class);
                                if (cartItems == null){
                                    Toast.makeText(getContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                double newQty = cartItems.getQty() + qty;

                                db.collection("cart_items").document(qs.getDocuments().get(0).getId()).update("qty", newQty).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Toast.makeText(requireContext(), "Product quantity updated", Toast.LENGTH_SHORT).show();
                                        moveToCategoryFragment();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), "Something went wrong. Please try again later!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });
                    }
                }
            });
        }
    }

    // move to category fragment
    public void moveToCategoryFragment() {
        getParentFragmentManager().popBackStack();
    }


    public void updateAddToCartBtn() {
        if (binding == null) return;

        if (isAvailable) {
            addToCartButton.setClickable(true);
            addToCartButton.setEnabled(true);
            addToCartButton.setText(
                    "Add " + qty + " to cart for LKR. " +
                            String.format(Locale.US, "%,.2f", (price * qty))
            );
        } else {
            addToCartButton.setClickable(false);
            addToCartButton.setEnabled(false);
            addToCartButton.setText("Unavailable");

            int grayColor = android.graphics.Color.parseColor("#D1D1D1");
            binding.singleProductAddToCartBtn.setSupportBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(grayColor)
            );
        }
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
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
        }
    }
}