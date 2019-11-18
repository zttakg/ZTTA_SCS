package xyz.yaroslav.scs.ui.preferences;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import xyz.yaroslav.scs.R;

public class BranchDialog extends DialogFragment {
    private Button saveButton;
    private RadioGroup branchGroup;

    private ISelectedBranch mCallback;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mCallback = (ISelectedBranch) context;
        } catch (ClassCastException e) {
            Log.e("ON_BRANCH_DIALOG_ATTACH", "Exception: " + e.getMessage());
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_branch, container, false);

        branchGroup = view.findViewById(R.id.branch_rg);
        saveButton = view.findViewById(R.id.button_ok);

        saveButton.setOnClickListener(v -> {
            boolean selected = false;
            int branch_id = -1;
            switch (branchGroup.getCheckedRadioButtonId()){
                case R.id.branch1:
                    branch_id = 0;
                    selected = true;
                    break;
                case R.id.branch2:
                    branch_id = 1;
                    selected = true;
                    break;
                case R.id.branch3:
                    branch_id = 2;
                    selected = true;
                    break;
                default:
                    Toast.makeText(getContext(), getString(R.string.toast_branch_not_selected), Toast.LENGTH_SHORT).show();
            }
            if (selected) {
                mCallback.onSelectedBranch(branch_id);
                getDialog().dismiss();
            }
        });

        return view;
    }
}
