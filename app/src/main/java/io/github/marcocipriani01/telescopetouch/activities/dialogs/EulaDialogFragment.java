package io.github.marcocipriani01.telescopetouch.activities.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.StardroidApplication;
import io.github.marcocipriani01.telescopetouch.inject.HasComponent;
import io.github.marcocipriani01.telescopetouch.util.MiscUtil;

/**
 * End User License agreement dialog.
 * Created by johntaylor on 4/3/16.
 */
public class EulaDialogFragment extends DialogFragment {
    private static final String TAG = MiscUtil.getTag(EulaDialogFragment.class);
    @Inject
    Activity parentActivity;
    private EulaAcceptanceListener resultListener;

    public void setEulaAcceptanceListener(EulaAcceptanceListener resultListener) {
        this.resultListener = resultListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Activities using this dialog MUST implement this interface.  Obviously.
        ((HasComponent<ActivityComponent>) getActivity()).getComponent().inject(this);

        LayoutInflater inflater = parentActivity.getLayoutInflater();
        View view = inflater.inflate(R.layout.tos_view, null);

        String apologyText = parentActivity.getString(R.string.language_apology_text);
        Spanned formattedApologyText = Html.fromHtml(apologyText);
        TextView apologyTextView = (TextView) view.findViewById(R.id.language_apology_box_text);
        apologyTextView.setText(formattedApologyText, TextView.BufferType.SPANNABLE);

        String eulaText = parentActivity.getString(R.string.eula_text);
        Spanned formattedEulaText = Html.fromHtml(eulaText);
        TextView eulaTextView = (TextView) view.findViewById(R.id.eula_box_text);
        eulaTextView.setText(formattedEulaText, TextView.BufferType.SPANNABLE);

        AlertDialog.Builder tosDialogBuilder = new AlertDialog.Builder(parentActivity)
                .setTitle(R.string.menu_tos)
                .setView(view);
        if (resultListener != null) {
            tosDialogBuilder
                    .setPositiveButton(R.string.dialog_accept,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    acceptEula(dialog);
                                }
                            })
                    .setNegativeButton(R.string.dialog_decline,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    rejectEula(dialog);
                                }
                            });
        }
        return tosDialogBuilder.create();
    }

    private void acceptEula(DialogInterface dialog) {
        Log.d(TAG, "TOS Dialog closed.  User accepts.");
        dialog.dismiss();
        if (resultListener != null) {
            resultListener.eulaAccepted();
        }
    }

    private void rejectEula(DialogInterface dialog) {
        Log.d(TAG, "TOS Dialog closed.  User declines.");
        dialog.dismiss();
        if (resultListener != null) {
            resultListener.eulaRejected();
        }
    }

    private String getVersionName() {
        return ((StardroidApplication) parentActivity.getApplication()).getVersionName();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        rejectEula(dialog);
    }

    public interface EulaAcceptanceListener {
        void eulaAccepted();

        void eulaRejected();
    }

    public interface ActivityComponent {
        void inject(EulaDialogFragment fragment);
    }
}
