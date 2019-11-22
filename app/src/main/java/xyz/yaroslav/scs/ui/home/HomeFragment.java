package xyz.yaroslav.scs.ui.home;

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
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.TagItem;
import xyz.yaroslav.scs.util.Utilities;

public class HomeFragment extends Fragment {
    private HandlerThread parseIntentThread;

    private List<Map> white_list;
    private boolean isWhiteListExists = false;
    private int branchId;

    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (parseIntentThread != null) {
            parseIntentThread.quitSafely();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(root.getContext());
        branchId = sharedPreferences.getInt(getString(R.string.pref_key_branch_id), -1);

        white_list = new ArrayList<>();

        parseWhiteListJsonFromFile();

        return root;
    }

    public void processTag(Intent intent) {
        parseIntentThread = new HandlerThread("ParseIntentThread");
        parseIntentThread.start();
        Handler parseIntentHandler = new Handler(parseIntentThread.getLooper());

        parseIntentHandler.post(() -> {
            String tag_data = "";
            String tag_id = "";
            long cur_time = System.currentTimeMillis();

            Parcelable[] data = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            tag_data = parseTagData(data);

            if (Objects.equals(intent.getAction(), NfcAdapter.ACTION_TAG_DISCOVERED)) {
                tag_id = parseTagId(Objects.requireNonNull(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
            }

            if (!tag_id.equals("") && !tag_data.equals("")) {
                if (isWhiteListExists) {
                    if (compareTag(tag_id, tag_data)) {
                        new Utilities().autoCloseDialog(getContext(), tag_data, getString(R.string.message_success), 1);
                        JSONObject jsonObject = new Utilities().buidJsonObject(tag_id, tag_data, String.valueOf(cur_time), branchId);
                        new SendTagsAsync(getContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jsonObject);
                        new SaveTagsAsync(getContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, jsonObject);
                    } else {
                        new Utilities().autoCloseDialog(getContext(), getString(R.string.label_unknown_tag), getString(R.string.message_unknown_tag), 3);
                    }
                } else {
                    new Utilities().autoCloseDialog(getContext(), getString(R.string.label_compare_error), getString(R.string.message_white_list), 2);
                }
            } else {
                new Utilities().autoCloseDialog(getContext(), getString(R.string.label_error), getString(R.string.message_fail), 2);
            }
        });
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
        }, 250);
    }
}








































