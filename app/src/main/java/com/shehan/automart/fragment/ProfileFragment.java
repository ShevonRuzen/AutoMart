package com.shehan.automart.fragment;


import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;
import com.shehan.automart.databinding.FragmentProfileBinding;
import com.shehan.automart.model.User;

import java.util.Objects;




public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private boolean isPickingImage = false;

    private FirebaseStorage storage;

    private StorageReference storageReference;

    private boolean imgPickOpen = false;

    public ProfileFragment() {
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
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.profileLocation.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new AddressListFragment()).addToBackStack(null).commit();
        });

        if (isAdded() && getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
        }

        loadProfileDetails();

        binding.profileCameraBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");

            imgPickOpen = true;

            imagePickerLauncher.launch(intent);
        });

        binding.profileBackBtn.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).addToBackStack(null).commit();
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


        binding.profileEditProfile.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                if (binding.profileEditProfileForm.getVisibility() == View.GONE){
                    binding.profileEditProfileForm.setVisibility(View.VISIBLE);
                }else {
                    binding.profileEditProfileForm.setVisibility(View.GONE);
                }
            }
        });


        binding.profileEditSave.setOnClickListener(v -> {
            if (currentUser == null){
                Intent intent = new Intent(this.getContext(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
                return;
            }

            if (isAdded() && getActivity() != null) {
                String name = binding.profileEditProfileName.getText().toString().trim();
                String mobile = binding.profileEditProfileMobile.getText().toString().trim();

                if (name.isEmpty()){
                    binding.profileEditProfileName.setError("Name is required");
                    binding.profileEditProfileName.requestFocus();
                    return;
                }

                if (mobile.isEmpty()){
                    binding.profileEditProfileMobile.setError("Mobile number is required");
                    binding.profileEditProfileMobile.requestFocus();
                    return;
                }

                if (!mobile.matches("^07[01245678][0-9]{7}$")){
                    binding.profileEditProfileMobile.setError("Mobile number is invalid");
                    binding.profileEditProfileMobile.requestFocus();
                    return;
                }

                db.collection("user")
                        .document(currentUser.getUid())
                        .update("name", name , "phoneNumber", mobile)
                        .addOnSuccessListener(unused -> {
                            if (isAdded() && binding != null) {
                                Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();

                                binding.profileName.setText(name);
                                binding.profilePhone.setText(mobile);

                                binding.profileEditProfileForm.setVisibility(View.GONE);

                                binding.profileEditProfileName.clearFocus();
                                binding.profileEditProfileMobile.clearFocus();
                            }

                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });


        binding.profileLogout.setOnClickListener(v -> {
            mAuth.signOut();
            currentUser = null;
            navigateLogin();
        });

    }

    private void loadProfileDetails() {
        if (currentUser == null || currentUser.getUid() == null) {
            navigateLogin();
            return;
        }

        if (!isAdded() || getActivity() == null || binding == null) {
            return;
        }

        db.collection("user")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(ds -> {

                    if (ds == null || !ds.exists()) {
                        navigateLogin();
                        return;
                    }

                    User user = ds.toObject(User.class);
                    if (user == null) {
                        navigateLogin();
                        return;
                    }


                    Glide.with(this)
                            .load(user.getProfileImage())
                            .error("https://uxwing.com/wp-content/themes/uxwing/download/peoples-avatars/man-user-circle-icon.png")
                            .circleCrop()
                            .into(binding.profileImage);

                    binding.profileName.setText(user.getName());
                    binding.profileEmail.setText(user.getEmail());
                    binding.profilePhone.setText(user.getPhoneNumber()!= null ? user.getPhoneNumber(): "");

                }).addOnFailureListener(e -> {
                    Toast.makeText(this.getContext(), "User details loading fail", Toast.LENGTH_SHORT)
                            .show();
                    navigateLogin();
                    return;
                });
    }

    ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            Uri imgUri = result.getData().getData();
            if (imgUri != null) {

                // set image to profile image view
                Glide.with(this)
                        .load(imgUri)
                        .circleCrop()
                        .into(binding.profileImage);

                isPickingImage = false;

                // save image to the db
                uploadImage(imgUri);
            }
        }
    });

    private void uploadImage(Uri uri) {   // upload profile image to firebase storage
        if (currentUser == null) {
            Toast.makeText(this.getContext(), "user not logged in", Toast.LENGTH_SHORT).show();
            navigateLogin();
            return;
        }

        StorageReference profileImageRef = storageReference.child("profile-images/" + currentUser.getUid() + ".jpg");
        profileImageRef
                .putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {

                    profileImageRef.getDownloadUrl().addOnSuccessListener(imgUri -> {
                        String downloadImgUri = imgUri.toString();

                        imgPickOpen = false;

                        // save uri in db
                        saveImg(downloadImgUri);

                    }).addOnFailureListener(e -> {
                        Toast.makeText(this.getContext(), "Failed to get image", Toast.LENGTH_SHORT).show();
                    });

                }).addOnFailureListener(e -> {
                    Toast.makeText(this.getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveImg(String downloadImgUri) {
        if (currentUser == null) {
            return;
        } else {
            db.collection("user").document(currentUser.getUid()).update("profileImage", downloadImgUri).addOnSuccessListener(unused -> {
                Toast.makeText(this.getContext(), "Image updated", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(this.getContext(), "Failed to update image", Toast.LENGTH_SHORT).show();
            });
            currentUser.getUid();
        }
    }


    private void navigateLogin() {
        if (isAdded() && getActivity() != null) {
            Intent intent = new Intent(this.getContext(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
            return;
        }
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        if (getActivity() != null && getActivity().findViewById(R.id.home_toolbar) != null && getActivity().findViewById(R.id.bottom_navigation) != null) {
//            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
//        }
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        if (getActivity() != null && !imgPickOpen) {
//            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
//        }
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        if (getActivity() != null) {
//            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
//        }
//    }


    @Override
    public void onStart() {
        super.onStart();
        if (currentUser == null){
            Intent intent = new Intent(this.getContext(),LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        }
    }
}