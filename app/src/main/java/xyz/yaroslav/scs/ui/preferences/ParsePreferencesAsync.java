package xyz.yaroslav.scs.ui.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import xyz.yaroslav.scs.R;

public class ParsePreferencesAsync extends AsyncTask<JSONObject, Void, Integer> {
    private Context context;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    private static final String NETWORK_PROTOCOL = "protocol";
    private static final String NETWORK_ADDRESS = "address";
    private static final String NETWORK_PORT = "port";
    private static final String NETWORK_POSTFIX_TAGS = "postfix_tags";
    private static final String NETWORK_POSTFIX_NEW = "postfix_new";
    private static final String NETWORK_POSTFIX_GET = "postfix_get";

    public ParsePreferencesAsync(Context context) { this.context = context; }

    @Override
    protected Integer doInBackground(JSONObject... jsonObjects) {
        int counter = 0;
        if (jsonObjects[0].length() > 0) {
            try {
                preferences = PreferenceManager.getDefaultSharedPreferences(context);
                editor = preferences.edit();
                editor.putString(context.getString(R.string.pref_key_net_protocol), jsonObjects[0].getString(NETWORK_PROTOCOL));
                editor.putString(context.getString(R.string.pref_key_net_address), jsonObjects[0].getString(NETWORK_ADDRESS));
                editor.putString(context.getString(R.string.pref_key_net_port), jsonObjects[0].getString(NETWORK_PORT));
                editor.putString(context.getString(R.string.pref_key_net_whitelist), jsonObjects[0].getString(NETWORK_POSTFIX_TAGS));
                editor.putString(context.getString(R.string.pref_key_net_add_new), jsonObjects[0].getString(NETWORK_POSTFIX_NEW));
                editor.putString(context.getString(R.string.pref_key_net_history), jsonObjects[0].getString(NETWORK_POSTFIX_GET));
                editor.apply();
            } catch (JSONException e) {
                Log.e("PARSE_NETWORK_SETTINGS", "JSONException: " + e.getMessage());
            }
        }
        return counter;
    }
}
