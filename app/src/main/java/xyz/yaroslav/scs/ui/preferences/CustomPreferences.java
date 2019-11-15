package xyz.yaroslav.scs.ui.preferences;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import xyz.yaroslav.scs.R;

public class CustomPreferences extends PreferenceFragmentCompat {

    Preference branchId;
    Preference deleteCache;
    Preference deleteHistory;
    Preference deleteWhitelist;

    Preference branchPhone;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.fragment_preferences);

        branchId = findPreference(getString(R.string.pref_key_branch_id));
        branchId.setOnPreferenceClickListener(preference -> {
            int value = branchId.getSharedPreferences().getInt(getString(R.string.pref_key_branch_id), -1);
            showBranchById(value);
            return true;
        });

        branchPhone = findPreference(getString(R.string.pref_key_branch_phone));

        deleteHistory = findPreference(getString(R.string.pref_key_delete_history));
        deleteHistory.setOnPreferenceClickListener(preference -> {
            warningDialog(getString(R.string.file_history),1);
            return true;
        });

        deleteCache = findPreference(getString(R.string.pref_key_delete_cache));
        deleteCache.setOnPreferenceClickListener(preference -> {
            warningDialog(getString(R.string.file_cache), 2);
            return true;
        });

        deleteWhitelist = findPreference(getString(R.string.pref_key_delete_whitelist));
        deleteWhitelist.setOnPreferenceClickListener(preference -> {
            warningDialog(getString(R.string.file_whitelist), 3);
            return true;
        });
    }

    private void showBranchById(int value) {
        if (value != -1) {
            switch (value) {
                case 0:
                    Toast.makeText(getContext(), value + " : ZTTA", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(getContext(), value + " : PM", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(getContext(), value + " : Tire Company", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(getContext(), "Unknown id: " + value, Toast.LENGTH_SHORT).show();
                    break;
            }
        } else {
            Toast.makeText(getContext(), "Branch is empty. Restart app and set branch again", Toast.LENGTH_SHORT).show();
        }
    }

    private void warningDialog(String file_name, Integer type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getActivity()));
        builder.setTitle(getString(R.string.label_warning));
        if (type == 1) {
            builder.setMessage(getString(R.string.message_warning_delete_history));
        } else if (type == 2) {
            builder.setMessage(getString(R.string.message_warning_delete_temp));
        } else if (type == 3){
            builder.setMessage(getString(R.string.message_warning_delete_whitelist));
        } else {
            builder.setMessage(getString(R.string.message_warning_delete_file));
        }
        builder.setIcon(R.drawable.ic_warning);
        builder.setPositiveButton(getString(R.string.label_ok), (dialog, which) -> {
            Objects.requireNonNull(getContext()).deleteFile(file_name);
            dialog.dismiss();
            Toast.makeText(getContext(), "File deleted", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton(getString(R.string.label_cancel), (dialog, which) -> dialog.dismiss());

        final AlertDialog closedialog = builder.create();
        closedialog.show();
    }
}
