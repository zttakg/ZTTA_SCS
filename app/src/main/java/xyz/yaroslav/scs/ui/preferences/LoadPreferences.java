package xyz.yaroslav.scs.ui.preferences;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.util.DownloadFromServerAsync;
import xyz.yaroslav.scs.util.Utilities;

public class LoadPreferences {
    private Context context;

    public LoadPreferences(Context context) {
        this.context = context;
    }

    public void getPreferencesJson(int id) {
        Utilities utilities = new Utilities();
        Handler handler = new Handler();
        handler.post(() -> {
            String url = "https://ztta.kg/static/scs/pref_" + id + ".json";
            try {
                String prefs = new DownloadFromServerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url).get();
                if (prefs != null) {
                    JSONObject topLevel = new JSONObject(prefs);
                    JSONObject branch_info = topLevel.getJSONObject(context.getString(R.string.json_key_branch));
                    JSONObject connection_info = topLevel.getJSONObject(context.getString(R.string.json_key_network));
                    JSONArray whitelist_info = topLevel.getJSONArray(context.getString(R.string.json_key_tags));

                    new ParseBranchInfoAsync(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, branch_info);
                    new ParsePreferencesAsync(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, connection_info);
                    utilities.writeToFile(context, context.getString(R.string.file_whitelist), whitelist_info.toString());
                }
            } catch (ExecutionException | InterruptedException | JSONException e) {
                Log.e("LOAD_PREFERENCES", "Exception: " + e.getMessage());
            }
        });
    }
}
