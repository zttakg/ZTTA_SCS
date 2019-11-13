package xyz.yaroslav.scs.ui.history;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.yaroslav.scs.TagItem;

public class HistoryParseAsync extends AsyncTask<String, Void, List<TagItem>> {
    private static final String TOP_KEY = "events";
    private static final String DATA_KEY = "tag_data";
    private static final String UID_KEY = "tag_id";
    private static final String TIME_KEY = "tag_time";

    @Override
    protected List<TagItem> doInBackground(String... strings) {
        List<TagItem> tagItemList = new ArrayList<>();
        try {
            JSONObject topLevel = new JSONObject(strings[0]);
            JSONArray jArray = topLevel.getJSONArray(TOP_KEY);

            for (int i = 0; i < jArray.length(); i++) {
                JSONObject nestedObject = jArray.getJSONObject(i);
                String tagName = nestedObject.getString(DATA_KEY);
                String tagTime = nestedObject.getString(TIME_KEY);
                String tagUid = nestedObject.getString(UID_KEY);

                TagItem tagItem = new TagItem(tagUid, tagName, tagTime);
                tagItemList.add(tagItem);
            }

            return tagItemList;
        } catch (JSONException e) {
            Log.e("GET_HISTORY", "JSON Exception: " + "(" + e.getClass() + "): " + e.getMessage());
        }
        return null;
    }
}
