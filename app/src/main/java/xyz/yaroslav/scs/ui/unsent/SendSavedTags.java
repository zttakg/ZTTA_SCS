package xyz.yaroslav.scs.ui.unsent;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.R;
import xyz.yaroslav.scs.util.UploadToServerAsync;
import xyz.yaroslav.scs.util.Utilities;

public class SendSavedTags {
    private ArrayList<String> retrieveTags(Context context) {
        Utilities util = new Utilities();
        String tmp = util.readFromFile(context, context.getString(R.string.file_cache));
        Log.i("RETRIEVED", "" + tmp);
        if (!tmp.equals("") && tmp.length() > 0) {
            String[] arr = tmp.split(";");
            ArrayList<String> list;
            if (arr[arr.length - 1].equals("")) {
                list = new ArrayList<>(Arrays.asList(Arrays.copyOf(arr, arr.length - 1)));
            } else {
                list = new ArrayList<>(Arrays.asList(arr));
            }
            return list;
        } else {
            return null;
        }
    }

    public void tryToSendTags(Context context) {
        if (isFileExist(context)) {
            ArrayList<String> list = retrieveTags(context);
            if (list != null && list.size() > 0) {
                ArrayList<String> temp = new ArrayList<>(list);
                for (String item : list) {
                    try {
                        int result = new UploadToServerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Utilities().buildUrl(context, 0), item).get();
                        if (result == 200) {
                            temp.remove(item);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e("SAVED_TAGS", "AsyncTask exception: " + e.getMessage());
                    }
                }
                rewriteCacheFile(context, temp);
            }
        }
    }

    private void rewriteCacheFile(Context context, ArrayList<String> values)  {
        try {
            OutputStreamWriter clearStreamWriter = new OutputStreamWriter(context.openFileOutput(context.getString(R.string.file_cache), Context.MODE_PRIVATE));
            clearStreamWriter.write("");
            clearStreamWriter.close();
            if (values.size() > 0) {
                for (String value : values) {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(context.getString(R.string.file_cache), Context.MODE_APPEND));
                    outputStreamWriter.write(value);
                    outputStreamWriter.close();
                }
            } else {
                Log.i("REWRITE_CACHE", "File is empty. All tags has been sent");
            }
        } catch (IOException e) {
            Log.e("Exception", "Cache file write failed: " + e.toString());
        }
    }

    private boolean isFileExist(Context context) {
        File file = context.getFileStreamPath(context.getString(R.string.file_cache));
        return file.exists() && file.length() > 0;
    }

}
