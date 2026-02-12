package com.example.nivgilboaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.nivgilboaapp.R;
import com.example.nivgilboaapp.YouTubeSearchActivity;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddRestaurantActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQ_PICK_YOUTUBE = 2001;
    private GoogleMap mMap;
    private double selectedLat = 32.0853;
    private double selectedLng = 34.7818;
    private PlacesClient placesClient;
    private AutocompleteSupportFragment autocompleteFragment;

    // Local (אם תרצה להשאיר)
    private SharedPreferences prefs;
    private List<Restaurant> restaurants;

    // Firebase
    private DatabaseReference restaurantsRef;

    // Views
    private TextInputEditText etName, etAddress, etCuisine, etRating, etPrice, etLat, etLng, etYouTubeUrl;
    private CheckBox cbKosher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_restaurant);

        // Firebase ref
        restaurantsRef = FirebaseDatabase.getInstance().getReference("restaurants");

        // Initialize Places API
        Places.initialize(getApplicationContext(), "AIzaSyAabQU2qs7PTdWsUcEhN5gDJHbj1qHAv68");
        placesClient = Places.createClient(this);

        // Local (אופציונלי)
        prefs = getSharedPreferences("restaurants", MODE_PRIVATE);
        loadRestaurants();

        initViews();
        setupAutocomplete();
        setupMap();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etAddress = findViewById(R.id.etAddress);
        etCuisine = findViewById(R.id.etCuisine);
        etYouTubeUrl = findViewById(R.id.etYouTubeUrl);
        etRating = findViewById(R.id.etRating);
        etPrice = findViewById(R.id.etPrice);
        etLat = findViewById(R.id.etLat);
        etLng = findViewById(R.id.etLng);
        cbKosher = findViewById(R.id.cbKosher);

        updateCoordsDisplay();

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveRestaurant());

        Button btnPickYouTube = findViewById(R.id.btnPickYouTube);
        btnPickYouTube.setOnClickListener(v -> {
            String q = (etName != null && etName.getText()!=null) ? etName.getText().toString().trim() : "";
            Intent i = new Intent(this, YouTubeSearchActivity.class);
            i.putExtra("q", q);
            startActivityForResult(i, REQ_PICK_YOUTUBE);
        });

    }

    private void setupAutocomplete() {
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.placeAutocompleteFragment);

        if (autocompleteFragment == null) return;

        autocompleteFragment.setPlaceFields(Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
        ));

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                if (latLng == null) return;

                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;
                updateCoordsDisplay();

                if (place.getAddress() != null && etAddress != null) {
                    etAddress.setText(place.getAddress());
                }

                if (mMap != null) {
                    mMap.clear();
                    mMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(place.getName() != null ? place.getName() : "מיקום"));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                }

                Toast.makeText(AddRestaurantActivity.this,
                        "✅ נבחר: " + (place.getName() != null ? place.getName() : "מיקום"),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(AddRestaurantActivity.this,
                        "❌ שגיאת חיפוש: " + status.getStatusMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_picker);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng telAviv = new LatLng(32.0853, 34.7818);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(telAviv, 10));

        mMap.setOnMapClickListener(latLng -> {
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;
            updateCoordsDisplay();

            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title("📍 מיקום נבחר ידנית"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

            Toast.makeText(this, "📍 נבחר מיקום ידני", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCoordsDisplay() {
        if (etLat != null) etLat.setText(String.format("%.6f", selectedLat));
        if (etLng != null) etLng.setText(String.format("%.6f", selectedLng));
    }

    private void saveRestaurant() {
        String name = (etName != null && etName.getText() != null)
                ? etName.getText().toString().trim()
                : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "❌ חובה להזין שם מסעדה!", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating;
        try {
            String ratingStr = (etRating != null && etRating.getText() != null)
                    ? etRating.getText().toString().trim()
                    : "";
            rating = ratingStr.isEmpty() ? 0f : Float.parseFloat(ratingStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "❌ ציון חייב להיות מספר (למשל: 9.5)", Toast.LENGTH_LONG).show();
            return;
        }

        // חדש: קישור יוטיוב (חייב להיות לפני יצירת האובייקט)
        String youtubeUrl = (etYouTubeUrl != null && etYouTubeUrl.getText() != null)
                ? etYouTubeUrl.getText().toString().trim()
                : "";

        Restaurant restaurant = new Restaurant(
                name,
                (etAddress != null && etAddress.getText() != null) ? etAddress.getText().toString() : "",
                (etCuisine != null && etCuisine.getText() != null) ? etCuisine.getText().toString() : "",
                rating,
                selectedLat,
                selectedLng,
                cbKosher != null && cbKosher.isChecked(),
                (etPrice != null && etPrice.getText() != null) ? etPrice.getText().toString() : "",
                "",
                youtubeUrl   // ← כאן הקישור
        );

        // --- שמירה ל-Firebase (לכל המשתמשים) ---
        String key = restaurantsRef.push().getKey();
        if (key == null) {
            Toast.makeText(this, "שגיאה: לא נוצר מזהה למסעדה", Toast.LENGTH_LONG).show();
            return;
        }
        restaurant.id = key;

        restaurantsRef.child(key)
                .setValue(restaurant)
                .addOnSuccessListener(unused -> {
                    // (אופציונלי) שמירה גם מקומית – אפשר למחוק אם לא צריך
                    restaurants.add(restaurant);
                    saveRestaurants();

                    Toast.makeText(this, "🎉 נשמר ב-Firebase לכל המשתמשים!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאת Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }


    // --- Local (אופציונלי) ---
    private void loadRestaurants() {
        Gson gson = new Gson();
        String json = prefs.getString("restaurants_list", null);
        Type type = new TypeToken<ArrayList<Restaurant>>() {}.getType();
        restaurants = (json == null) ? new ArrayList<>() : gson.fromJson(json, type);
        if (restaurants == null) restaurants = new ArrayList<>();
    }

    private void saveRestaurants() {
        Gson gson = new Gson();
        String json = gson.toJson(restaurants);
        prefs.edit().putString("restaurants_list", json).apply();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_PICK_YOUTUBE && resultCode == RESULT_OK && data != null) {
            String url = data.getStringExtra("videoUrl");
            if (etYouTubeUrl != null && url != null) etYouTubeUrl.setText(url);
        }
    }

}
