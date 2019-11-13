package xyz.yaroslav.scs.util;

import android.content.Context;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Timer;
import java.util.TimerTask;

import xyz.yaroslav.scs.R;

public class Utilities {

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

}
