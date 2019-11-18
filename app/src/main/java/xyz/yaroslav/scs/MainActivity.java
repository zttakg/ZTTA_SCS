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
import android.util.Log;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import xyz.yaroslav.scs.ui.home.HomeFragment;
import xyz.yaroslav.scs.ui.preferences.BranchDialog;
import xyz.yaroslav.scs.ui.preferences.ISelectedBranch;
import xyz.yaroslav.scs.ui.preferences.LoadPreferences;
import xyz.yaroslav.scs.ui.unsent.SendSavedTags;
import xyz.yaroslav.scs.util.DownloadFromServerAsync;
import xyz.yaroslav.scs.util.Utilities;

public class MainActivity extends AppCompatActivity implements ISelectedBranch {

    private AppBarConfiguration mAppBarConfiguration;
    SharedPreferences preferences;

    private int branch;

    NfcManager nfcManager;
    NfcAdapter nfcAdapter;

    TextView nfcLabel;
    TextView wifiLabel;
    ImageView nfcIcon;
    ImageView wifiIcon;

    TextView branchNameField;
    TextView branchPhoneField;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setScreenOrientationToPortrait();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        branch = preferences.getInt(getString(R.string.pref_key_branch_id), -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        nfcLabel = findViewById(R.id.nfc_text);
        wifiLabel = findViewById(R.id.net_text);
        nfcIcon = findViewById(R.id.nfc_icon);
        wifiIcon = findViewById(R.id.net_icon);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_history, R.id.nav_whitelist, R.id.nav_unsent,
                R.id.nav_settings)
                .setDrawerLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        View headerView = navigationView.getHeaderView(0);
        branchNameField = headerView.findViewById(R.id.branchName);
        branchPhoneField = headerView.findViewById(R.id.branchPhone);

        branchNameField.setText(preferences.getString(getString(R.string.pref_key_branch_name), getString(R.string.pref_value_branch_name)));
        branchPhoneField.setText(preferences.getString(getString(R.string.pref_key_branch_phone), getString(R.string.pref_value_branch_phone)));
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
            sendSavedTags();
            updateWhitelist();
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
            editor.putInt(getString(R.string.pref_key_branch_id), branchId);
            editor.apply();
            new LoadPreferences(getApplicationContext()).getPreferencesJson(branchId);
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.message_empty_branch), Toast.LENGTH_LONG).show();
        }
    }

    private void sendSavedTags() {
        Timer delayTimer = new Timer();
        final Handler sendHandler = new Handler();
        delayTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendHandler.post(() -> {
                    if (isWiFiResponsible()) {
                        new SendSavedTags().tryToSendTags(getApplicationContext());
                    }
                });
            }
        }, 0L, 60L * 1000);
    }

    private void updateWhitelist() {
        Handler whitelistHandler = new Handler();
        whitelistHandler.postDelayed(() -> {
            try {
                Utilities util = new Utilities();
                String url = util.buildUrl(getApplicationContext(), 1);
                String result = new DownloadFromServerAsync().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url).get();
                if (result != null && !result.isEmpty()) {
                    JSONObject jsonObject = new JSONObject(result);
                    JSONArray whitelist = jsonObject.getJSONArray(getString(R.string.json_key_tags));
                    if (!util.readFromFile(getApplicationContext(), getString(R.string.file_whitelist)).equals(whitelist.toString())) {
                        util.writeToFile(getApplicationContext(), getString(R.string.file_whitelist), whitelist.toString());
                    }
                }
            } catch (JSONException | ExecutionException | InterruptedException e) {
                Log.e("WHITELIST_UPDATE", "Exception: " + e.getMessage());
            }
        }, 3000);
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
}
