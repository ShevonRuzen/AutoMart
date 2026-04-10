package com.shehan.automart.activity;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.R;
import com.shehan.automart.databinding.ActivityMainBinding;
import com.shehan.automart.fragment.AboutFragment;
import com.shehan.automart.fragment.AddressListFragment;
import com.shehan.automart.fragment.CartFragment;
import com.shehan.automart.fragment.CategoryFragment;
import com.shehan.automart.fragment.FavouriteFragment;
import com.shehan.automart.fragment.HomeFragment;
import com.shehan.automart.fragment.NotificationFragment;
import com.shehan.automart.fragment.OrderFragment;
import com.shehan.automart.fragment.ProfileFragment;
import com.shehan.automart.fragment.SettingFragment;
import com.shehan.automart.model.Notification;

import java.util.List;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private NavigationView sideNavigation;
    private NavigationBarView bottomNavigation;
    private MaterialToolbar toolbar;
    private DrawerLayout drawerLayout;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private static final String CHANNEL_ID = "order_status_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        createNotificationChannel();


        this.toolbar = binding.homeToolbar;
        this.drawerLayout = binding.main;

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        this.sideNavigation = binding.sideNavigation;
        this.bottomNavigation = binding.bottomNavigation;

        sideNavigation.setNavigationItemSelectedListener(this);
        bottomNavigation.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            sideNavigation.getMenu().findItem(R.id.side_nav_home).setChecked(true);
            bottomNavigation.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUser = mAuth.getCurrentUser();
        }
        db = FirebaseFirestore.getInstance();

        notificationIcon();
        binding.btnNotificationContainer.setOnClickListener(v -> {
            loadFragment(new NotificationFragment());
            sideNavigation.setCheckedItem(R.id.side_nav_notification);
        });


//        binding.mainAddress.setOnClickListener(v -> {
//            loadFragment(new AddressListFragment());
//        });


//        loadAddress();
        updateSideNavVisibility();

    }

//    private void loadAddress() {
//        if (currentUser == null) {
//            binding.mainTitleTxt.setVisibility(View.VISIBLE);
//            binding.mainAddressTxt.setVisibility(View.GONE);
//        } else {
//            db.collection("address")
//                    .whereEqualTo("user_id", currentUser.getUid()).whereEqualTo("checked", true)
//                    .get()
//                    .addOnSuccessListener(documentSnapshot -> {
//                        if (documentSnapshot == null || documentSnapshot.isEmpty()) {
//                            binding.mainTitleTxt.setVisibility(View.VISIBLE);
//                            binding.mainAddressTxt.setVisibility(View.GONE);
//                            return;
//                        }
//
//                        String homeName = documentSnapshot.getDocuments().get(0).getString("home_name");
//
//                        if (homeName != null && !homeName.trim().isEmpty()) {
//                            binding.mainTitleTxt.setVisibility(View.GONE);
//                            binding.mainAddressTxt.setVisibility(View.VISIBLE);
//                            binding.mainAddressTxt.setText(homeName);
//                        } else {
//                            binding.mainTitleTxt.setVisibility(View.VISIBLE);
//                            binding.mainAddressTxt.setVisibility(View.GONE);
//                        }
//                    })
//                    .addOnFailureListener(e -> {
//                        binding.mainTitleTxt.setVisibility(View.VISIBLE);
//                        binding.mainAddressTxt.setVisibility(View.GONE);
//                    });
//        }
//    }

    private void updateSideNavVisibility() {
        Menu menu = sideNavigation.getMenu();

        boolean isLoggedIn = currentUser != null;

        menu.findItem(R.id.side_nav_login).setVisible(!isLoggedIn);

        menu.findItem(R.id.side_nav_profile).setVisible(isLoggedIn);
        menu.findItem(R.id.side_nav_order).setVisible(isLoggedIn);
        menu.findItem(R.id.side_nav_favorite).setVisible(isLoggedIn);
        menu.findItem(R.id.side_nav_cart).setVisible(isLoggedIn);
        menu.findItem(R.id.side_nav_notification).setVisible(isLoggedIn);
        menu.findItem(R.id.side_nav_logout).setVisible(isLoggedIn);

        // always visible items
        menu.findItem(R.id.side_nav_home).setVisible(true);
        menu.findItem(R.id.side_nav_about).setVisible(true);
        menu.findItem(R.id.side_nav_setting).setVisible(true);
    }


    public void reloadAddress() {
//        loadAddress();
    }


    int coutn = 0;

    private void notificationIcon() {
        if (currentUser == null){
            binding.notificationBadgeContainer.setVisibility(View.GONE);
            return;
        }
        coutn = 0;
        db.collection("notifications")
                .whereEqualTo("user_doc_id", currentUser.getUid())
                .get()
                .addOnSuccessListener(qds -> {
                    if (qds == null || qds.isEmpty()) {
                        binding.notificationBadgeContainer.setVisibility(View.GONE);
                    }

                    binding.notificationCount.setVisibility(View.VISIBLE);

                    if (qds.size() == 0) {
                        binding.notificationBadgeContainer.setVisibility(View.GONE);
                    } else {
                        List<Notification> list = qds.toObjects(Notification.class);

                        list.forEach(notification -> {
                            if (!notification.isRead()) {
                                coutn++;
                            }
                        });

                        if (coutn > 10) {
                            binding.notificationCount.setText("10+");
                        } else if (coutn < 10) {
                            binding.notificationCount.setText(String.valueOf(coutn));
                        }
                    }
                });
    }


    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        sideNavigation.setCheckedItem(-1);

        Menu sideNavigationMenu = sideNavigation.getMenu();
        Menu bottomNavigationMenu = bottomNavigation.getMenu();

        for (int i = 0; i < sideNavigationMenu.size(); i++) {
            sideNavigationMenu.getItem(i).setChecked(false);
        }
        for (int i = 0; i < bottomNavigationMenu.size(); i++) {
            bottomNavigationMenu.getItem(i).setChecked(false);
        }
        if (itemId == R.id.side_nav_home || itemId == R.id.bottom_nav_home) {
            loadFragment(new HomeFragment());
        }
        if (itemId == R.id.side_nav_order || itemId == R.id.bottom_nav_orders) {
            loadFragment(new OrderFragment());
        }
        if (itemId == R.id.side_nav_cart) {
            loadFragment(new CartFragment());
        }
        if (itemId == R.id.bottom_nav_category) {
            loadFragment(new CategoryFragment());
        }
        if (itemId == R.id.side_nav_favorite) {
            loadFragment(new FavouriteFragment());
        }
        if (itemId == R.id.side_nav_profile || itemId == R.id.bottom_nav_profile) {
            loadFragment(new ProfileFragment());
        }
        if (itemId == R.id.side_nav_about) {
            loadFragment(new AboutFragment());
        }
        if (itemId == R.id.side_nav_notification) {
            loadFragment(new NotificationFragment());
        }
        if (itemId == R.id.side_nav_setting) {
            loadFragment(new SettingFragment());
        }
        if (itemId == R.id.side_nav_login) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        if (itemId == R.id.side_nav_logout) {
            if (currentUser != null) {
                mAuth.signOut();
                currentUser = null;
                updateSideNavVisibility();
//                loadAddress();
                notificationIcon();
                loadFragment(new HomeFragment());
                Toast.makeText(this, "User Logged out", Toast.LENGTH_SHORT).show();
            }
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Order status updated",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications about your order updates");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        updateSideNavVisibility();
//        loadAddress();
    }
}
