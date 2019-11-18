package xyz.yaroslav.scs.ui.history;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import java.io.OutputStreamWriter;
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
    private LinearLayout imageHolder;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        updateHistory();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_history, container, false);

        progressBar = root.findViewById(R.id.history_progressbar);
        imageHolder = root.findViewById(R.id.imageForErrors);

        decoration = new DividerItemDecoration(root.getContext(), DividerItemDecoration.VERTICAL);
        historyRecyclerView = root.findViewById(R.id.history_tags);
        historyRecyclerView.addItemDecoration(decoration);

        tagFromFileList = new ArrayList<>();
        tagItemList = new ArrayList<>();

        getLocalTags();

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
                String url = new Utilities().buildUrl(getContext(), 2, begin, end);
                getTagsFromServer(url);
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(getContext(), getString(R.string.toast_cancelled_by_user), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateHistory() {
        String data = new Utilities().readFromFile(getContext(), getString(R.string.file_history));
        if (data != null && !data.isEmpty()) {
            try {
                List<String> items = new UpdateLocalHistoryAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data).get();
                if (items != null) {
                    rewriteHistoryFile(items);
                } else {
                    new Utilities().clearFile(getContext(), getString(R.string.file_history));
                }
            } catch (InterruptedException | ExecutionException e) {
                Log.e("UPDATE_LOCAL_HISTORY", "Exception: " + e.getMessage());
            }
        }
    }

    private void getLocalTags() {
        progressBar.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            String raw = new Utilities().readFromFile(getContext(), getString(R.string.file_history));
            if (raw != null && !raw.isEmpty()) {
                try {
                    tagFromFileList = new HistoryFileParseAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, raw).get();
                    if (tagFromFileList != null) {
                        displayTags(tagFromFileList);
                    } else {
                        getTagsFromServer(new Utilities().buildUrl(getContext(), 2));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("SHOW_TAGS", "Exception: " + e.getMessage());
                    progressBar.setVisibility(View.INVISIBLE);
                    showError();
                    Toast.makeText(getContext(), getString(R.string.toast_empty_result), Toast.LENGTH_SHORT).show();
                }
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                showError();
                Toast.makeText(getContext(), getString(R.string.toast_empty_result), Toast.LENGTH_SHORT).show();
            }
        }, 100);
    }

    private void getTagsFromServer(String _url) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                String tags = new DownloadFromServerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, _url).get();
                if (tags != null && !tags.isEmpty()) {
                    tagItemList = new HistoryParseAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, tags).get();
                    if (tagItemList != null && !tagItemList.isEmpty()) {
                        displayTags(tagItemList);
                    } else {
                        showError();
                        Toast.makeText(getContext(), getString(R.string.toast_empty_result), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showError();
                    Toast.makeText(getContext(), getString(R.string.toast_empty_result), Toast.LENGTH_SHORT).show();
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e("DISPLAY_TAGS", "Exception: " + e.getMessage());
                showError();
            }
            progressBar.setVisibility(View.INVISIBLE);
        }, 200);
    }

    private void displayTags(List<TagItem> list) {
        hideError();
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

    private void showError() {
        if (historyRecyclerView.getVisibility() == View.VISIBLE) {
            historyRecyclerView.setVisibility(View.INVISIBLE);
        }
        imageHolder.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        if (historyRecyclerView.getVisibility() == View.INVISIBLE) {
            historyRecyclerView.setVisibility(View.VISIBLE);
        }
        if (imageHolder.getVisibility() == View.VISIBLE) {
            imageHolder.setVisibility(View.GONE);
        }
    }

    private void rewriteHistoryFile(List<String> values)  {
        try {
            OutputStreamWriter streamWriter = new OutputStreamWriter(getContext().openFileOutput(getString(R.string.file_history), Context.MODE_PRIVATE));
            streamWriter.write("");
            streamWriter.close();
            for (String value : values) {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getContext().openFileOutput(getString(R.string.file_history), Context.MODE_APPEND));
                outputStreamWriter.write(value);
                outputStreamWriter.close();
            }
        }
        catch (IOException e) {
            Log.e("Exception", "History file write failed: " + e.toString());
        }
    }

}






























