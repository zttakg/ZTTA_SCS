package xyz.yaroslav.scs.ui.unsent;

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
import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.TagAdapter;
import xyz.yaroslav.scs.TagItem;
import xyz.yaroslav.scs.ui.history.HistoryParseAsync;
import xyz.yaroslav.scs.util.Utilities;

public class UnsentFragment extends Fragment {
    private TagAdapter tagAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private List<TagItem> unsentTags;
    private static final String TOP_KEY = "events";

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_unsent, container, false);

        unsentTags = new ArrayList<>();
        progressBar = root.findViewById(R.id.unsent_progressbar);
        recyclerView = root.findViewById(R.id.unsent_tags);

        new ShowTempAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return root;
    }

    private class ShowTempAsync extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            String tmp = new Utilities().readFromFile(getContext(), getString(R.string.file_cache));
            if (!tmp.equals("")) {
                String[] arr = tmp.split(";");
                if (arr.length > 0) {
                    unsentTags.clear();
                    for (String value : arr) {
                        try {
                            String tagName;
                            String tagTime;
                            String tagUid;

                            JSONObject jsonObject = new JSONObject(value);
                            tagName = jsonObject.getString("tag_data");
                            tagTime = jsonObject.getString("tag_time");
                            tagUid = jsonObject.getString("tag_id");
                            TagItem tagItem = new TagItem(tagUid, tagName, tagTime);

                            unsentTags.add(tagItem);
                        } catch (JSONException e) {
                            Log.e("TEMP_FILE", "JSON Exception in " + "(" + e.getClass() + "): " + e.getMessage());
                        }
                    }
                    if (!unsentTags.isEmpty()) {
                        return 0;
                    }
                }
            }
            return 1;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Integer flag) {
            if (flag == 0) {
                displayTags(unsentTags);
            } else {
                Toast.makeText(getContext(), getString(R.string.toast_msg_empty_temp), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void displayTags(List<TagItem> list) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Collections.sort(list, TagItem.TagComparator);
            tagAdapter = new TagAdapter(list);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            recyclerView.setAdapter(tagAdapter);
            progressBar.setVisibility(View.INVISIBLE);
        }, 200);
    }
}