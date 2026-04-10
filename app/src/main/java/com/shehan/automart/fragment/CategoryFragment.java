package com.shehan.automart.fragment;


import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.shehan.automart.R;
import com.shehan.automart.adapter.CategoryFragmentAdapter;
import com.shehan.automart.adapter.ProductAdapter;
import com.shehan.automart.databinding.FragmentCategoryBinding;
import com.shehan.automart.model.Category;
import com.shehan.automart.model.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;



public class CategoryFragment extends Fragment {
    private FragmentCategoryBinding binding;
    private FirebaseFirestore db;
    private List<Category> categoryList = new ArrayList<>();
    private List<Product> productList = new ArrayList<>();
    private List<Product> searchResultList = new ArrayList<>();

    private Map<String, View> sectionViewMap = new HashMap<>();
    private CategoryFragmentAdapter categoryFragmentAdapter;
    private ProductAdapter productAdapter;
    private ProductAdapter searchAdapter;

    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);

        if (isAdded() && getActivity() !=null){
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
        }

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();


        binding.categoryCategoryListRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        db.collection("categories").orderBy("id").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (!isAdded() || binding == null) {
                        return;
                    }

                    if (task.getResult() != null) {

                        categoryList = task.getResult().toObjects(Category.class);

                        categoryFragmentAdapter = new CategoryFragmentAdapter(categoryList, new CategoryFragmentAdapter.OnCatClickListener() {
                            @Override
                            public void onCatClick(Category category, int position) {
                                scrollToSection(category);
                            }
                        });
                        loadProducts();
                        binding.categoryCategoryListRecycler.setAdapter(categoryFragmentAdapter);
                    }
                }
            }
        });


        cartBtnVisile();

        binding.categoryBtnCart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new CartFragment()).addToBackStack(null).commit();
            }
        });


        binding.categoryBackBtn.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .addToBackStack(null)
                    .commit();
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


        ///  search
        binding.categorySearchView.setVisibility(View.GONE);

        binding.searchResultsRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        searchAdapter = new ProductAdapter(searchResultList, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {

                Bundle bundle = new Bundle();
                bundle.putString("productId", product.getId());

                SingleProductViewFragment singleProductViewFragment = new SingleProductViewFragment();
                singleProductViewFragment.setArguments(bundle);

                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, singleProductViewFragment)
                        .addToBackStack(null)
                        .commit();
            }
        }, new ProductAdapter.OnFavClickListener() {
            @Override
            public void onFavClick(Product product) {
                Toast.makeText(getContext(), product.getName() + " saved as a favourite", Toast.LENGTH_SHORT).show();
            }
        });
        binding.searchResultsRecycler.setAdapter(searchAdapter);


        binding.categorySearchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchProducts(s.toString());

            }
        });
    }

    private void searchProducts(String query) {
        if (!isAdded() || binding == null) {
            return;
        }

        searchResultList.clear();

        if (query == null || query.trim().isEmpty()) {
            binding.categorySearchView.setVisibility(View.GONE);
            searchAdapter.notifyDataSetChanged();
            return;
        }

        String searchText = query.toLowerCase().trim();

        for (Product product : productList) {
            String productName = product.getName() != null ? product.getName().toLowerCase() : "";
            String productDescription = product.getDescription() != null ? product.getDescription().toLowerCase() : "";

            if (productName.contains(searchText) || productDescription.contains(searchText)) {
                searchResultList.add(product);
            }
        }

        if (searchResultList.isEmpty()) {
            binding.categorySearchView.setVisibility(View.GONE);
            binding.categorySearchEmptyContainer.setVisibility(View.VISIBLE);
        } else {
            binding.categorySearchView.setVisibility(View.VISIBLE);
            binding.categorySearchEmptyContainer.setVisibility(View.GONE);

        }

        searchAdapter.notifyDataSetChanged();
    }

    private void scrollToSection(Category category) {
        View targetView = sectionViewMap.get(category.getId());

        if (targetView != null) {
            binding.categoryNestedScroll.post(() ->
                    binding.categoryNestedScroll.smoothScrollTo(0, targetView.getTop()));
        }
    }

    private void loadProducts() {
        if (!isAdded() || binding == null){
            binding.categoryLoadingContainer.setVisibility(View.VISIBLE);
        }
        db.collection("product").get().addOnCompleteListener(task -> {
            if (!isAdded() || binding == null) {
                return;
            }

            binding.categoryLoadingContainer.setVisibility(View.GONE);

            if (task.isSuccessful() && task.getResult() != null) {
                productList.clear();
                productList.addAll(task.getResult().toObjects(Product.class));
                buildSections();
            }
        });
    }

    private void buildSections() {
        if (!isAdded() || binding == null) {
            return;
        }

        binding.categorySectionContainer.removeAllViews();
        sectionViewMap.clear();

        for (Category category : categoryList) {
            ArrayList<Product> filterProducts = new ArrayList<>();

            for (Product product : productList) {
                if (product.getCategory_id() != null &&
                        product.getCategory_id().equals(category.getId())) {
                    filterProducts.add(product);
                }
            }

            if (filterProducts.isEmpty()) {
                continue;
            }

            TextView titleView = new TextView(requireContext());
            titleView.setText(category.getName());
            titleView.setTextSize(20);
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setPadding(24, 24, 24, 12);

            binding.categorySectionContainer.addView(titleView);
            sectionViewMap.put(category.getId(), titleView);

            RecyclerView productRecyclerView = getRecyclerView(filterProducts);
            binding.categorySectionContainer.addView(productRecyclerView);
        }

        setUpScrollListener();
    }

    @NonNull
    private RecyclerView getRecyclerView(ArrayList<Product> filterProducts) {
        RecyclerView productRecyclerView = new RecyclerView(requireContext());

        productRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        productRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        productRecyclerView.setNestedScrollingEnabled(false);
        productRecyclerView.setAdapter(new ProductAdapter(filterProducts, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
//                Toast.makeText(getContext(), "product "+product.getName(), Toast.LENGTH_SHORT).show();
                Bundle bundle = new Bundle();
                bundle.putString("productId", product.getId());

                SingleProductViewFragment singleProductViewFragment = new SingleProductViewFragment();
                singleProductViewFragment.setArguments(bundle);

                getParentFragmentManager().
                        beginTransaction().
                        replace(R.id.fragment_container, singleProductViewFragment).
                        addToBackStack(null).
                        commit();
            }
        }, new ProductAdapter.OnFavClickListener() {
            @Override
            public void onFavClick(Product product) {
                Toast.makeText(getContext(), product.getName() + "saved as a favourite", Toast.LENGTH_SHORT).show();

            }
        }));

        return productRecyclerView;
    }


    private void setUpScrollListener() {
        binding.categoryNestedScroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            updateHighlightedCategory(scrollY);
        });
    }

    private void updateHighlightedCategory(int scrollY) {
        if (categoryList == null || categoryList.isEmpty() || categoryFragmentAdapter == null) {
            return;
        }

        int selectedIndex = 0;

        for (int i = 0; i < categoryList.size(); i++) {
            Category category = categoryList.get(i);
            View sectionView = sectionViewMap.get(category.getId());

            if (sectionView != null) {
                int sectionTop = sectionView.getTop();

                if (scrollY >= sectionTop - 20) {
                    selectedIndex = i;
                } else {
                    break;
                }
            }
        }

        categoryFragmentAdapter.setSelectedPosition(selectedIndex);
        binding.categoryCategoryListRecycler.smoothScrollToPosition(selectedIndex);
    }

    int cartItemCount = 0;

    public void cartBtnVisile() {
        cartItemCount = 0;
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            db.collection("cart").whereEqualTo("user_id", mAuth.getCurrentUser().getUid()).get().addOnSuccessListener(qs -> {
                if (qs == null || qs.isEmpty()) return;

                if (!isAdded() || binding == null) return;
                if (qs == null || qs.isEmpty()) return;


                String cartDocId = qs.getDocuments().get(0).getId();
                if (cartDocId == null || cartDocId.isEmpty()) return;

                db.collection("cart_items").whereEqualTo("cart_id", cartDocId).get().addOnSuccessListener(qs2 -> {

                    if (!isAdded() || binding == null) return;
                    if (qs2 == null || qs2.isEmpty()) return;


                    qs2.getDocuments().forEach(ds -> {
                        cartItemCount += ds.getDouble("qty").intValue();
                    });

                    if (cartItemCount > 0) {
                        binding.categoryAddToCartBtnContainer.setVisibility(View.VISIBLE);
                        if (cartItemCount < 10) {
                            binding.categoryCartItemCount.setText("0" + String.valueOf(cartItemCount));
                        } else {
                            binding.categoryCartItemCount.setText(String.valueOf(cartItemCount));
                        }
                    } else {
                        binding.categoryAddToCartBtnContainer.setVisibility(View.GONE);
                    }
                });
            });
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.categoryLoadingContainer.setVisibility(View.GONE);
        }
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null && getActivity().findViewById(R.id.home_toolbar) != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
        }
    }
}