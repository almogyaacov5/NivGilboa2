package com.example.nivgilboaapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import com.example.nivgilboaapp.R;

import java.util.List;

public class RestaurantSuggestionAdapter extends ArrayAdapter<Restaurant> {
    public RestaurantSuggestionAdapter(Context context, List<Restaurant> restaurants) {
        super(context, 0, restaurants);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Restaurant restaurant = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_suggestion, parent, false);
        }

        TextView nameView = convertView.findViewById(R.id.suggestion_name);
        TextView cuisineView = convertView.findViewById(R.id.suggestion_cuisine);
        RatingBar ratingBar = convertView.findViewById(R.id.suggestion_rating);
        ImageView kosherIcon = convertView.findViewById(R.id.suggestion_kosher);

        nameView.setText(restaurant.name);
        cuisineView.setText(restaurant.cuisine + " • " + restaurant.priceLevel);
        ratingBar.setRating(restaurant.rating);
        kosherIcon.setVisibility(restaurant.kosher ? View.VISIBLE : View.GONE);

        return convertView;
    }
}
