package xyz.yaroslav.scs.ui.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.TagItem;
import xyz.yaroslav.scs.util.Utilities;

public class HomeFragment extends Fragment {
    private HandlerThread saveTagInLocalFile;
    private JSONArray jsonArray;

    private List<Map> white_list;
    private boolean isWhiteListExists = false;

    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        jsonArray = new JSONArray();
        white_list = new ArrayList<>();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        parseWhiteListJsonFromFile();


        return root;
    }

    public void processTag(Intent intent) {
        new IntentProcessingAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, intent);
    }

    private class IntentProcessingAsync extends AsyncTask<Intent, Void, Map> {
        @Override
        protected Map doInBackground(Intent... intents) {
            String tag_data = "";
            String tag_id = "";
            long cur_time = System.currentTimeMillis();

            Parcelable[] data = intents[0].getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            tag_data = parseTagData(data);

            if (Objects.equals(intents[0].getAction(), NfcAdapter.ACTION_TAG_DISCOVERED)) {
                tag_id = parseTagId(Objects.requireNonNull(intents[0].getByteArrayExtra(NfcAdapter.EXTRA_ID)));
            }

            @SuppressLint("UseSparseArrays") Map<Integer, String> flag_name = new HashMap<>();

            if (!tag_id.equals("") && !tag_data.equals("")) {
                if (isWhiteListExists) {
                    if (compareTag(tag_id, tag_data)) {
                        flag_name.put(1, tag_data);
                        saveInLocalFile(tag_id, tag_data, String.valueOf(cur_time));
                        String url = buildUrl();
                        if (url != null) {
                            sendTagsToServer(buildUrl(), tag_id, tag_data, String.valueOf(cur_time));
                        }
                    } else {
                        flag_name.put(2, tag_id);
                    }
                } else {
                    flag_name.put(3, tag_id);
                }
            } else {
                flag_name.put(-1, "");
            }

            return flag_name;
        }

        @Override
        protected void onPostExecute(Map pair) {
            if (pair.containsKey(1)) {
                new Utilities().autoCloseDialog(getContext(), pair.get(1).toString(), getString(R.string.message_success), 1);
                //autoCloseDialog(pair.get(1).toString(), getString(R.string.message_success), 1);
            } else if (pair.containsKey(2)) {
                new Utilities().autoCloseDialog(getContext(), getString(R.string.label_unknown_tag), getString(R.string.message_unknown_tag), 3);
                //autoCloseDialog(getString(R.string.label_unknown_tag), getString(R.string.message_unknown_tag), 3);
            } else if (pair.containsKey(3)) {
                new Utilities().autoCloseDialog(getContext(), getString(R.string.label_compare_error), getString(R.string.message_white_list), 2);
                //autoCloseDialog(getString(R.string.label_compare_error), getString(R.string.message_white_list), 2);
            } else {
                new Utilities().autoCloseDialog(getContext(), getString(R.string.label_error), getString(R.string.message_fail), 2);
                //autoCloseDialog(getString(R.string.label_error), getString(R.string.message_fail), 2);
            }
        }
    }

    private boolean compareTag(String uid, String tagName) {
        Map<String, Object> tagItem = new TagItem(uid, tagName).toWhiteListMap();
        return white_list.contains(tagItem);
    }

    private String parseTagData(Parcelable[] data) {
        StringBuilder tag_data = new StringBuilder();
        if (data != null) {
            try {
                for (Parcelable aData : data) {
                    NdefRecord[] recs = ((NdefMessage) aData).getRecords();
                    for (NdefRecord rec : recs) {
                        if (rec.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(rec.getType(), NdefRecord.RTD_TEXT)) {
                            byte[] payload = rec.getPayload();
                            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
                            int langCodeLen = payload[0] & 63;
                            tag_data.append(new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1, textEncoding));
                        }
                    }
                }
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e("TAG_DISPATCH", "Exception: " + e.getLocalizedMessage());
                } else {
                    e.printStackTrace();
                }
            }
        }
        if (tag_data.length() == 0) {
            return "";
        }
        return tag_data.toString();
    }

    private String parseTagId(byte [] bytesArray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        StringBuilder out = new StringBuilder();

        for (j = 0; j < bytesArray.length; ++j) {
            in = (int) bytesArray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out.append(hex[i]);
            i = in & 0x0f;
            out.append(hex[i]);
        }

        return out.toString();
    }

    private void parseWhiteListJsonFromFile() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            try {
                String jsonString = new Utilities().readFromFile(Objects.requireNonNull(getContext()), getString(R.string.file_whitelist));
                JSONArray jArray = new JSONArray(jsonString);

                white_list.clear();

                for (int i = 0; i < jArray.length(); i++) {
                    String tagName;
                    String tagUid ;
                    try {
                        JSONObject nestedObject = jArray.getJSONObject(i);
                        tagName = nestedObject.getString("tag_data");
                        tagUid = nestedObject.getString("tag_id");

                        Map<String,Object> tagItem = new TagItem(tagUid, tagName).toWhiteListMap();
                        white_list.add(tagItem);
                    } catch (JSONException e) {
                        Log.e("WHITE_LIST", "JSON Exception: " + e.getMessage());
                    }
                }

                if (white_list.size() > 0) {
                    isWhiteListExists = true;
                }
            } catch (JSONException e) {
                Log.e("WHITE_LIST", "JSON Parse Exception: " + e.getMessage());
            }
        }, 1000);
    }

    private void saveInLocalFile(String uid, String payload, String time) {
        saveTagInLocalFile = new HandlerThread("SaveTagLocal");
        saveTagInLocalFile.start();
        Handler saveLocalHandler = new Handler(saveTagInLocalFile.getLooper());

        saveLocalHandler.post(() -> {
            JSONObject jsonObject = buidJsonObject(uid, payload, time);
            if (jsonObject != null) {
                String str = jsonObject + ";";
                try {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getContext().openFileOutput(getString(R.string.file_history), Context.MODE_APPEND));
                    outputStreamWriter.write(str);
                    outputStreamWriter.close();
                } catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                } finally {
                    saveTagInLocalFile.quitSafely();
                }
            }
        });
    }

    private JSONObject buidJsonObject(String uid, String payload, String time) {
        int branch_id = sharedPreferences.getInt(getString(R.string.pref_key_branch_id), -1);
        if (branch_id == -1) {
            Toast.makeText(getContext(), getString(R.string.message_empty_branch), Toast.LENGTH_SHORT).show();
        } else {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("tag_id", uid);
                jsonObject.put("tag_data", payload);
                jsonObject.put("tag_time", time);
                jsonObject.put("br", branch_id);
                return jsonObject;
            } catch (JSONException e) {
                Log.e("BUILD_JSON", "JSON Exception: " + e.getMessage());
            }
        }
        return null;
    }

    private void sendTagsToServer(String srv_url, String tag_id, String tag_data, String timestamp) {
        try {
            URL url = new URL(srv_url);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject jsonObject = buidJsonObject(tag_id, tag_data, timestamp);
            if (jsonObject != null) {
                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                writer.write(jsonObject.toString());
                writer.flush();
                writer.close();
                os.close();

                conn.connect();
                if (conn.getResponseCode() != 200) {
                    writeTagToTempFile(tag_id, tag_data, timestamp);
                }
            }
            conn.disconnect();
        } catch (IOException e) {
            Log.e("HTTP_POST", "IO Exception: " + e.getMessage());
            writeTagToTempFile(tag_id, tag_data, timestamp);
        }
    }

    private void writeTagToTempFile(String tag_id, String tag_data, String timestamp) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getContext().openFileOutput(getString(R.string.file_cache), Context.MODE_APPEND));
            JSONObject jsonObject = buidJsonObject(tag_id, tag_data, timestamp);
            assert jsonObject != null;
            outputStreamWriter.write(jsonObject.toString() + ";");
            outputStreamWriter.close();
            Log.i("WRITE_TEMP", jsonObject.toString());
        } catch (IOException ex) {
            Log.e("Exception", "File write failed: " + ex.toString());
        }
    }

    private String buildUrl() {
        int branch = sharedPreferences.getInt(getString(R.string.pref_key_branch_id), -1);
        String protocol = sharedPreferences.getString(getString(R.string.pref_key_net_protocol), getString(R.string.pref_value_net_protocol));
        String address = sharedPreferences.getString(getString(R.string.pref_key_net_address), getString(R.string.pref_value_net_address));
        String port = sharedPreferences.getString(getString(R.string.pref_key_net_port), getString(R.string.pref_value_net_port));
        String prefix = sharedPreferences.getString(getString(R.string.pref_key_net_add_new), getString(R.string.pref_value_net_add_new));

        if (branch != -1) {
            return protocol + "://" + address + ":" + port + "/" + prefix;
        } else {
            return null;
        }
    }
}
