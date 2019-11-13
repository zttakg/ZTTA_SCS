package xyz.yaroslav.scs.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.util.DownloadFromServerAsync;
import xyz.yaroslav.scs.util.Utilities;

public class LoadPreferences {
    private Context context;

    private static final String BRANCH_KEY = "branch";
    private static final String NETWORK_KEY = "network";
    private static final String TAGS_KEY = "tags";

    private static final String WHITELIST = "whitelist.txt";

    public LoadPreferences(Context context) {
        this.context = context;
    }

    public void getPreferencesJson(int id) {
        Utilities utilities = new Utilities();
        Handler handler = new Handler();
        handler.post(() -> {
            //String url = "http://points.temirtulpar.com/pref_" + id + ".json";
            String url = "https://ztta.kg/static/scs/pref_" + id + ".json";
            try {
                String prefs = new DownloadFromServerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url).get();
                if (prefs != null) {
                    JSONObject topLevel = new JSONObject(prefs);
                    JSONObject branch_info = topLevel.getJSONObject(BRANCH_KEY);
                    JSONObject connection_info = topLevel.getJSONObject(NETWORK_KEY);
                    JSONArray whitelist_info = topLevel.getJSONArray(TAGS_KEY);

                    new ParseBranchInfoAsync(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, branch_info);
                    new ParsePreferencesAsync(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, connection_info);
                    utilities.writeToFile(context, WHITELIST, whitelist_info.toString());
                }
            } catch (ExecutionException | InterruptedException | JSONException e) {
                Log.e("LOAD_PREFERENCES", "Exception: " + e.getMessage());
            }
        });
    }
}
