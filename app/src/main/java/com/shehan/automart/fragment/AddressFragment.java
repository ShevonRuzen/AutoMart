package com.shehan.automart.fragment;

import static com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.security.MessageDigest;
import com.shehan.automart.BuildConfig;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.maps.android.PolyUtil;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import com.shehan.automart.R;
import com.shehan.automart.activity.LoginActivity;
import com.shehan.automart.databinding.FragmentAddressBinding;
import com.shehan.automart.services.DirectionApi;
import com.shehan.automart.services.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FragmentAddressBinding binding;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private LatLng currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Marker markerPin;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private String address_doc_id;
    private com.shehan.automart.model.Address address;

    private double shippingFee = 0;
    private Polyline polyline;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            this.address_doc_id = getArguments().getString("address_doc_id");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentAddressBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null && getActivity().findViewById(R.id.home_toolbar) != null && getActivity().findViewById(R.id.bottom_navigation) != null) {
            getActivity().findViewById(R.id.home_toolbar).setVisibility(View.GONE);
            getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.GONE);
        }


        mAuth = FirebaseAuth.getInstance();
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            currentUser = mAuth.getCurrentUser();
            db = FirebaseFirestore.getInstance();
        }


//        back button
        binding.addressBackBtn.setOnClickListener(v ->
                getParentFragmentManager().popBackStack());


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


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000).build();


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lng = locationResult.getLastLocation().getLongitude();

                    currentLocation = new LatLng(lat, lng);

                    if (mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                    }
                }
            }
        };

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.address_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


//        address select btn
        binding.addressBtnSelectLocation.setOnClickListener(v -> {
            if (markerPin != null) {

                LatLng selectedLocation = markerPin.getPosition();

                fillAddressFormLatLng(selectedLocation);

                LatLng restaurantLocation = new LatLng(6.9195317, 79.8643593);
                getDirectionApi(restaurantLocation, selectedLocation);
            } else {
                Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show();
            }
        });


        // if address_doc_id is not null, then edit address
        if (address_doc_id != null) {

            db.collection("address").document(address_doc_id).get().addOnSuccessListener(ds -> {
                if (!isAdded() || binding == null) return;

                if (!ds.exists()) {
                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                }

                address = ds.toObject(com.shehan.automart.model.Address.class);
                if (address == null) {
                    Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                    return;
                } else {
                    binding.addressHomeName.setText(address.getHome_name());
                    binding.addressLine1.setText(address.getAddress_line1());
                    binding.addressLine2.setText(address.getAddress_line2());
                    binding.addressCity.setText(address.getCity());
                    binding.addressPostalCode.setText(address.getPostal_code());
                }
            });

        }


//        address save btn
        binding.addressSaveBtn.setOnClickListener(v -> {
            saveAddress();
        });
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // add restaurant location
        LatLng restaurantLocation = new LatLng(6.9195317, 79.8643593);
        mMap.addMarker(new MarkerOptions().position(restaurantLocation).title("Folk|Flame Restaurant"));
        mMap.moveCamera(newLatLngZoom(restaurantLocation, 15f));

        checkLocationPermissionAndFetch();

        mMap.setOnMapClickListener(latLng -> {
            if (markerPin == null) {
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
                markerOptions.title("selected location");
                markerPin = mMap.addMarker(markerOptions);
            } else {
                markerPin.setPosition(latLng);
            }

        });
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLocationAndFetch();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableLocationAndFetch();
                } else {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });


    private void enableLocationAndFetch() {
        if (mMap == null) return;

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        getUserCurrentLocation();
    }

    private void fillAddressFormLatLng(LatLng latlng) {

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        if (!isAdded() || binding == null) return;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latlng.latitude, latlng.longitude, 1, addresses -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address address = addresses.get(0);

                        binding.addressHomeName.setText(address.getFeatureName() != null ? address.getFeatureName() : "");
                        binding.addressLine1.setText(address.getThoroughfare() != null ? address.getThoroughfare() : "");
                        binding.addressLine2.setText(address.getSubLocality() != null ? address.getSubLocality() : "");
                        binding.addressCity.setText(address.getLocality() != null ? address.getLocality() : "");
                        binding.addressPostalCode.setText(address.getPostalCode() != null ? address.getPostalCode() : "");
                    }
                });
            } else {


                List<Address> addresses = geocoder.getFromLocation(latlng.latitude, latlng.longitude, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    binding.addressHomeName.setText(address.getFeatureName() != null ? address.getFeatureName() : "");
                    binding.addressLine1.setText(address.getThoroughfare() != null ? address.getThoroughfare() : "");
                    binding.addressLine2.setText(address.getSubLocality() != null ? address.getSubLocality() : "");
                    binding.addressCity.setText(address.getLocality() != null ? address.getLocality() : "");
                    binding.addressPostalCode.setText(address.getPostalCode() != null ? address.getPostalCode() : "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (!isAdded() || getContext() == null) return;
            Toast.makeText(requireContext(), "Failed to fetch address. Check your internet and try again.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            if (!isAdded() || getContext() == null) return;
            Toast.makeText(requireContext(), "Something went wrong while fetching address", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAddress() {
        try {

            if (currentUser == null) return;

            String home = binding.addressHomeName.getText().toString().trim();
            String line1 = binding.addressLine1.getText().toString().trim();
            String line2 = binding.addressLine2.getText().toString().trim();
            String city = binding.addressCity.getText().toString().trim();
            String postalcode = binding.addressPostalCode.getText().toString().trim();

            if (home.isEmpty()) {
                binding.addressHomeName.setError("Please enter home name");
                binding.addressHomeName.requestFocus();
                return;
            }

            if (line1.isEmpty()) {
                binding.addressLine1.setError("Please enter line 1");
                binding.addressLine1.requestFocus();
                return;
            }

            if (line2.isEmpty()) {
                binding.addressLine2.setError("Please enter line 2");
                binding.addressLine2.requestFocus();
                return;
            }

            if (city.isEmpty()) {
                binding.addressCity.setError("Please enter city");
                binding.addressCity.requestFocus();
                return;
            }

            if (postalcode.isEmpty()) {
                binding.addressPostalCode.setError("Please enter postal code");
                binding.addressPostalCode.requestFocus();
                return;
            }

            if (!postalcode.matches("^\\d{5}$")) {
                binding.addressPostalCode.setError("Please enter valid postal code");
                binding.addressPostalCode.requestFocus();
                return;
            }


            // create new address
            com.shehan.automart.model.Address address2 = com.shehan.automart.model.Address.builder()
                    .user_id(currentUser.getUid())
                    .home_name(home)
                    .address_line1(line1)
                    .address_line2(line2)
                    .city(city)
                    .postal_code(postalcode)
                    .checked(true)
                    .shippingFee(shippingFee == 0 ? 100 : shippingFee)
                    .build();



            db.collection("address").whereEqualTo("user_id", currentUser.getUid()).get()
                    .addOnSuccessListener(qds -> {
                        qds.forEach(queryDocumentSnapshot -> {
                            boolean checked = queryDocumentSnapshot.getBoolean("checked");

                            if (Boolean.TRUE.equals(checked)) {
                                queryDocumentSnapshot.getReference().update("checked", false);
                            }


                        });

                        //            if address_doc_id is not null, then update address
                        if (address_doc_id != null) {
                            db.collection("address").document(address_doc_id).set(address2).addOnSuccessListener(unused -> {
                                if (!isAdded() || getContext() == null) return;
                                Toast.makeText(requireContext(), "Address updated successfully", Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().popBackStack();
                            }).addOnFailureListener(e -> {
                                if (!isAdded() || getContext() == null) return;
                                Toast.makeText(requireContext(), "Error updating address", Toast.LENGTH_SHORT).show();
                            });
                        } else { // if address_doc_id is null, then add new address
                            //            adding address to the db
                            db.collection("address").add(address2).addOnSuccessListener(documentReference -> {
                                if (documentReference == null) {
                                    if (!isAdded() || getContext() == null) return;
                                    Toast.makeText(requireContext(), "Error adding address", Toast.LENGTH_SHORT).show();
                                    return;
                                } else {
                                    if (!isAdded() || getContext() == null) return;
                                    Toast.makeText(requireContext(), "Address added successfully", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager().popBackStack();
                                }
                            }).addOnFailureListener(e -> {
                                if (!isAdded() || getContext() == null) return;
                                Toast.makeText(requireContext(), "Error adding address", Toast.LENGTH_SHORT).show();
                            });
                        }


                        if (address != null) {
                            address = null;
                        }
                        if (markerPin != null) {
                            markerPin = null;
                        }
                    });

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLatlng = new LatLng(location.getLatitude(), location.getLongitude());

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatlng, 15f));
            }
        });
//        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

    }

    private void getDirectionApi(LatLng start, LatLng end) {
        shippingFee = 0;

        String origin = start.latitude + "," + start.longitude;
        String destination = end.latitude + "," + end.longitude;

        String key = BuildConfig.MAPS_API_KEY;
        if (key == null || key.trim().isEmpty()) {
            key = "AIzaSyAIRQHhy8GudAiWtvpeYKM3BZFmA6eeO8w";
        }

        String packageName = requireContext().getPackageName();
        String certSha1 = getSigningCertificateSHA1();

        DirectionApi api = RetrofitClient.getClient().create(DirectionApi.class);

        api.getJson(origin, destination, key, packageName, certSha1).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded() || binding == null) return;

                if (!response.isSuccessful() || response.body() == null) {
                    fallbackRouteAndFee(start, end);
                    return;
                }

                try {
                    JsonObject body = response.body();

                    String status = body.has("status") ? body.get("status").getAsString() : "UNKNOWN";
                    if (!"OK".equals(status)) {
                        String errorMsg = body.has("error_message") ? body.get("error_message").getAsString() : status;
                        Log.w("DirectionsAPI", "Directions API returned " + status + ": " + errorMsg);
                        fallbackRouteAndFee(start, end);
                        return;
                    }

                    JsonArray routes = body.getAsJsonArray("routes");
                    if (routes == null || routes.size() == 0) {
                        fallbackRouteAndFee(start, end);
                        return;
                    }

                    JsonObject route = routes.get(0).getAsJsonObject();

                    String encodedPath = route
                            .getAsJsonObject("overview_polyline")
                            .get("points")
                            .getAsString();

                    List<LatLng> points = PolyUtil.decode(encodedPath);

                    JsonArray legs = route.getAsJsonArray("legs");
                    JsonObject leg = legs.get(0).getAsJsonObject();

                    long distanceInMeters = leg
                            .getAsJsonObject("distance")
                            .get("value")
                            .getAsLong();

                    String distanceText = leg
                            .getAsJsonObject("distance")
                            .get("text")
                            .getAsString();

                    String durationText = leg
                            .getAsJsonObject("duration")
                            .get("text")
                            .getAsString();

                    if (polyline != null) {
                        polyline.remove();
                    }

                    PolylineOptions polylineOptions = new PolylineOptions()
                            .width(20)
                            .color(ContextCompat.getColor(requireContext(), R.color.colorCustomColor1))
                            .addAll(points);

                    polyline = mMap.addPolyline(polylineOptions);

                    double distanceKm = distanceInMeters / 1000.0;
                    shippingFee = Math.ceil(distanceKm) * 100;

                    Toast.makeText(
                            requireContext(),
                            "Distance: " + distanceText + "\nDuration: " + durationText + "\nFee: LKR " + (int) shippingFee,
                            Toast.LENGTH_LONG
                    ).show();

                } catch (Exception e) {
                    Log.e("DirectionsAPI", "Error parsing direction data", e);
                    fallbackRouteAndFee(start, end);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (!isAdded() || binding == null) return;
                Log.e("DirectionsAPI", "Directions API network error", t);
                fallbackRouteAndFee(start, end);
            }
        });
    }

    private void fallbackRouteAndFee(LatLng start, LatLng end) {
        if (!isAdded() || binding == null || getContext() == null) return;

        float[] results = new float[1];
        android.location.Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
        double distanceKm = results[0] / 1000.0;
        shippingFee = Math.max(100, Math.ceil(distanceKm) * 100);

        if (polyline != null) {
            polyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .width(15)
                .color(ContextCompat.getColor(requireContext(), R.color.colorCustomColor1))
                .add(start, end);

        if (mMap != null) {
            polyline = mMap.addPolyline(polylineOptions);
        }

        String distanceText = String.format(Locale.US, "%.1f km", distanceKm);
        Toast.makeText(
                requireContext(),
                "Location selected.\nDistance: " + distanceText + "\nDelivery Fee: LKR " + (int) shippingFee,
                Toast.LENGTH_LONG
        ).show();
    }

    private String getSigningCertificateSHA1() {
        try {
            Context context = getContext();
            if (context == null) return null;
            PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES
                );
                if (packageInfo.signingInfo != null) {
                    Signature[] signatures = packageInfo.signingInfo.getApkContentsSigners();
                    if (signatures != null && signatures.length > 0) {
                        return getSHA1FromSignature(signatures[0]);
                    }
                }
            } else {
                packageInfo = context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNATURES
                );
                if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                    return getSHA1FromSignature(packageInfo.signatures[0]);
                }
            }
        } catch (Exception e) {
            Log.e("AddressFragment", "Error getting SHA-1", e);
        }
        return null;
    }

    private String getSHA1FromSignature(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(signature.toByteArray());
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xFF & b).toUpperCase(Locale.US);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return null;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mAuth == null || currentUser == null) {
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }
}