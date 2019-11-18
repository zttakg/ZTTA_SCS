package xyz.yaroslav.scs.ui.history;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.yaroslav.scs.TagItem;

public class HistoryFileParseAsync extends AsyncTask<String, Void, List<TagItem>> {
    private static final String DATA_KEY = "tag_data";
    private static final String UID_KEY = "tag_id";
    private static final String TIME_KEY = "tag_time";

    @Override
    protected List<TagItem> doInBackground(String... strings) {
        List<TagItem> tagItems = new ArrayList<>();
        String data = strings[0];
        String[] arr = data.split(";");
        if (arr.length > 0) {
            tagItems.clear();
            for (String value : arr) {
                try {
                    JSONObject jsonObject = new JSONObject(value);
                    String tagName = jsonObject.getString(DATA_KEY);
                    String tagTime = jsonObject.getString(TIME_KEY);
                    String tagUid = jsonObject.getString(UID_KEY);
                    TagItem tagItem = new TagItem(tagUid, tagName, tagTime);
                    tagItems.add(tagItem);
                } catch (JSONException e) {
                    Log.e("PARSE_HISTORY_FILE", "JSON Exception in " + "(" + e.getClass() + "): " + e.getMessage());
                }
            }
            if (!tagItems.isEmpty()) {
                return tagItems;
            }
        }
        return null;
    }
}
