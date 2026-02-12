package com.example.nivgilboaapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.example.nivgilboaapp.BuildConfig;
import com.example.nivgilboaapp.R;
import com.example.nivgilboaapp.YouTubeVideoAdapter;
import com.example.nivgilboaapp.YouTubeVideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class YouTubeSearchActivity extends AppCompatActivity {

    private static final String YT_API_KEY = BuildConfig.YOUTUBE_API_KEY;

    private EditText etQuery;
    private MaterialButton btnSearch;
    private ProgressBar progress;
    private RecyclerView recycler;

    private final ArrayList<YouTubeVideoItem> items = new ArrayList<>();
    private YouTubeVideoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_search);

        etQuery = findViewById(R.id.etQuery);
        btnSearch = findViewById(R.id.btnSearch);
        progress = findViewById(R.id.progress);
        recycler = findViewById(R.id.recycler);

        adapter = new YouTubeVideoAdapter(items, item -> {
            Intent data = new Intent();
            data.putExtra("videoUrl", "https://www.youtube.com/watch?v=" + item.videoId);
            setResult(RESULT_OK, data);
            finish();
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        String q = getIntent().getStringExtra("q");
        if (q != null) etQuery.setText(q);

        btnSearch.setOnClickListener(v -> doSearch());
    }

    private void doSearch() {
        final String q = etQuery.getText().toString().trim();
        if (q.isEmpty()) {
            Toast.makeText(this, "הכנס מילת חיפוש", Toast.LENGTH_SHORT).show();
            return;
        }

        if (YT_API_KEY == null || YT_API_KEY.trim().isEmpty()) {
            Toast.makeText(this, "חסר YOUTUBE_API_KEY ב-BuildConfig", Toast.LENGTH_LONG).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);

        new Thread(() -> {
            try {
                String urlStr =
                        "https://www.googleapis.com/youtube/v3/search" +
                                "?part=snippet" +
                                "&type=video" +
                                "&maxResults=15" +
                                "&q=" + URLEncoder.encode(q, "UTF-8") +
                                "&key=" + YT_API_KEY;

                // search.list של YouTube Data API [web:155]
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int code = conn.getResponseCode();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()
                ));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                if (code < 200 || code >= 300) {
                    throw new RuntimeException("HTTP " + code + ": " + sb);
                }

                JSONObject root = new JSONObject(sb.toString());
                JSONArray arr = root.getJSONArray("items");

                ArrayList<YouTubeVideoItem> newItems = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);

                    JSONObject idObj = item.optJSONObject("id");
                    if (idObj == null) continue;

                    String videoId = idObj.optString("videoId", "");
                    if (videoId.isEmpty()) continue;

                    JSONObject snippet = item.optJSONObject("snippet");
                    String title = snippet != null ? snippet.optString("title", "") : "";
                    String channel = snippet != null ? snippet.optString("channelTitle", "") : "";

                    newItems.add(new com.example.nivgilboaapp.YouTubeVideoItem(videoId, title, channel));
                }

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnSearch.setEnabled(true);

                    items.clear();
                    items.addAll(newItems);
                    adapter.notifyDataSetChanged();

                    if (items.isEmpty()) {
                        Toast.makeText(this, "לא נמצאו תוצאות", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnSearch.setEnabled(true);
                    Toast.makeText(this, "שגיאה בחיפוש: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
            }
        }).start();
    }
}
