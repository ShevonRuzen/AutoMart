package com.shehan.automart.fragment;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.shehan.automart.R;
import com.shehan.automart.adapter.CategoryAdapter;
import com.shehan.automart.adapter.FeaturedItemAdapter;
import com.shehan.automart.databinding.FragmentHomeBinding;
import com.shehan.automart.model.Category;
import com.shehan.automart.model.SpecialMenu;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private List<Category> categoryList = new ArrayList<>();
    private List<SpecialMenu> featuredList = new ArrayList<>();
    private CategoryAdapter categoryAdapter;
    private FeaturedItemAdapter featuredItemAdapter;
    private FirebaseFirestore db;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private long lastTime = 0;
    private long lastShakeTime = 0;
    private static final long SHAKE_COOLDOWN_MS = 1200;
    private static final float SHAKE_THRESHOLD = 800;


    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (isAdded() && getActivity() != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.VISIBLE);
        }

        loadCategories();
        loadSpecialMenuItems();

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        startFlashSaleCountdown();
    }


    private final SensorEventListener shakeListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            long currentTime = System.currentTimeMillis();

            if ((currentTime - lastTime) > 100) {
                long diffTime = currentTime - lastTime;
                lastTime = currentTime;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD && (currentTime - lastShakeTime) > SHAKE_COOLDOWN_MS) {
                    lastShakeTime = currentTime;
                    onShakeDetected();
                }

                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    };

    private void onShakeDetected() {
        binding.homeLoadingContainer.setVisibility(View.VISIBLE);

        loadCategories();
        loadSpecialMenuItems();
        Toast.makeText(requireContext(), "Reloading!", Toast.LENGTH_SHORT).show();
    }

    private void loadSpecialMenuItems() {
        binding.homeFeaturedList.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        db.collection("special-menu").get().addOnSuccessListener(qds -> {
            if (qds == null || qds.isEmpty()) return;


            if (binding.homeLoadingContainer.getVisibility() == View.VISIBLE) {
                binding.homeLoadingContainer.setVisibility(View.GONE);
            }

            featuredList.clear();
            featuredList.addAll(qds.toObjects(SpecialMenu.class));

            featuredItemAdapter = new FeaturedItemAdapter(featuredList, product -> {

                Bundle bundle = new Bundle();
                bundle.putString("productId", product.getId());

                SingleProductViewFragment fragment = new SingleProductViewFragment();
                fragment.setArguments(bundle);

                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });

            binding.homeFeaturedList.setAdapter(featuredItemAdapter);
            if (binding.homeLoadingContainer.getVisibility() == View.VISIBLE) {
                binding.homeLoadingContainer.setVisibility(View.GONE);
            }
        });

    }

    private void loadCategories() {
        binding.homeCategoryList.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        db.collection("categories").orderBy("id").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {

                categoryList.clear();
                categoryList = task.getResult().toObjects(Category.class);

                categoryAdapter = new CategoryAdapter(categoryList, position -> {
                    if (isAdded() && getActivity() != null) {

                        Bundle bundle = new Bundle();
                        bundle.putInt("position", position);

                        CategoryFragment categoryFragment = new CategoryFragment();
                        categoryFragment.setArguments(bundle);

                        getParentFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, categoryFragment)
                                .addToBackStack(null)
                                .commit();

                        BottomNavigationView bottomNav =
                                getActivity().findViewById(R.id.bottom_navigation);
                        bottomNav.setSelectedItemId(R.id.bottom_nav_category);
                    }
                });

                binding.homeCategoryList.setAdapter(categoryAdapter);
            }
        });
    }


    private void startFlashSaleCountdown() {

        long totalTime = 2 * 60 * 60 * 1000; // 2 hours

        new CountDownTimer(totalTime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {

                long hours = millisUntilFinished / (1000 * 60 * 60);
                long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                long seconds = (millisUntilFinished / 1000) % 60;

                binding.tvHours.setText(String.format("%02d", hours));
                binding.tvMinutes.setText(String.format("%02d", minutes));
                binding.tvSeconds.setText(String.format("%02d", seconds));
            }

            @Override
            public void onFinish() {
                binding.tvHours.setText("00");
                binding.tvMinutes.setText("00");
                binding.tvSeconds.setText("00");
            }

        }.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }
}