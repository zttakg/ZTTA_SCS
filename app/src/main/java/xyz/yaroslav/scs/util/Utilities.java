package xyz.yaroslav.scs.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import xyz.yaroslav.scs.R;

public class Utilities {
    SharedPreferences preferences;

    public void autoCloseDialog(Context context, String title, String message, int iconType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        switch (iconType) {
            case 1:
                builder.setIcon(R.drawable.ic_success);
                break;
            case 2:
                builder.setIcon(R.drawable.ic_error);
                break;
            case 3:
                builder.setIcon(R.drawable.ic_warning);
                break;
            case 4:
                builder.setIcon(R.drawable.ic_info);
                break;
        }
        builder.setCancelable(true);

        final AlertDialog closedialog = builder.create();

        closedialog.show();

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                closedialog.dismiss();
                timer.cancel();
            }
        }, 2000);

    }

    public String readFromFile(Context context, String file_name) {
        try {
            InputStream inputStream = context.openFileInput(file_name);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                return stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("READ_FILE", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("READ_FILE", "IO Exception: " + e.getMessage());
        }

        return "";
    }

    public boolean writeToFile(Context context, String file_name, String data) {
        File file = new File(file_name);
        if (file.exists() && data.equals(readFromFile(context, file_name))) {
            Log.i("WRITE_FILE", "Whitelist is actual. Skip re-writing");
        } else {
            try {
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file_name, Context.MODE_PRIVATE));
                outputStreamWriter.write(data);
                outputStreamWriter.close();
                return true;
            } catch (IOException e) {
                Log.e("WRITE_FILE", "IO Exception: " + e.toString());
            }
        }
        return false;
    }

    public String buildUrl(Context context, int type, @Nullable long... range) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int branch = preferences.getInt(context.getString(R.string.pref_key_branch_id), -1);
        String protocol = preferences.getString(context.getString(R.string.pref_key_net_protocol), context.getString(R.string.pref_value_net_protocol));
        String address = preferences.getString(context.getString(R.string.pref_key_net_address), context.getString(R.string.pref_value_net_address));
        String port = preferences.getString(context.getString(R.string.pref_key_net_port), context.getString(R.string.pref_value_net_port));
        String prefix = "";
        if (branch != -1) {
            switch (type) {
                case 0:
                    prefix = preferences.getString(context.getString(R.string.pref_key_net_add_new), context.getString(R.string.pref_value_net_add_new));
                    return protocol + "://" + address + ":" + port + "/" + prefix;
                case 1:
                    prefix = preferences.getString(context.getString(R.string.pref_key_net_whitelist), context.getString(R.string.pref_value_net_whitelist));
                    return protocol + "://" + address + ":" + port + "/" + prefix + branch;
                case 2:
                    prefix = preferences.getString(context.getString(R.string.pref_key_net_history), context.getString(R.string.pref_value_net_history));
                    String[] prefix_arr = prefix.split("&");

                    if (range != null) {
                        return protocol + "://" + address + ":" + port + "/" + prefix_arr[0] + range[0] + "&" + prefix_arr[1] + range[1] + "&" + prefix_arr[2] + branch;
                    } else {
                        return protocol + "://" + address + ":" + port + "/" + prefix + branch;
                    }
                default:
                    return protocol + "://" + address + ":" + port;
            }
        }
        return "";
    }

}

















