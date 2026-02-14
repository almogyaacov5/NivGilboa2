package com.example.nivgilboaapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.example.nivgilboaapp.R;

public class RestaurantDetailActivity extends AppCompatActivity {

    private Restaurant restaurant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant_detail);

        Intent intent = getIntent();
        restaurant = (Restaurant) intent.getSerializableExtra("restaurant");

        if (restaurant == null) {
            Toast.makeText(this, "שגיאה: לא התקבלו פרטי מסעדה", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        populateRestaurantDetails();
        setupActionButtons();
    }

    private void populateRestaurantDetails() {
        TextView tvName = findViewById(R.id.tvRestaurantName);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvCuisine = findViewById(R.id.tvCuisine);
        TextView tvRating = findViewById(R.id.tvRating);
        TextView tvReview = findViewById(R.id.tvReviewSummary);
        RatingBar ratingBar = findViewById(R.id.ratingBar);
        ImageView ivKosher = findViewById(R.id.ivKosher);

        tvName.setText(restaurant.name != null ? restaurant.name : "");
        tvAddress.setText(restaurant.address != null ? restaurant.address : "");
        tvCuisine.setText(restaurant.cuisine != null ? restaurant.cuisine : "");
        tvRating.setText(String.valueOf(restaurant.rating));
        tvReview.setText(restaurant.reviewSummary != null ? restaurant.reviewSummary : "");
        ratingBar.setRating(restaurant.rating);
        ivKosher.setVisibility(restaurant.kosher ? View.VISIBLE : View.GONE);
    }

    private void setupActionButtons() {
        Button btnWatchVideo = findViewById(R.id.btnWatchVideo);
        Button btnNavigate = findViewById(R.id.btnNavigate);
        Button btnFavorite = findViewById(R.id.btnFavorite);
        Button btnDelete = findViewById(R.id.btnDelete);

        // NEW:
        Button btnEdit = findViewById(R.id.btnEdit);

        btnWatchVideo.setOnClickListener(v -> openVideoUrl());

        btnNavigate.setOnClickListener(v -> {
            String uri = String.format("google.navigation:q=%f,%f", restaurant.lat, restaurant.lng);
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            } catch (Exception e) {
                Toast.makeText(this, "לא ניתן לפתוח ניווט במכשיר הזה", Toast.LENGTH_LONG).show();
            }
        });

        btnFavorite.setOnClickListener(v ->
                Toast.makeText(this, "נשמר כמועדף!", Toast.LENGTH_SHORT).show()
        );

        // NEW: Edit
        if (btnEdit != null) {
            if (restaurant.id == null || restaurant.id.trim().isEmpty()) {
                btnEdit.setEnabled(false);
                btnEdit.setAlpha(0.5f);
                btnEdit.setOnClickListener(v ->
                        Toast.makeText(this, "לא ניתן לערוך מסעדה בלי מזהה Firebase", Toast.LENGTH_LONG).show()
                );
            } else {
                btnEdit.setOnClickListener(v -> {
                    Intent i = new Intent(this, AddRestaurantActivity.class);
                    i.putExtra(AddRestaurantActivity.EXTRA_RESTAURANT, restaurant);
                    startActivity(i);
                });
            }
        }

        // Delete
        if (restaurant.id == null || restaurant.id.trim().isEmpty()) {
            btnDelete.setEnabled(false);
            btnDelete.setAlpha(0.5f);
            btnDelete.setOnClickListener(v ->
                    Toast.makeText(this, "לא ניתן למחוק מסעדה שאין לה מזהה ב-Firebase", Toast.LENGTH_LONG).show()
            );
        } else {
            btnDelete.setOnClickListener(v -> confirmDelete());
        }
    }


    private void openVideoUrl() {
        String url = restaurant.videoUrl != null ? restaurant.videoUrl.trim() : "";
        if (url.isEmpty()) {
            Toast.makeText(this, "אין סרטון זמין", Toast.LENGTH_SHORT).show();
            return;
        }

        // אם המשתמש הדביק בלי http/https – Intent עלול להיכשל, אז נוסיף https [web:119]
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "לא ניתן לפתוח את הקישור במכשיר הזה", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת מסעדה")
                .setMessage("למחוק את המסעדה לכל המשתמשים?\nלא ניתן לשחזר.")
                .setPositiveButton("מחק", (d, which) -> deleteFromFirebase())
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void deleteFromFirebase() {
        String id = restaurant.id.trim();

        // מחיקה ב-Realtime Database נעשית ע"י removeValue() על ה-node [web:56]
        FirebaseDatabase.getInstance()
                .getReference("restaurants")
                .child(id)
                .removeValue()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "המסעדה נמחקה לכל המשתמשים", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה במחיקה: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
