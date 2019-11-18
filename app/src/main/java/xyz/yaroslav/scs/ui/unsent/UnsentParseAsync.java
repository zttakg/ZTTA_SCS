package xyz.yaroslav.scs.ui.unsent;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import xyz.yaroslav.scs.TagItem;

public class UnsentParseAsync extends AsyncTask<String, Void, List<TagItem>> {
    @Override
    protected List<TagItem> doInBackground(String... strings) {
        String[] arr = strings[0].split(";");
        if (arr.length > 0) {
            List<TagItem> tagItems = new ArrayList<>();
            for (String value : arr) {
                try {
                    JSONObject jsonObject = new JSONObject(value);
                    String tagName = jsonObject.getString("tag_data");
                    String tagTime = jsonObject.getString("tag_time");
                    String tagUid = jsonObject.getString("tag_id");
                    TagItem tagItem = new TagItem(tagUid, tagName, tagTime);
                    tagItems.add(tagItem);
                } catch (JSONException e) {
                    Log.e("TEMP_FILE", "JSON Exception in " + "(" + e.getClass() + "): " + e.getMessage());
                }
            }
            if (!tagItems.isEmpty()) {
                return tagItems;
            }
        }
        return null;
    }
}
