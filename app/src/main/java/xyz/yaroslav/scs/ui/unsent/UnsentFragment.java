package xyz.yaroslav.scs.ui.unsent;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

public class UnsentFragment extends Fragment {
    private TagAdapter tagAdapter;
    private RecyclerView recyclerView;
    private LinearLayout statusImageHolder;
    private ProgressBar progressBar;
    private RecyclerView.ItemDecoration decoration;

    private List<TagItem> unsentTags;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_unsent, container, false);

        unsentTags = new ArrayList<>();
        progressBar = root.findViewById(R.id.unsent_progressbar);
        statusImageHolder = root.findViewById(R.id.statusImageHolder);
        decoration = new DividerItemDecoration(root.getContext(), DividerItemDecoration.VERTICAL);
        recyclerView = root.findViewById(R.id.unsent_tags);
        recyclerView.addItemDecoration(decoration);

        getUnsentTags();

        return root;
    }

    private void getUnsentTags() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            String raw = new Utilities().readFromFile(getContext(), getString(R.string.file_cache));
            if (raw != null && !raw.isEmpty()) {
                try {
                    unsentTags = new UnsentParseAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, raw).get();
                    if (unsentTags != null) {
                        hideInfoImage();
                        displayTags(unsentTags);
                    } else {
                        Toast.makeText(getContext(), getString(R.string.toast_msg_empty_temp), Toast.LENGTH_SHORT).show();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Log.e("GET_UNSENT_TAGS", "Exception: " + e.getMessage());
                }
            } else {
                showInfoImage();
            }
        }, 100);
    }

    private void displayTags(List<TagItem> list) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            Collections.sort(list, TagItem.TagComparator);
            tagAdapter = new TagAdapter(list);
            RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
            recyclerView.setAdapter(tagAdapter);
            progressBar.setVisibility(View.INVISIBLE);
        }, 200);
    }

    private void showInfoImage() {
        if (recyclerView.getVisibility() == View.VISIBLE) {
            recyclerView.setVisibility(View.INVISIBLE);
        }
        statusImageHolder.setVisibility(View.VISIBLE);
    }

    private void hideInfoImage() {
        if (recyclerView.getVisibility() == View.INVISIBLE) {
            recyclerView.setVisibility(View.VISIBLE);
        }
        if (statusImageHolder.getVisibility() == View.VISIBLE) {
            statusImageHolder.setVisibility(View.GONE);
        }
    }
}