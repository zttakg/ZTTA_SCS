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

    private static final String TOP_KEY = "events";
    public static final String BRANCH = "branch_id_key";

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
        historyRecyclerView = root.findViewById(R.id.history_tags);

        tagFromFileList = new ArrayList<>();
        tagItemList = new ArrayList<>();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        //showTagsFromLocalFile();
        new ShowTagsAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        int branch = preferences.getInt(BRANCH, -1);
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
                    JSONArray jsonArray = new JSONArray();
                    try {
                        JSONObject jsonObject0 = new JSONObject();
                        JSONObject jsonObject1 = new JSONObject();
                        JSONObject jsonObject2 = new JSONObject();
                        JSONObject jsonObject3 = new JSONObject();
                        JSONObject jsonObject4 = new JSONObject();
                        JSONObject jsonObject5 = new JSONObject();
                        JSONObject jsonObject6 = new JSONObject();
                        JSONObject jsonObject7 = new JSONObject();
                        JSONObject jsonObject8 = new JSONObject();
                        JSONObject jsonObject9 = new JSONObject();
                        JSONObject jsonObject10 = new JSONObject();
                        JSONObject jsonObject11 = new JSONObject();
                        JSONObject jsonObject12 = new JSONObject();
                        JSONObject jsonObject13 = new JSONObject();
                        JSONObject jsonObject14 = new JSONObject();
                        JSONObject jsonObject15 = new JSONObject();
                        JSONObject jsonObject16 = new JSONObject();
                        JSONObject jsonObject17 = new JSONObject();
                        JSONObject jsonObject18 = new JSONObject();
                        JSONObject jsonObject19 = new JSONObject();

                        jsonObject0.put("tag_time", "1572548313677");
                        jsonObject0.put("tag_id", "04E59CA2E74C80");
                        jsonObject0.put("tag_data", "М2 Юг");
                        jsonObject1.put("tag_time", "1572548409580");
                        jsonObject1.put("tag_id", "04AF9AA2E74C80");
                        jsonObject1.put("tag_data", "ДСМ Запад");
                        jsonObject2.put("tag_time", "1572548505471");
                        jsonObject2.put("tag_id", "04069CA2E74C81");
                        jsonObject2.put("tag_data", "Косой склад");
                        jsonObject3.put("tag_time", "1572548616018");
                        jsonObject3.put("tag_id", "04D59CA2E74C80");
                        jsonObject3.put("tag_data", "Цех 3");
                        jsonObject4.put("tag_time", "1572548850213");
                        jsonObject4.put("tag_id", "04EE9CA2E74C80");
                        jsonObject4.put("tag_data", "Столовая Север");
                        jsonObject5.put("tag_time", "1572548889922");
                        jsonObject5.put("tag_id", "04B69BA2E74C80");
                        jsonObject5.put("tag_data", "Домик");
                        jsonObject6.put("tag_time", "1572549052036");
                        jsonObject6.put("tag_id", "04BE9BA2E74C80");
                        jsonObject6.put("tag_data", "Раскрой");
                        jsonObject7.put("tag_time", "1572555568267");
                        jsonObject7.put("tag_id", "04A899A2E74C80");
                        jsonObject7.put("tag_data", "ДСМ Восток");
                        jsonObject8.put("tag_time", "1572555705054");
                        jsonObject8.put("tag_id", "04E59CA2E74C80");
                        jsonObject8.put("tag_data", "М2 Юг");
                        jsonObject9.put("tag_time", "1572555795573");
                        jsonObject9.put("tag_id", "04AF9AA2E74C80");
                        jsonObject9.put("tag_data", "ДСМ Запад");
                        jsonObject10.put("tag_time", "1572555886536");
                        jsonObject10.put("tag_id", "04069CA2E74C81");
                        jsonObject10.put("tag_data", "Косой склад");
                        jsonObject11.put("tag_time", "1572556065128");
                        jsonObject11.put("tag_id", "04D59CA2E74C80");
                        jsonObject11.put("tag_data", "Цех 3");
                        jsonObject12.put("tag_time", "1572556297498");
                        jsonObject12.put("tag_id", "04EE9CA2E74C80");
                        jsonObject12.put("tag_data", "Столовая Север");
                        jsonObject13.put("tag_time", "1572556340590");
                        jsonObject13.put("tag_id", "04B69BA2E74C80");
                        jsonObject13.put("tag_data", "Домик");
                        jsonObject14.put("tag_time", "1572556522047");
                        jsonObject14.put("tag_id", "04BE9BA2E74C80");
                        jsonObject14.put("tag_data", "Раскрой");
                        jsonObject15.put("tag_time", "1572567894565");
                        jsonObject15.put("tag_id", "04A899A2E74C80");
                        jsonObject15.put("tag_data", "ДСМ Восток");
                        jsonObject16.put("tag_time", "1572568105328");
                        jsonObject16.put("tag_id", "04AF9AA2E74C80");
                        jsonObject16.put("tag_data", "ДСМ Запад");
                        jsonObject17.put("tag_time", "1572568170617");
                        jsonObject17.put("tag_id", "04069CA2E74C81");
                        jsonObject17.put("tag_data", "Косой склад");
                        jsonObject18.put("tag_time", "1572568240858");
                        jsonObject18.put("tag_id", "04D59CA2E74C80");
                        jsonObject18.put("tag_data", "Цех 3");
                        jsonObject19.put("tag_time", "1572568668173");
                        jsonObject19.put("tag_id", "04B69BA2E74C80");
                        jsonObject19.put("tag_data", "Домик");


                        jsonArray.put(jsonObject0);
                        jsonArray.put(jsonObject1);
                        jsonArray.put(jsonObject2);
                        jsonArray.put(jsonObject3);
                        jsonArray.put(jsonObject4);
                        jsonArray.put(jsonObject5);
                        jsonArray.put(jsonObject6);
                        jsonArray.put(jsonObject7);
                        jsonArray.put(jsonObject8);
                        jsonArray.put(jsonObject9);
                        jsonArray.put(jsonObject10);
                        jsonArray.put(jsonObject11);
                        jsonArray.put(jsonObject12);
                        jsonArray.put(jsonObject13);
                        jsonArray.put(jsonObject14);
                        jsonArray.put(jsonObject15);
                        jsonArray.put(jsonObject16);
                        jsonArray.put(jsonObject17);
                        jsonArray.put(jsonObject18);
                        jsonArray.put(jsonObject19);

                    } catch (JSONException e) {
                        Log.e("BUILD_JSON", "JSON Exception: " + e.getMessage());
                    }

                    try {
                        JSONObject bigJson = new JSONObject();
                        bigJson.put(TOP_KEY, jsonArray);
                        try {
                            tagFromFileList = new HistoryParseAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bigJson.toString()).get();
                            if (tagFromFileList != null && !tagFromFileList.isEmpty()) {
                                displayTags(tagFromFileList);
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(getContext(), "Empty result", Toast.LENGTH_SHORT).show();
                            }
                        } catch (ExecutionException | InterruptedException e) {
                            Log.e("DISPLAY_TAGS", "Exception: " + e.getMessage());
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    } catch (JSONException e) {
                        Log.e("BUILD_JSON", "JSON Exception: " + e.getMessage());
                        progressBar.setVisibility(View.INVISIBLE);
                    }
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
            historyRecyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
            historyRecyclerView.setAdapter(tagAdapter);
            progressBar.setVisibility(View.INVISIBLE);
        }, 200);
    }
}