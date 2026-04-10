package com.shehan.automart.fragment;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;
import com.shehan.automart.adapter.NotificationAdapter;
import com.shehan.automart.databinding.FragmentNotificationBinding;
import com.shehan.automart.model.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;




public class NotificationFragment extends Fragment {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private NotificationAdapter adapter;
    private FragmentNotificationBinding binding;
    private List<Notification> notifications = new ArrayList<>();
    private boolean isNotificationsEmpty = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {   // no current user
            navigateLogin();
            return;
        }
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (isAdded() && getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
        }

        binding.notificationBackBtn.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container,
                                new HomeFragment()).addToBackStack(null).commit();
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

        getNotificationData();

    }

    private void getNotificationData() {
        if (user == null) {
            navigateLogin();
            return;
        }

        db.collection("notifications").whereEqualTo("user_doc_id", user.getUid()).get().addOnSuccessListener(qs -> {
            if (qs == null || qs.isEmpty()) {  //no notifications
//                TODO => set empty container visible
                isNotificationsEmpty = true;
                setupLayoutsVisibility(isNotificationsEmpty);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }

                return;
            }

            isNotificationsEmpty = false;
            setupLayoutsVisibility(false);


            notifications.clear();
            qs.forEach(qds -> {
                if (qds == null) {
                    return;
                }
                Notification notification = qds.toObject(Notification.class);
                if (notification == null) return;

                notifications.add(notification);
            });

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            } else {
                getRecyclerView();
            }

        }).addOnFailureListener(e -> { // notification loading fail
            if (isAdded() && getActivity() != null) {
                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container,
                                new HomeFragment()).commit();
            }
            return;
        });
    }

    private void getRecyclerView() {
        if (notifications.isEmpty()) {
            isNotificationsEmpty = true;
            setupLayoutsVisibility(isNotificationsEmpty);
            return;
        }


        isNotificationsEmpty = false;
        setupLayoutsVisibility(false);


        if (adapter == null) {
            adapter = new NotificationAdapter(notifications, new NotificationAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {
                    //update read ===> true
                    db.collection("notifications")
                            .document(notifications.get(position)
                                    .getNotification_doc_id())
                            .update("read", true)
                            .addOnSuccessListener(unused -> {
                                if (!isAdded() || getActivity() == null) return;

                                if (adapter != null) {
                                    notifications.get(position).setRead(true);
                                    adapter.notifyItemChanged(position);
                                }
                            }).addOnFailureListener(e -> {
                                return;
                            });

                    Bundle bundle = new Bundle();
                    bundle.putString("order_doc_id", notifications.get(position).getOrder_doc_id());

                    OrderDetailsFragment orderDetailsFragment = new OrderDetailsFragment();
                    orderDetailsFragment.setArguments(bundle);

                    if (isAdded() && getActivity() != null) {
                        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, orderDetailsFragment).addToBackStack(null).commit();
                    }
                }
            }, new NotificationAdapter.OnRemoveClickListener() {
                @Override
                public void onRemoveClick(int position) {

                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Delete Notification")
                            .setMessage("Are you sure you want to delete this notification?")
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .setPositiveButton("Yes", (dialog, which) -> {

                                db.collection("notifications")
                                        .document(notifications
                                                .get(position)
                                                .getNotification_doc_id())
                                        .delete()
                                        .addOnSuccessListener(unused -> {
                                            if (!isAdded() || getActivity() == null) return;

                                            notifications.remove(position);

                                            if (adapter != null) {
                                                adapter.notifyItemRemoved(position);
                                            }

                                            if (notifications.isEmpty()) {
                                                setupLayoutsVisibility(true);
                                            }
                                        });

                            }).show();
                }
            });
            binding.notificationItemRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
            binding.notificationItemRecycler.setAdapter(adapter);

        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void setupLayoutsVisibility(boolean isEmpty) {
        if (isEmpty) {
            binding.notificationEmptyContainer.setVisibility(View.VISIBLE);
            binding.notificationItemRecyclerContainer.setVisibility(View.GONE);
        } else {
            binding.notificationEmptyContainer.setVisibility(View.GONE);
            binding.notificationItemRecyclerContainer.setVisibility(View.VISIBLE);
        }
    }

    private void navigateLogin() {
        Intent intent = new Intent(this.getContext(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
        return;
    }

    @Override
    public void onResume() {
        super.onResume();
        getNotificationData();
    }
}