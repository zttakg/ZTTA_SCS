package xyz.yaroslav.scs.ui.whitelist;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.yaroslav.scs.TagItem;

public class WhitelistParseAsync extends AsyncTask<String, Void, List<TagItem>> {
    @Override
    protected List<TagItem> doInBackground(String... strings) {
        try {
            JSONArray jsonArray = new JSONArray(strings[0]);
            List<TagItem> whiteList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                String tagName;
                String tagUid;

                JSONObject nestedObject = jsonArray.getJSONObject(i);
                tagName = nestedObject.getString("tag_data");
                tagUid = nestedObject.getString("tag_id");

                TagItem tagItem = new TagItem(tagUid, tagName);
                whiteList.add(tagItem);
            }

            return whiteList;
        } catch (JSONException e) {
            Log.e("WHITE_LIST_PARSE", "JSON Exception: " + e.getMessage());
        }
        return null;
    }
}
