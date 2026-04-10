package com.shehan.automart.fragment;


import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.shehan.automart.R;

import java.util.Objects;



public class SettingFragment extends Fragment {

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
    }

    @Override
    public void onResume() {
        super.onResume();
//        updateToolbar(true);
    }

//    private void updateToolbar(boolean b) {
//        boolean isNotCategoryOrHomeFragment = b;
//
//        View homeNav = getActivity().findViewById(R.id.home_toolbar);
//        View catNav = getActivity().findViewById(R.id.category_toolbar);
//        View otherNav = getActivity().findViewById(R.id.other_toolbar);
//
//        if (isNotCategoryOrHomeFragment){
//            homeNav.setVisibility(View.GONE);
//            catNav.setVisibility(View.GONE);
//            otherNav.setVisibility(View.VISIBLE);
//        }
//    }
}