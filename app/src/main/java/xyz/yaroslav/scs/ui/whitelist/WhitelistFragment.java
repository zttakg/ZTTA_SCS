package xyz.yaroslav.scs.ui.whitelist;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.TagAdapter;
import xyz.yaroslav.scs.TagItem;
import xyz.yaroslav.scs.util.Utilities;

public class WhitelistFragment extends Fragment {
    private TagAdapter tagAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private List<TagItem> whiteList;
    private static final String WHITELIST = "whitelist.txt";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_whitelist, container, false);

        whiteList = new ArrayList<>();

        progressBar = root.findViewById(R.id.whitelist_progressbar);
        recyclerView = root.findViewById(R.id.whitelist_tags);

        parseTags();

        return root;
    }


    private void parseTags() {
        progressBar.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.post(() -> {
            try {
                String jsonString = new Utilities().readFromFile(getContext(), WHITELIST);
                JSONArray jsonArray = new JSONArray(jsonString);
                //JSONArray jArray = jsonObject.getJSONArray("tags");

                whiteList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    String tagName;
                    String tagUid ;
                    try {
                        JSONObject nestedObject = jsonArray.getJSONObject(i);
                        tagName = nestedObject.getString("tag_data");
                        tagUid = nestedObject.getString("tag_id");

                        TagItem tagItem = new TagItem(tagUid, tagName);
                        whiteList.add(tagItem);
                    } catch (JSONException e) {
                        Log.e("WHITE_LIST", "JSON Exception: " + e.getMessage());
                    }
                }

                if (whiteList.size() > 0) {
                    displayTags(whiteList);
                } else {
                    Toast.makeText(getContext(), "Unable to parse file with acceptable tags", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.e("WHITE_LIST", "JSON Parse Exception: " + e.getMessage());
            }
        });
    }


    private void displayTags(List<TagItem> list) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Collections.sort(list, TagItem.WhitelistComparator);
            tagAdapter = new TagAdapter(list);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            recyclerView.setAdapter(tagAdapter);
            progressBar.setVisibility(View.INVISIBLE);
        }, 300);
    }
}