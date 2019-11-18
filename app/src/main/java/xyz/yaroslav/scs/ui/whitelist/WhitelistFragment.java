package xyz.yaroslav.scs.ui.whitelist;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.TagAdapter;
import xyz.yaroslav.scs.TagItem;
import xyz.yaroslav.scs.util.Utilities;

public class WhitelistFragment extends Fragment {
    private TagAdapter tagAdapter;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private RecyclerView.ItemDecoration decoration;

    private List<TagItem> whiteList;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_whitelist, container, false);

        whiteList = new ArrayList<>();

        progressBar = root.findViewById(R.id.whitelist_progressbar);
        recyclerView = root.findViewById(R.id.whitelist_tags);
        decoration = new DividerItemDecoration(root.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(decoration);

        parseTags();

        return root;
    }


    private void parseTags() {
        progressBar.setVisibility(View.VISIBLE);
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            String result = new Utilities().readFromFile(getContext(), getString(R.string.file_whitelist));
            if (result != null && !result.isEmpty()) {
                try {
                    whiteList = new WhitelistParseAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, result).get();
                    if (whiteList != null && !whiteList.isEmpty()) {
                        displayTags(whiteList);
                    } else {
                        Toast.makeText(getContext(), getString(R.string.toast_msg_empty_whitelist), Toast.LENGTH_SHORT).show();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("WHITELIST_PARSE", "Exception: " + e.getMessage());
                    progressBar.setVisibility(View.INVISIBLE);
                }
            } else {
                progressBar.setVisibility(View.INVISIBLE);
            }
        }, 100);
    }


    private void displayTags(List<TagItem> list) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Collections.sort(list, TagItem.WhitelistComparator);
            tagAdapter = new TagAdapter(list);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(tagAdapter);
            progressBar.setVisibility(View.INVISIBLE);
        }, 100);
    }
}