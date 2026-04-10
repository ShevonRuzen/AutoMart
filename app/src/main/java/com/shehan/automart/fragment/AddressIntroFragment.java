package com.shehan.automart.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;


public class AddressIntroFragment extends Fragment {

    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_intro_address, container, false);
    }


    @Override
    public void onStart() {
        super.onStart();
        if (mAuth == null || mAuth.getCurrentUser() == null){
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }
}