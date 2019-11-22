package xyz.yaroslav.scs.ui.home;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;

import xyz.yaroslav.scs.R;


public class SaveTagsAsync extends AsyncTask<JSONObject, Void, Void> {
    private Context context;

    public SaveTagsAsync(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(JSONObject... jsonObjects) {
        if (jsonObjects[0] != null) {
            String str = jsonObjects[0] + ";";
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(context.getString(R.string.file_history), Context.MODE_APPEND));
                outputStreamWriter.write(str);
                outputStreamWriter.close();
            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
        }
        return null;
    }
}
