package xyz.yaroslav.scs.ui.history;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import xyz.yaroslav.scs.R;

public class RangeFragment extends DialogFragment {
    private Button okButton;
    private Button cancelButton;
    private EditText beginRange;
    private EditText endRange;
    private Calendar calendar;


    private static final long dateCorrection = 1000 * 60 * 60 * 24;
    private static long todayDate = new Date().getTime();
    private long startTimeMsec = 0;
    private long endTimeMsec = 0;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_range, container, false);

        calendar = Calendar.getInstance();

        beginRange = view.findViewById(R.id.date_start);
        endRange = view.findViewById(R.id.date_finish);
        okButton = view.findViewById(R.id.button_ok);
        cancelButton = view.findViewById(R.id.button_cancel);

        beginRange.setOnClickListener(v -> setDate(beginRange, 1));
        endRange.setOnClickListener(v -> setDate(endRange, 2));

        okButton.setOnClickListener(v -> {
            makeSureThatTimeIsNormal();
            prepareReturnBundle();
        });

        cancelButton.setOnClickListener(v -> {
            getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, null);
            dismiss();
        });

        return view;
    }

    private void setDate(final EditText editText, final int flag) {
        DatePickerDialog dialog = new DatePickerDialog(getContext(), (view, year, monthOfYear, dayOfMonth) -> {
            calendar = setDefaultTime(calendar);
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            getDateInMilliseconds(editText, flag);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(todayDate);
        dialog.show();
    }

    private void getDateInMilliseconds(EditText edit_field, int typeOfDate) {
        long currentDateInMillisec = calendar.getTimeInMillis();
        String str = (DateUtils.formatDateTime(getContext(), currentDateInMillisec,DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR ));
        edit_field.setText(str);
        if (typeOfDate == 1) {
            startTimeMsec = currentDateInMillisec;
        } else if (typeOfDate == 2) {
            endTimeMsec = currentDateInMillisec;
        }
    }

    private static Calendar setDefaultTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Calendar todayDate() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.MILLISECOND, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.HOUR_OF_DAY, 0);
        return today;
    }

    private void makeSureThatTimeIsNormal() {
        if (endTimeMsec == 0) {
            endTimeMsec = todayDate().getTimeInMillis() + dateCorrection;
        }
        if (startTimeMsec == 0) {
            startTimeMsec = todayDate().getTimeInMillis();
        }
        if (startTimeMsec > endTimeMsec) {
            long temp = startTimeMsec;
            startTimeMsec = endTimeMsec;
            endTimeMsec = temp;
        } else if (startTimeMsec == endTimeMsec) {
            endTimeMsec += dateCorrection;
        }
    }

    private void prepareReturnBundle() {
        Bundle bundle = new Bundle();
        bundle.putLong("start", startTimeMsec);
        bundle.putLong("end", endTimeMsec);

        Intent intent = new Intent().putExtras(bundle);
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        dismiss();
    }
}






























