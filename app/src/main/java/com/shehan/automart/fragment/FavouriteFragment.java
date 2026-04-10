package com.shehan.automart.fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.R;
import com.shehan.automart.adapter.FavouriteProductAdapter;
import com.shehan.automart.databinding.FragmentFavouriteBinding;
import com.shehan.automart.helper.FavouriteManager;
import com.shehan.automart.model.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;



public class FavouriteFragment extends Fragment {


    private FragmentFavouriteBinding binding;
    private FirebaseFirestore db;
    private List<Product> favProducts = new ArrayList<>();
    private FavouriteProductAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFavouriteBinding.inflate(inflater, container, false);

        if (isAdded() && getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
        }
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.favBackBtn.setOnClickListener(v -> {

            if (isAdded() && getActivity() != null) {
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .addToBackStack(null)
                        .commit();

//                com.google.android.material.bottomnavigation.BottomNavigationView bn = getActivity().findViewById(R.id.bottom_navigation);
//                bn.setSelectedItemId(R.id.bottom_nav_home);
//
//                NavigationView nv = getActivity().findViewById(R.id.side_navigation);
//                nv.setCheckedItem(R.id.side_nav_home);

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

        // get products
        getFavProducts();

    }

    private void setupRecycler() {
        adapter = new FavouriteProductAdapter(favProducts, new FavouriteProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
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
        }, new FavouriteProductAdapter.OnFavRemoveListener() {
            @Override
            public void onRemoveClick(int position) {
                FavouriteManager.removeFav(requireContext(), favProducts.get(position).getId());
                favProducts.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, favProducts.size());
            }
        });

        binding.favItemsRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.favItemsRecycler.setAdapter(adapter);
    }


//    private void getFavProducts() {
//        Set<String> favs = FavouriteManager.getFavs(requireContext());
//
//        if (favs == null || favs.isEmpty()) {
//            favProducts.clear();
//            setupRecycler();
//            return;
//            ///
//        }
//
//        favProducts.clear();
//        favs.forEach(s -> {
//            db.collection("products")
//                    .whereEqualTo("id", s)
//                    .get()
//                    .addOnSuccessListener(queryDocumentSnapshots -> {
//
//                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
//                            Product product = queryDocumentSnapshots.getDocuments().get(0).toObject(Product.class);
//                            favProducts.add(product);
//                        }
//
//                    });
//
//        });
//        setupRecycler();
//    }

    private void getFavProducts() {

        Set<String> favs = FavouriteManager.getFavs(requireContext());

        favProducts.clear();

        if (favs == null || favs.isEmpty()) {
            setupRecycler();
            return;
        }

        db.collection("product")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    favProducts.clear();

                    for (var doc : queryDocumentSnapshots.getDocuments()) {

                        Product product = doc.toObject(Product.class);

                        if (product != null && product.getId() != null) {

                            if (favs.contains(product.getId())) {
                                favProducts.add(product);
                            }
                        }
                    }

                    setupRecycler();
                });
    }
}