package com.shehan.automart.fragment;


import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.shehan.automart.R;
import com.shehan.automart.databinding.FragmentAboutBinding;


public class AboutFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FragmentAboutBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAboutBinding.inflate(inflater, container, false);

        if (isAdded() && getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.about_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
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


        binding.aboutContactMobile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:0766940120"));
            startActivity(intent);
        });

        binding.aboutContactMessage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setData(Uri.parse("smsto:0766940120"));
            intent.putExtra("sms_body", "Hello, AutoMart");
            startActivity(intent);
        });

        binding.aboutContactWhatsapp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("text/plain");
            intent.setData(Uri.parse("https://wa.me/+94766940120?text=Hello%20I%20need%20help"));
            startActivity(intent);
        });

        binding.aboutContactEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:softlyrex.info@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Order support");
            intent.putExtra(Intent.EXTRA_TEXT, "Hello, I need a help with..");
            startActivity(intent);
        });


        binding.aboutBackBtn.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                getParentFragmentManager().popBackStack();
                BottomNavigationView bn = getActivity().findViewById(R.id.bottom_navigation);
                if (bn != null) {
                    bn.setSelectedItemId(R.id.bottom_nav_home);
                }

                com.google.android.material.navigation.NavigationView nv = getActivity().findViewById(R.id.side_navigation);
                if (nv != null) {
                    nv.setCheckedItem(R.id.side_nav_home);
                }
            }
        });

    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {

        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // add restaurant location
        LatLng restaurantLocation = new LatLng(6.9195317, 79.8643593);
        mMap.addMarker(new MarkerOptions().position(restaurantLocation).title("AutoMart"));
        mMap.moveCamera(newLatLngZoom(restaurantLocation, 15f));
    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        if (isAdded() && getActivity() != null) {
//            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
//            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
//        }
//    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        View toolbar = getActivity().findViewById(R.id.home_toolbar);
        View bottomNav = getActivity().findViewById(R.id.bottom_navigation);

        if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
        if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

        binding = null;
    }
}