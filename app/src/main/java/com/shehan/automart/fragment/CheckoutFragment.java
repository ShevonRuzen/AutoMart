package com.shehan.automart.fragment;


import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;
import com.shehan.automart.databinding.FragmentCheckoutBinding;
import com.shehan.automart.model.Address;
import com.shehan.automart.model.CartItems;
import com.shehan.automart.model.Order;
import com.shehan.automart.model.Order_item;
import com.shehan.automart.model.Product;
import com.shehan.automart.model.User;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Address currentUserAddress;
    private List<CartItems> cartItemsList = new ArrayList<>();
    private List<Product> productList = new ArrayList<>();

    private double subtotal = 0;
    private double shippingFee = 0;
    private int itemCount = 0;
    private User user;
    private boolean paymentActive = false;
    private String orderId;

    private Map<String, String> productDocList = new HashMap<>();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth == null || mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null && getActivity().findViewById(R.id.home_toolbar) != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
        }


        if (!isAdded() || binding == null) return;

        getCartItems();
        getDeliveryAddress();
        getUserDetails();

        //back btn
        binding.checkoutBackBtn.setOnClickListener(v -> {
            if (!isAdded() || binding == null) return;
            getParentFragmentManager().popBackStack();
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

        // toggle location card
        binding.checkoutLocationCard.setOnClickListener(v -> {
            if (binding.checkoutLocationCardBody.getVisibility() == View.GONE) {
                binding.checkoutLocationCardBody.setVisibility(View.VISIBLE);
                binding.checkoutShippingArrowBtn.setRotation(180f);
            } else {
                binding.checkoutLocationCardBody.setVisibility(View.GONE);
                binding.checkoutShippingArrowBtn.setRotation(0);

            }
        });

        //toggle summary card
        binding.checkoutSummaryCard.setOnClickListener(v -> {
            if (binding.checkoutSummaryCardBody.getVisibility() == View.GONE) {
                binding.checkoutSummaryCardBody.setVisibility(View.VISIBLE);
                binding.checkoutSummaryArrowBtn.setRotation(180f);
            } else {
                binding.checkoutSummaryCardBody.setVisibility(View.GONE);
                binding.checkoutSummaryArrowBtn.setRotation(0);

            }
        });

        // change address btn
        binding.checkoutLocationChangeBtn.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new AddressListFragment()).addToBackStack(null).commit();
        });


        // checkout btn
        binding.checkoutPlaceOrderBtn.setOnClickListener(v -> {
            checkoutProcess();
        });
    }

    private void getDeliveryAddress() {
        try {
            if (currentUser == null) return;

            db.collection("address").whereEqualTo("user_id", currentUser.getUid()).whereEqualTo("checked", true).get().addOnSuccessListener(qds -> {
                if (qds != null && !qds.isEmpty()) {
                    currentUserAddress = qds.getDocuments().get(0).toObject(Address.class);
                }else {
                    currentUserAddress =null;
                }
                // set picked_up address
                setAddressDetails();
                if (currentUserAddress != null && currentUserAddress.getShippingFee()  != 0){
                    shippingFee = currentUserAddress.getShippingFee();
                }else {
                    shippingFee = 150;
                }
            }).addOnFailureListener(e -> {
                currentUserAddress = null;
            });
        } catch (Exception e) {
            Log.e("Checkout", "Error loading address", e);
        }
    }

    private void setAddressDetails() {
        if (!isAdded() || binding == null) return;
        if (currentUser == null) {
            return;
        }

        if (currentUserAddress == null) {
            binding.checkoutLocationHomeName.setText("No address found");
            binding.checkoutLocationAddress.setText("Please add a picked_up address");
            binding.checkoutLocationLetter.setText("!");
            return;
        };
        Log.i("address", currentUserAddress.getHome_name());
        try {
            binding.checkoutLocationHomeName.setText(currentUserAddress.getHome_name() != null ? currentUserAddress.getHome_name() : "");
            if (currentUserAddress.getAddress_line1() != null && currentUserAddress.getAddress_line2() != null
                    && currentUserAddress.getCity() != null && currentUserAddress.getPostal_code() != null) {

                binding.checkoutLocationAddress.setText(currentUserAddress.getAddress_line1() + ", "
                        + currentUserAddress.getAddress_line2() + ", "
                        + currentUserAddress.getCity() + ", " + currentUserAddress.getPostal_code());
            }
            binding.checkoutLocationLetter.setText(currentUserAddress.getHome_name() != null ? currentUserAddress.getHome_name().substring(0, 1).toUpperCase() : "");
            binding.checkoutLocationHomeName.setText(currentUserAddress.getHome_name() != null ? currentUserAddress.getHome_name() : "");
        } catch (Exception e) {
            Log.e("Checkout", "Error loading address", e);
        }

    }

    private void getUserDetails() {
        if (currentUser == null) return;
        db.collection("user").
                document(currentUser.getUid())
                .get()
                .addOnSuccessListener(ds -> {
                    if (ds == null) {
                        Toast.makeText(this.getContext(), "Something Went wrong! Please try again later", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    user = ds.toObject(User.class);
                    if (user == null) {
                        Toast.makeText(this.getContext(), "Something Went wrong! Please try again later", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this.getContext(), "Something Went wrong! Please try again later", Toast.LENGTH_SHORT).show();
                    return;
                });
    }

    private void getCartItems() {
        if (currentUser == null) return;
        try {
            cartItemsList.clear();
            db.collection("cart").whereEqualTo("user_id", currentUser.getUid()).get().addOnSuccessListener(qds -> {
                if (qds == null || qds.isEmpty()) return;

                qds.forEach(cart -> {
                    if (cart.getId() != null) {
                        db.collection("cart_items").whereEqualTo("cart_id", cart.getId()).get().addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty())
                                return;

                            queryDocumentSnapshots.forEach(ds -> {
                                CartItems cartItem = ds.toObject(CartItems.class);
                                if (cartItem != null) {
                                    cartItemsList.add(cartItem);

                                }
                            });
                            getCartItemProducts();
                        });
                    }
                });
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    int cartItemCount = 0;
    private void getCartItemProducts() {
        Log.i("product", "start getCartItemProduct");
        if (currentUser == null) return;
        if (cartItemsList.isEmpty()) return;

        subtotal = 0;
        itemCount = 0;
        cartItemCount = 0;

        int totalItems = cartItemsList.size();
        Log.i("product", "cartItemList.size() =" + cartItemsList.size());


        productList.clear();

        try {
            for (CartItems cartItem : cartItemsList) {
                int qty = (int) cartItem.getQty();
                itemCount += qty;
                db.collection("product").whereEqualTo("id", cartItem.getProduct_id()).get().addOnSuccessListener(qds -> {
                    if (qds == null || qds.isEmpty()) {
                        return;
                    }

                    cartItemCount++;

                    Product product = qds.getDocuments().get(0).toObject(Product.class);
                    if (product != null) {
                        productList.add(product);
                        subtotal += product.getPrice() * qty;
                        productDocList.put(product.getId(), qds.getDocuments().get(0).getId());
                    }

                    if (cartItemsList.size() == cartItemCount) {
                        setTotal();
                    }


                }).addOnFailureListener(e -> {
                    cartItemCount++;

                    if (cartItemsList.size() == cartItemCount) {
                        setTotal();
                    }
                });
            }
        } catch (Exception e) {
            Log.e("Checkout", "Error loading product", e);
        }
    }



    private void setTotal() {
        try {
            binding.checkoutSubtotal.setText(String.format(Locale.US, "LKR. %,.2f", subtotal));
            binding.checkoutShippingFee.setText(String.format(Locale.US, "LKR. %,.2f", shippingFee));
            binding.checkoutTotal.setText(String.format(Locale.US, "LKR. %,.2f", shippingFee + subtotal));
            binding.checkoutItemCount.setText(String.valueOf(itemCount) + " Items");

            binding.checkoutSummaryCardBody.removeAllViews();
            for (int i = 0; i < cartItemsList.size(); i++) {
                for (int j = 0; j < productList.size(); j++) {

                    if (cartItemsList.get(i) == null || productList.get(j) == null) continue;

                    if (cartItemsList.get(i).getProduct_id().equals(productList.get(j).getId())) {

                        Product product = productList.get(j);
                        double subTotal = cartItemsList.get(i).getQty() * product.getPrice();

                        // divider
                        View divider = new View(requireContext());
                        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                1
                        );
                        dividerParams.setMargins(0, 12, 0, 12);
                        divider.setLayoutParams(dividerParams);
                        divider.setBackgroundColor(android.graphics.Color.parseColor("#E8D7D2"));

                        // row container
                        LinearLayout row = new LinearLayout(requireContext());
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(Gravity.CENTER_VERTICAL);

                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        row.setLayoutParams(rowParams);

                        // qty box
                        TextView qtyView = new TextView(requireContext());
                        qtyView.setText(String.valueOf((int) cartItemsList.get(i).getQty()));
                        qtyView.setGravity(Gravity.CENTER);
                        qtyView.setTextSize(16);
                        qtyView.setTextColor(android.graphics.Color.parseColor("#4A3F45"));
                        qtyView.setBackgroundResource(R.drawable.checkout_qty_box_bg);

                        LinearLayout.LayoutParams qtyParams = new LinearLayout.LayoutParams(80, 80);
                        qtyParams.setMargins(0, 0, 24, 0);
                        qtyView.setLayoutParams(qtyParams);

                        // product name
                        TextView nameView = new TextView(requireContext());
                        nameView.setText(product.getName());
                        nameView.setTextSize(16);
                        nameView.setTextColor(android.graphics.Color.parseColor("#4A3F45"));

                        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                1
                        );
                        nameView.setLayoutParams(nameParams);

                        // price
                        TextView priceView = new TextView(requireContext());
                        priceView.setText(String.format(Locale.US, "LKR %,.2f", subTotal));
                        priceView.setTextSize(16);
                        priceView.setTextColor(android.graphics.Color.parseColor("#4A3F45"));
                        priceView.setGravity(Gravity.END);

                        LinearLayout.LayoutParams priceParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        );
                        priceView.setLayoutParams(priceParams);

                        row.addView(qtyView);
                        row.addView(nameView);
                        row.addView(priceView);

                        binding.checkoutSummaryCardBody.addView(divider);
                        binding.checkoutSummaryCardBody.addView(row);

                    }

                }
            }

            paymentActive = true;

//            Log.i("product", "product list size =" + productList.size());
//            Log.i("product", "cart item list size =" + cartItemsList.size());
        } catch (Exception e) {
            Log.i("error", e.getMessage());

        }

    }

    private boolean validatePayment() {
        if (cartItemsList == null || cartItemsList.isEmpty() || cartItemsList.size() == 0 || productList == null || productList.isEmpty() || productList.size() == 0) {
            getParentFragmentManager().popBackStack();
            Toast.makeText(getContext(), "Cart is empty", Toast.LENGTH_SHORT).show();

            return false;
        }
        if (currentUser == null || user == null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return false;
        }

        if (!paymentActive) {
            Toast.makeText(getContext(), "Please wait, checkout is still loading", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentUserAddress == null) {
            Toast.makeText(getContext(), "Please select a picked_up address", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void checkoutProcess() {
        try {
            double total = subtotal + shippingFee;

            if (!validatePayment()) return;

            orderId = "FF-" + System.currentTimeMillis();

            InitRequest req = new InitRequest();
            req.setSandBox(true);
            req.setMerchantId("1227046");
            req.setMerchantSecret("MjAxNzg1MjY5MjMwOTM4MjYyMjAxNTUxNDc1NzMxOTkxMDk4ODMx");
            req.setCurrency("LKR");
            req.setAmount(total);
            req.setOrderId(orderId);
            req.setItemsDescription("AutoMart order");

            String name = user.getName() != null ? user.getName().trim() : "";
            String[] nameParts = name.split(" ", 2);

            req.getCustomer().setFirstName(nameParts.length > 0 ? nameParts[0] : "");
            req.getCustomer().setLastName(nameParts.length > 1 ? nameParts[1] : "");
            req.getCustomer().setEmail(user.getEmail() != null ? user.getEmail() : "");
            req.getCustomer().setPhone(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");

            String fullAddress = (currentUserAddress.getHome_name() != null ? currentUserAddress.getHome_name() : "") + ", " +
                    (currentUserAddress.getAddress_line1() != null ? currentUserAddress.getAddress_line1() : "") + ", " +
                    (currentUserAddress.getCity() != null ? currentUserAddress.getCity() : "") + ", " +
                    (currentUserAddress.getPostal_code() != null ? currentUserAddress.getPostal_code() : "");

            req.getCustomer().getAddress().setAddress(fullAddress);
            req.getCustomer().getAddress().setCity(currentUserAddress.getCity() != null ? currentUserAddress.getCity() : "");
            req.getCustomer().getAddress().setCountry("Sri Lanka");

            req.getCustomer().getDeliveryAddress().setAddress(fullAddress);
            req.getCustomer().getDeliveryAddress().setCity(currentUserAddress.getCity() != null ? currentUserAddress.getCity() : "");
            req.getCustomer().getDeliveryAddress().setCountry("Sri Lanka");

            req.setNotifyUrl("https://my-eshop.requestcatcher.com/test");

            Intent intent = new Intent(requireContext(), PHMainActivity.class);
            intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

            payhereLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(this.getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("Checkout", "Error");
        }
    }

    private final ActivityResultLauncher<Intent> payhereLauncher
            = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Intent data = result.getData();
            if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                if (response != null && response.isSuccess()) {
                    StatusResponse statusResponse = response.getData();

                    // save order to firestore
                    saveOrder(statusResponse);   /// we can get payment time , status, currency and more from status response


                    Log.i("PAYHERE", "Payment success!");
                } else {
                    Log.e("PAYHERE", "Payment failed!" + response.getData().getMessage());
                }
            }
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Log.e("PAYHERE", "Payment canceled!");
        }
    });

    private void saveOrder(StatusResponse statusResponse) {
        // add order
        try {
            Order order = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                order = Order.builder().user_doc_id(currentUser.getUid())
                        .created_date(Timestamp.now())
                        .notes(!binding.checkoutSpecialInstructions.getText().isEmpty() ?
                                binding.checkoutSpecialInstructions.getText().toString() : "")
                        .order_id(orderId)
                        .payment_status("PAID")
                        .order_status("PENDING")
                        .address_home(currentUserAddress.getHome_name())
                        .address_city(currentUserAddress.getCity())
                        .address_line_1(currentUserAddress.getAddress_line1())
                        .address_line_2(currentUserAddress.getAddress_line2())
                        .address_postal_code(currentUserAddress.getPostal_code())
                        .total_amount(subtotal + shippingFee)
                        .shipping_fee(currentUserAddress.getShippingFee() != 0 ? currentUserAddress.getShippingFee():100)
                        .build();
            }

            db.collection("order").add(order).addOnSuccessListener(dr -> {
                if (dr == null) return;

                cartItemsList.forEach(cartItem -> {
                    if (cartItem != null) {
                        productList.forEach(product -> {
                            if (product != null && cartItem.getProduct_id().equals(product.getId())) {
                                Order_item order_item = Order_item.builder()
                                        .order_doc_id(dr.getId())
                                        .product_doc_id(productDocList.get(product.getId()))
                                        .product_price(product.getPrice())
                                        .qty(cartItem.getQty())
                                        .subtotal(product.getPrice() * cartItem.getQty())
                                        .build();

                                db.collection("order_item").add(order_item).addOnSuccessListener(dr2 -> {
                                    if (dr2 != null) {

                                        // clear cart items
                                        db.collection("cart_items").document(cartItem.getDocId()).delete().addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                cartItemsList.remove(cartItem);
                                            }
                                        });
                                    }
                                }).addOnFailureListener(e -> {
                                    Log.e("Checkout", "Error saving order item", e);
                                });
                            }
                        });
                    }


                });

                // clear cart
                db.collection("cart")
                        .whereEqualTo("user_id", currentUser.getUid())
                        .get().addOnSuccessListener(qds -> {
                            if (qds == null || qds.isEmpty()) {
                                return;
                            }

                            if (!cartItemsList.isEmpty()) cartItemsList.clear();
                            if (!productList.isEmpty()) productList.clear();

                            qds.forEach(ds -> {
                                db.collection("cart").document(ds.getId()).delete();
                                // go to Order Details
                                Bundle bundle = new Bundle();
                                bundle.putString("order_doc_id", dr.getId());

                                OrderDetailsFragment orderDetailsFragment = new OrderDetailsFragment();
                                orderDetailsFragment.setArguments(bundle);

                                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container,orderDetailsFragment).addToBackStack(null).commit();
                                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                                bottomNav.setSelectedItemId(R.id.bottom_nav_orders);

                                NavigationView  sideNav = getActivity().findViewById(R.id.side_navigation);
                                sideNav.setCheckedItem(R.id.side_nav_order);
                                Toast.makeText(getContext(), "Your order placed successfully. ", Toast.LENGTH_SHORT).show();

                            });
                        }).addOnFailureListener(e -> {
                            Log.e("Checkout", "Error clearing cart", e);
                        });
            }).addOnFailureListener(e -> {
                Log.e("Checkout", "Error saving order", e);
            });
        } catch (Exception e) {
            Toast.makeText(this.getContext(), "Error: " + e, Toast.LENGTH_SHORT).show();
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (getActivity() != null) {
            View toolbar = getActivity().findViewById(R.id.home_toolbar);
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation);

            if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }

        binding = null;
    }
}