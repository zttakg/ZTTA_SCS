package xyz.yaroslav.scs.ui.history;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.TagAdapter;
import xyz.yaroslav.scs.TagItem;
import xyz.yaroslav.scs.util.DownloadFromServerAsync;
import xyz.yaroslav.scs.util.Utilities;

public class HistoryFragment extends Fragment {
    SharedPreferences preferences;

    private List<TagItem> tagFromFileList;
    private List<TagItem> tagItemList;

    private TagAdapter tagAdapter;
    private RecyclerView historyRecyclerView;
    private RecyclerView.ItemDecoration decoration;

    private ProgressBar progressBar;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);

        progressBar = root.findViewById(R.id.history_progressbar);
        decoration = new DividerItemDecoration(root.getContext(), DividerItemDecoration.VERTICAL);
        historyRecyclerView = root.findViewById(R.id.history_tags);
        historyRecyclerView.addItemDecoration(decoration);

        tagFromFileList = new ArrayList<>();
        tagItemList = new ArrayList<>();

        new ShowTagsAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return root;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        Objects.requireNonNull(getActivity()).getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setOnMenuItemClickListener(item -> {
            RangeFragment rangeFragment = new RangeFragment();
            rangeFragment.setTargetFragment(this, 100);
            rangeFragment.show(getFragmentManager(), "FRAG");
            progressBar.setVisibility(View.VISIBLE);
            return true;
        });
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100) {
            if (resultCode == Activity.RESULT_OK) {
                long begin = 0;
                long end = 0;
                if (data != null) {
                    if (data.getExtras().containsKey("start")) {
                        begin = data.getExtras().getLong("start");
                    }
                    if (data.getExtras().containsKey("end")) {
                        end = data.getExtras().getLong("end");
                    }
                }
                getTags(begin, end);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(getContext(), "Cancelled by user", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getTags(long _start, long _finish) {
        String url = buildUrl(_start, _finish);
        showTagsFromServer(url);
    }

    private String buildUrl(long start, long end) {
        int branch = preferences.getInt(getString(R.string.pref_key_branch_id), -1);
        String protocol = preferences.getString(getString(R.string.pref_key_net_protocol), getString(R.string.pref_value_net_protocol));
        String address = preferences.getString(getString(R.string.pref_key_net_address), getString(R.string.pref_value_net_address));
        String port = preferences.getString(getString(R.string.pref_key_net_port), getString(R.string.pref_value_net_port));
        String prefix = preferences.getString(getString(R.string.pref_key_net_history), getString(R.string.pref_value_net_history));
        String[] prefix_arr = prefix.split("&");

        if (start != 0 && end != 0) {
            return protocol + "://" + address + ":" + port + "/" + prefix_arr[0] + start + "&" + prefix_arr[1] + end + "&" + prefix_arr[2] + branch;
        } else {
            return protocol + "://" + address + ":" + port + "/" + prefix + branch;
        }
    }

    private void showTagsFromServer(String _url) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                String tags = new DownloadFromServerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, _url).get();
                if (tags != null && !tags.isEmpty()) {
                    tagItemList = new HistoryParseAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tags).get();
                    if (tagItemList != null && !tagItemList.isEmpty()) {
                        displayTags(tagItemList);
                    } else {
                        Toast.makeText(getContext(), "Empty result", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Empty result", Toast.LENGTH_SHORT).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("DISPLAY_TAGS", "Exception: " + e.getMessage());
            }
            progressBar.setVisibility(View.INVISIBLE);
        }, 200);
    }

    private class ShowTagsAsync extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            String tmp = new Utilities().readFromFile(getContext(), getString(R.string.file_history));
            if (!tmp.equals("")) {
                String[] arr = tmp.split(";");
                if (arr.length > 0) {
                    tagFromFileList.clear();
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

                            tagFromFileList.add(tagItem);
                        } catch (JSONException e) {
                            Log.e("HISTORY_FILE", "JSON Exception in " + "(" + e.getClass() + "): " + e.getMessage());
                        }
                    }
                    if (!tagFromFileList.isEmpty()) {
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
                displayTags(tagFromFileList);
            } else {
                showTagsFromServer(buildUrl(0,0));
            }
        }
    }

    private void displayTags(List<TagItem> list) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Collections.sort(list, TagItem.TagComparator);
            tagAdapter = new TagAdapter(list);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
            historyRecyclerView.setLayoutManager(layoutManager);
            historyRecyclerView.setItemAnimator(new DefaultItemAnimator());
            historyRecyclerView.setAdapter(tagAdapter);
            progressBar.setVisibility(View.INVISIBLE);
        }, 200);
    }

}






























