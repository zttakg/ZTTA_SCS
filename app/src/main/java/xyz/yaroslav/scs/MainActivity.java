package xyz.yaroslav.scs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.ui.history.HistoryDownloadAsync;
import xyz.yaroslav.scs.ui.home.HomeFragment;
import xyz.yaroslav.scs.ui.preferences.BranchDialog;
import xyz.yaroslav.scs.ui.preferences.ISelectedBranch;
import xyz.yaroslav.scs.ui.preferences.LoadPreferences;
import xyz.yaroslav.scs.util.Utilities;

public class MainActivity extends AppCompatActivity implements ISelectedBranch {

    private AppBarConfiguration mAppBarConfiguration;
    SharedPreferences preferences;

    private static final String WHITELIST = "whitelist.txt";
    public static final String BRANCH = "branch_id_key";
    private int branch;

    NfcManager nfcManager;
    NfcAdapter nfcAdapter;

    TextView nfcLabel;
    TextView wifiLabel;
    ImageView nfcIcon;
    ImageView wifiIcon;

    TextView branchNameField;
    TextView branchPhoneField;

    List<Map> whiteList;
    private boolean isWhiteListExists = false;

    private final String[][] techList = new String[][] {
            new String[] {
                    NfcA.class.getName(),
                    NfcB.class.getName(),
                    NfcV.class.getName(),
                    IsoDep.class.getName(),
                    MifareClassic.class.getName(),
                    MifareUltralight.class.getName(),
                    Ndef.class.getName()
            }
    };

    private HandlerThread whitelistThread;
    private Handler whilelistHandler;
    private HandlerThread jsonParseThread;
    private Handler jsonParseHandler;
    private HandlerThread saveTagInLocalFile;
    private HandlerThread updateTagsInHistory;
    private Handler updateTagsHandler;
    private HandlerThread sendTagsWhenOnline;
    private Handler sendTagsOnlineHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setScreenOrientationToPortrait();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        branch = preferences.getInt(BRANCH, -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        nfcLabel = findViewById(R.id.nfc_text);
        wifiLabel = findViewById(R.id.net_text);
        nfcIcon = findViewById(R.id.nfc_icon);
        wifiIcon = findViewById(R.id.net_icon);

        whiteList = new ArrayList<>();

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_history, R.id.nav_whitelist, R.id.nav_unsent,
                R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        whitelistThread = new HandlerThread("WhiteListThread");
        jsonParseThread = new HandlerThread("JsonParseThread");
        sendTagsWhenOnline = new HandlerThread("SendTagsWhenOnlineThread");
        //updateTagsInHistory = new HandlerThread("UpdateTagsHistory");

        View headerView = navigationView.getHeaderView(0);
        branchNameField = headerView.findViewById(R.id.branchName);
        branchPhoneField = headerView.findViewById(R.id.branchPhone);
    }


    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkNfcState();
        checkWifiState();

        if (branch == -1) {
            DialogFragment dialogFragment = new BranchDialog();
            dialogFragment.show(getSupportFragmentManager(), "BRANCH_DIALOG");
        } else {
            startBackgroundOperations(branch);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        listenForNfc();
    }

    @Override
    public void onSelectedBranch(int branchId) {
        SharedPreferences.Editor editor;
        if (branchId != -1) {
            editor = preferences.edit();
            editor.putInt(BRANCH, branchId);
            editor.apply();
            startBackgroundOperations(branchId);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.message_empty_branch), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        whitelistThread.quitSafely();
        jsonParseThread.quitSafely();
        //updateTagsInHistory.quitSafely();
        if (sendTagsWhenOnline != null) {
            sendTagsWhenOnline.quitSafely();
        }
    }

    private void startBackgroundOperations(int branch_id) {
        LoadPreferences loadPreferences = new LoadPreferences(getApplicationContext());
        loadPreferences.getPreferencesJson(branch_id);

        branchNameField.setText(preferences.getString(getString(R.string.pref_key_branch_name), getString(R.string.pref_value_branch_name)));
        branchPhoneField.setText(preferences.getString(getString(R.string.pref_key_branch_phone), getString(R.string.pref_value_branch_phone)));

        if (!whitelistThread.isAlive()) {
            whitelistThread.start();
            whilelistHandler = new Handler(whitelistThread.getLooper());
        }

        if (!jsonParseThread.isAlive()) {
            jsonParseThread.start();
            jsonParseHandler = new Handler(jsonParseThread.getLooper());
        }

        /*
        if (!updateTagsInHistory.isAlive()) {
            updateTagsInHistory.start();
            updateTagsHandler = new Handler(updateTagsInHistory.getLooper());
        }
        */

        loadWhiteListFromServer();
        parseWhiteListJsonFromFile();
        //updateLocalHistory();
        tryToSendSavedTags();
    }

    private void setScreenOrientationToPortrait() {
        switch (getResources().getConfiguration().orientation){
            case Configuration.ORIENTATION_PORTRAIT:

            case Configuration.ORIENTATION_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    public boolean isNfcResponsible() {
        nfcManager = (NfcManager) Objects.requireNonNull(getApplicationContext()).getSystemService(Context.NFC_SERVICE);
        if (nfcManager != null) {
            boolean service_enabled = false;
            try {
                nfcAdapter = nfcManager.getDefaultAdapter();
                if (nfcAdapter != null && nfcAdapter.isEnabled()) {
                    service_enabled = true;
                }
            } catch (NullPointerException e) {
                Log.e("NFC_INIT", "Exception: " + e.getLocalizedMessage());
            }
            return service_enabled;
        } else {
            return false;
        }
    }

    public boolean isWiFiResponsible() {
        WifiManager wifi = (WifiManager) Objects.requireNonNull(getApplicationContext()).getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            return wifi.isWifiEnabled();
        } else {
            return false;
        }
    }

    public void checkNfcState() {
        Timer nfcTimer = new Timer();
        final Handler nfcHandler = new Handler();
        nfcTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final int statusColor;
                if (isNfcResponsible()) {
                    statusColor = getResources().getColor(R.color.colorGreen);
                } else {
                    statusColor = getResources().getColor(R.color.colorRed);
                }
                nfcHandler.post(() -> {
                    nfcLabel.setTextColor(statusColor);
                    nfcIcon.setColorFilter(statusColor);
                });
            }
        }, 0L, 3L * 1000);
    }

    public void checkWifiState() {
        Timer netTimer = new Timer();
        final Handler netHandler = new Handler();
        netTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final int textColor;
                try {
                    if (isWiFiResponsible()) {
                        textColor = getResources().getColor(R.color.colorGreen);
                    } else {
                        textColor = getResources().getColor(R.color.colorRed);
                    }
                    netHandler.post(() -> {
                        wifiLabel.setTextColor(textColor);
                        wifiIcon.setColorFilter(textColor);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0L, 2L * 1000);
    }

    private void listenForNfc() {
        if (isNfcResponsible()) {
            try {
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                IntentFilter filter = new IntentFilter();
                filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
                filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
                filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
                nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
            } catch (Exception e) {
                Log.e("LISTEN_NFC", "Exception: " + e.getLocalizedMessage());
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        FragmentManager fragmentManager = MainActivity.this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof NavHostFragment) {
               List<Fragment> childFragments = fragment.getChildFragmentManager().getFragments();
               for (Fragment childFragment : childFragments) {
                   if (childFragment instanceof HomeFragment) {
                       HomeFragment homeFragment = (HomeFragment) childFragment;
                       homeFragment.processTag(intent);
                   }
               }
            }
        }
    }

    private void parseWhiteListJsonFromFile() {
        jsonParseHandler.postDelayed(() -> {
            try {
                String jsonString = new Utilities().readFromFile(getApplicationContext(), WHITELIST);
                JSONArray jArray = new JSONArray(jsonString);

                whiteList.clear();

                for (int i = 0; i < jArray.length(); i++) {
                    String tagName;
                    String tagUid ;
                    try {
                        JSONObject nestedObject = jArray.getJSONObject(i);
                        tagName = nestedObject.getString("tag_data");
                        tagUid = nestedObject.getString("tag_id");

                        Map<String,Object> tagItem = new TagItem(tagUid, tagName).toWhiteListMap();
                        whiteList.add(tagItem);
                    } catch (JSONException e) {
                        Log.e("WHITE_LIST", "JSON Exception: " + e.getMessage());
                    }
                }

                if (whiteList.size() > 0) {
                    isWhiteListExists = true;
                }
            } catch (JSONException e) {
                Log.e("WHITE_LIST", "JSON Parse Exception: " + e.getMessage());
            }
        }, 1000);
    }

    private void loadWhiteListFromServer() {
        whilelistHandler.postDelayed(() -> {
            try {
                String data = new HistoryDownloadAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, buildUrlWhitelist()).get();
                if (data != null) {
                    JSONObject jsonObject = new JSONObject(data);
                    JSONArray jsonArray = jsonObject.getJSONArray("tags");
                    if (new Utilities().writeToFile(getApplicationContext(), getString(R.string.file_whitelist), jsonArray.toString())) {
                        Log.i("WHITE_LIST", "White List saved");
                    } else {
                        Log.i("WHITE_LIST", "White List does NOT saved!");
                    }
                }
            } catch (ExecutionException | InterruptedException | JSONException e) {
                Log.e("WHITE_LIST", "Exception: " + e.getMessage());
            }
        }, 1000);
    }


    private void tryToSendSavedTags() {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            sendTagsWhenOnline.start();
            sendTagsOnlineHandler = new Handler(sendTagsWhenOnline.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    sendSavedOfllineTags();
                    sendTagsOnlineHandler.sendEmptyMessageDelayed(0, 60 * 1000);
                }
            };
            sendTagsOnlineHandler.sendEmptyMessage(0);
        }, 5000);
    }

    private void sendSavedOfllineTags() {
        String tmp = new Utilities().readFromFile(getApplicationContext(), getString(R.string.file_cache));
        String[] arr = tmp.split(";");
        if (arr.length > 0) {
            ArrayList<String> list = new ArrayList<>(Arrays.asList(arr));
            ArrayList<String> temp = new ArrayList<>(Arrays.asList(arr));
            for (String s : list) {
                if (s.length() != 0) {
                    try {
                        Integer result = new SendSavedTagToServerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, buildUrlNew(), s).get();
                        if (result == 200) {
                            temp.remove(s);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        Log.e("SAVED_TAGS", "AsyncTask exception: " + e.getMessage());
                    }
                }
            }
            rewriteCacheFile(temp);
        }
    }

    private void rewriteCacheFile(List<String> values)  {
        try {
            OutputStreamWriter streamWriter = new OutputStreamWriter(openFileOutput(getString(R.string.file_cache), Context.MODE_PRIVATE));
            streamWriter.write("");
            streamWriter.close();
            Log.i("CACHE_FILE", "Deleted");
            if (values.size() > 0) {
                for (String value : values) {
                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(getString(R.string.file_cache), Context.MODE_APPEND));
                    outputStreamWriter.write(value);
                    outputStreamWriter.close();
                }
                Log.i("CACHE_FILE", "Updated");
            }
        }
        catch (IOException e) {
            Log.e("Exception", "Cache file write failed: " + e.toString());
        }
    }

    private class SendSavedTagToServerAsync extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... strings) {
            try {
                URL url = new URL(strings[0]);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

                if (strings[1] != null) {
                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
                    writer.write(strings[1]);
                    writer.flush();
                    writer.close();
                    os.close();

                    conn.connect();
                    return conn.getResponseCode();
                }
                conn.disconnect();
            } catch (IOException e) {
                Log.e("HTTP_POST", "IO Exception: " + e.getMessage());
            }
            return 0;
        }
    }

    private String buildUrlWhitelist() {
        int _branch = preferences.getInt(getString(R.string.pref_key_branch_id), -1);
        String protocol = preferences.getString(getString(R.string.pref_key_net_protocol), getString(R.string.pref_value_net_protocol));
        String address = preferences.getString(getString(R.string.pref_key_net_address), getString(R.string.pref_value_net_address));
        String port = preferences.getString(getString(R.string.pref_key_net_port), getString(R.string.pref_value_net_port));
        String prefix = preferences.getString(getString(R.string.pref_key_net_whitelist), getString(R.string.pref_value_net_whitelist));

        if (_branch != -1) {
            return protocol + "://" + address + ":" + port + "/" + prefix + _branch;
        } else {
            return "";
        }
    }

    private String buildUrlNew() {
        int _branch = preferences.getInt(getString(R.string.pref_key_branch_id), -1);
        String protocol = preferences.getString(getString(R.string.pref_key_net_protocol), getString(R.string.pref_value_net_protocol));
        String address = preferences.getString(getString(R.string.pref_key_net_address), getString(R.string.pref_value_net_address));
        String port = preferences.getString(getString(R.string.pref_key_net_port), getString(R.string.pref_value_net_port));
        String prefix = preferences.getString(getString(R.string.pref_key_net_add_new), getString(R.string.pref_value_net_add_new));

        if (_branch != -1) {
            return protocol + "://" + address + ":" + port + "/" + prefix;
        } else {
            return "";
        }
    }
}
