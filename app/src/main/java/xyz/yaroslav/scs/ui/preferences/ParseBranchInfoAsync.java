package xyz.yaroslav.scs.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.yaroslav.scs.R;

public class ParseBranchInfoAsync extends AsyncTask<JSONObject, Void, Integer> {
    private Context context;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    private static final String BRANCH_NAME = "name";
    private static final String BRANCH_CONTACT = "contact";

    public ParseBranchInfoAsync(Context context) {
        this.context = context;
    }

    @Override
    protected Integer doInBackground(JSONObject... jsonObjects) {
        int counter = 0;
        if (jsonObjects[0].length() > 0) {
            try {
                preferences = PreferenceManager.getDefaultSharedPreferences(context);
                editor = preferences.edit();
                editor.putString(context.getString(R.string.pref_key_branch_name), jsonObjects[0].getString(BRANCH_NAME));
                editor.putString(context.getString(R.string.pref_key_branch_phone), jsonObjects[0].getString(BRANCH_CONTACT));
                editor.apply();
            } catch (JSONException e) {
                Log.e("PARSE_BRANCH_INFO", "JSON Exception: " + e.getMessage());
            }
        }
        return counter;
    }
}
