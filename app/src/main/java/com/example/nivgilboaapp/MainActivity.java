package com.example.nivgilboaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int ADD_REQUEST_CODE = 2;

    private DatabaseReference restaurantsRef;
    private ValueEventListener restaurantsListener;

    private final List<Restaurant> restaurants = new ArrayList<>();

    // חשוב: נשמור markers לפי id של Firebase כדי למחוק/לעדכן בקלות
    private final Map<String, Marker> markerById = new HashMap<>();

    // --- Drag FAB state ---
    private boolean dragMode = false;
    private float dX, dY;

    // --- Persist FAB position ---
    private static final String PREF_UI = "ui_prefs";
    private static final String KEY_FAB_SET = "fab_set";
    private static final String KEY_FAB_X = "fab_x";
    private static final String KEY_FAB_Y = "fab_y";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        restaurantsRef = FirebaseDatabase.getInstance().getReference("restaurants");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        View root = findViewById(R.id.root);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        if (fabAdd != null) {

            final int safeMarginPx = dpToPx(16); // שנה כאן מרווח מהקצוות אם בא לך

            // קליק רגיל = הוספה
            fabAdd.setOnClickListener(v -> {
                if (dragMode) return;
                Intent i = new Intent(MainActivity.this, AddRestaurantActivity.class);
                startActivityForResult(i, ADD_REQUEST_CODE);
            });

            // לחיצה ארוכה = מצב גרירה
            fabAdd.setOnLongClickListener(v -> {
                dragMode = true;
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            });

            // גרירה בפועל
            fabAdd.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {

                    case MotionEvent.ACTION_DOWN:
                        // נשמור offset תמיד, כדי שאם תהיה לחיצה ארוכה אחר כך נוכל לגרור חלק
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        return false; // מאפשר ל-FAB להמשיך לטפל בקליק/לונג-פרס רגילים

                    case MotionEvent.ACTION_MOVE:
                        if (!dragMode) return false;

                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        if (root != null) {
                            float minX = safeMarginPx;
                            float minY = safeMarginPx;
                            float maxX = root.getWidth() - v.getWidth() - safeMarginPx;
                            float maxY = root.getHeight() - v.getHeight() - safeMarginPx;

                            newX = Math.max(minX, Math.min(newX, maxX));
                            newY = Math.max(minY, Math.min(newY, maxY));
                        }

                        v.setX(newX);
                        v.setY(newY);
                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (!dragMode) return false;

                        dragMode = false;
                        saveFabPosition(v.getX(), v.getY());
                        return true;
                }
                return false;
            });

            // שחזור מיקום אחרי שהמסך נמדד
            if (root != null) {
                root.post(() -> restoreFabPosition(fabAdd, root, safeMarginPx));
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            moveToCurrentLocationOrDefault();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }

        // marker click -> details
        mMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Restaurant) {
                openRestaurantDetails((Restaurant) tag);
                return true;
            }
            return false;
        });

        // להתחיל להאזין ל-Firebase רק אחרי שהמפה מוכנה
        attachFirebaseListener();
    }

    private void attachFirebaseListener() {
        if (restaurantsListener != null) return;

        restaurantsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                restaurants.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Restaurant r = child.getValue(Restaurant.class);
                    if (r == null) continue;

                    r.id = child.getKey(); // קריטי למחיקה
                    restaurants.add(r);
                }

                redrawMarkers(); // ← זה מה שמסיר סמנים שנמחקו
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this,
                        "Firebase error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        restaurantsRef.addValueEventListener(restaurantsListener);
    }

    private void redrawMarkers() {
        if (mMap == null) return;

        // 1) הסר את כל הסמנים הישנים (כדי שלא יישאר סמן של מסעדה שנמחקה)
        for (Marker m : markerById.values()) {
            if (m != null) m.remove();
        }
        markerById.clear();

        // 2) צייר מחדש רק לפי הרשימה הנוכחית מ-Firebase
        for (Restaurant r : restaurants) {
            if (r == null) continue;

            LatLng pos = new LatLng(r.lat, r.lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(r.name)
                    .snippet("⭐ " + r.rating + " | " + r.cuisine));

            if (marker != null) {
                marker.setTag(r);
                if (r.id != null) markerById.put(r.id, marker);
            }
        }
    }

    private void openRestaurantDetails(Restaurant restaurant) {
        Intent intent = new Intent(this, RestaurantDetailActivity.class);
        intent.putExtra("restaurant", restaurant);
        startActivity(intent);
    }

    private void moveToCurrentLocationOrDefault() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, loc -> {
            if (mMap == null) return;
            if (loc != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(loc.getLatitude(), loc.getLongitude()), 12));
            } else {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(32.0853, 34.7818), 10)); // תל אביב
            }
        });
    }

    private void saveFabPosition(float x, float y) {
        SharedPreferences sp = getSharedPreferences(PREF_UI, MODE_PRIVATE);
        sp.edit()
                .putBoolean(KEY_FAB_SET, true)
                .putFloat(KEY_FAB_X, x)
                .putFloat(KEY_FAB_Y, y)
                .apply();
    }

    private void restoreFabPosition(FloatingActionButton fab, View root, int safeMarginPx) {
        SharedPreferences sp = getSharedPreferences(PREF_UI, MODE_PRIVATE);
        if (!sp.getBoolean(KEY_FAB_SET, false)) return;

        float x = sp.getFloat(KEY_FAB_X, fab.getX());
        float y = sp.getFloat(KEY_FAB_Y, fab.getY());

        // clamp כדי לא לצאת מהמסך (במיוחד אחרי שינוי גודל/רוטציה)
        float minX = safeMarginPx;
        float minY = safeMarginPx;
        float maxX = root.getWidth() - fab.getWidth() - safeMarginPx;
        float maxY = root.getHeight() - fab.getHeight() - safeMarginPx;

        x = Math.max(minX, Math.min(x, maxX));
        y = Math.max(minY, Math.min(y, maxY));

        fab.setX(x);
        fab.setY(y);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (restaurantsRef != null && restaurantsListener != null) {
            restaurantsRef.removeEventListener(restaurantsListener);
        }
    }
}
