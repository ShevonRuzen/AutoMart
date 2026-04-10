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
import com.shehan.automart.activity.MainActivity;
import com.shehan.automart.adapter.AddressListAdapter;
import com.shehan.automart.databinding.FragmentAddressListBinding;
import com.shehan.automart.model.Address;

import java.util.ArrayList;
import java.util.List;




public class AddressListFragment extends Fragment {

    private FragmentAddressListBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    private List<Address> addressList = new ArrayList<>();
    private AddressListAdapter addressListAdapter;

    private int checkedPosition =-1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAddressListBinding.inflate(inflater, container, false);


        if (getActivity() != null) {
            View toolbar = getActivity().findViewById(R.id.home_toolbar);
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation);

            if (toolbar != null) toolbar.setVisibility(View.GONE);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        mAuth = FirebaseAuth.getInstance();

        if (mAuth == null || mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this.requireContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();


        setupRecycle();
        loadAddresses();


        binding.addressListBackBtn.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new ProfileFragment()).addToBackStack(null).commit();

        });


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



        binding.addressListAddNew.setOnClickListener(v -> {
            if (user == null) {
                Intent intent = new Intent(this.requireContext(), LoginActivity.class);
                startActivity(intent);
                return;
            }

            if (addressList.size() >= 3){
                return;
            }

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AddressFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadAddresses() {
        db.collection("address")
                .whereEqualTo("user_id", user.getUid())
                .get()
                .addOnSuccessListener(qds -> {
                    if (!isAdded() || binding == null) return;

                    addressList.clear();

                    if (qds != null && !qds.isEmpty()) {
                        addressList.addAll(qds.toObjects(Address.class));

                        addressList.forEach(address -> {
                            if (address.isChecked()){
                                checkedPosition=addressList.indexOf(address);
                            }
                        });
                    }

                    addressListAdapter.notifyDataSetChanged();
                    updateRecyclerViewVisibility();
                    updateAddNewState();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || binding == null) return;
                    binding.addressListRecycler.setVisibility(View.GONE);
                });
    }



    private void setupRecycle() {
        binding.addressListRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        binding.addressListRecycler.setNestedScrollingEnabled(false);

        addressListAdapter = new AddressListAdapter(addressList, new AddressListAdapter.OnEditListener() {
            @Override
            public void onEdit(int position) {
                Bundle bundle = new Bundle();
                bundle.putString("address_doc_id", addressList.get(position).getAddress_doc_id());
                AddressFragment addressFragment = new AddressFragment();
                addressFragment.setArguments(bundle);
                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, addressFragment).addToBackStack(null).commit();
            }
        }, new AddressListAdapter.OnRemoveListener() {
            @Override
            public void onRemove(int position) {
                db.collection("address").document(addressList.get(position).getAddress_doc_id()).delete().addOnSuccessListener(unused -> {
                    addressList.remove(position);
                    addressListAdapter.notifyItemRemoved(position);
                    addressListAdapter.notifyItemRangeChanged(position, addressList.size());
                    updateAddNewState();
                    updateRecyclerViewVisibility();

                    if (checkedPosition == position){
                        checkedPosition = -1;
                    }else if (position < checkedPosition){
                        checkedPosition--;
                    }
                });
            }
        }, new AddressListAdapter.OnCheckListener() {
            @Override
            public void onCheck(int position) {
                if (position == checkedPosition) return;

                String newId = addressList.get(position).getAddress_doc_id();

                if (checkedPosition == -1) {
                    db.collection("address").document(newId).update("checked", true)
                            .addOnSuccessListener(unused -> {
                                addressList.get(position).setChecked(true);
                                checkedPosition = position;
                                addressListAdapter.notifyItemChanged(position);
//                                ((MainActivity) requireActivity()).reloadAddress();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show());
                    return;
                }

                int oldPosition = checkedPosition;
                String oldId = addressList.get(oldPosition).getAddress_doc_id();

                db.collection("address").document(oldId).update("checked", false)
                        .addOnSuccessListener(unused -> {
                            db.collection("address").document(newId).update("checked", true)
                                    .addOnSuccessListener(unused2 -> {
                                        addressList.get(oldPosition).setChecked(false);
                                        addressList.get(position).setChecked(true);
                                        checkedPosition = position;

                                        addressListAdapter.notifyItemChanged(oldPosition);
                                        addressListAdapter.notifyItemChanged(position);

//                                        ((MainActivity) requireActivity()).reloadAddress();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show());
            }
        });
        binding.addressListRecycler.setAdapter(addressListAdapter);
    }




    private void updateRecyclerViewVisibility() {
        if (binding == null) return;

        if (addressList.isEmpty()) {
            binding.addressListRecycler.setVisibility(View.GONE);
        } else {
            binding.addressListRecycler.setVisibility(View.VISIBLE);
        }
    }

    private void updateAddNewState() {

        if (binding == null) return;

        if (addressList.size() >= 3) {
            binding.addressListAddNew.setEnabled(false);
            binding.addressListAddNew.setAlpha(0.5f); // Makes the whole card look faded/disabled
            binding.addressListAddNewText.setText("Maximum addresses reached");
        } else {
            binding.addressListAddNew.setEnabled(true);
            binding.addressListAddNew.setAlpha(1.0f);
            binding.addressListAddNewText.setText("Add New Address");
        }
    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        if (getActivity() != null) {
//            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
//            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
//        }
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        if (getActivity() != null) {
//            View toolbar = getActivity().findViewById(R.id.home_toolbar);
//            View bottomNav = getActivity().findViewById(R.id.bottom_navigation);
//
//            if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
//            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
//        }
//    }


    @Override
    public void onPause() {
        super.onPause();
        if (getActivity() != null) {
            View toolbar = getActivity().findViewById(R.id.home_toolbar);
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation);

            if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            View toolbar = getActivity().findViewById(R.id.home_toolbar);
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation);

            if (toolbar != null) toolbar.setVisibility(View.VISIBLE);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

    }


    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            View toolbar = getActivity().findViewById(R.id.home_toolbar);
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation);

            if (toolbar != null) toolbar.setVisibility(View.GONE);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }
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