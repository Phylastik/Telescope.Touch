package io.github.marcocipriani01.telescopetouch.activities.fragments;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public abstract class ActionFragment extends Fragment implements Runnable {

    protected Context context;
    private volatile ActionListener listener = null;

    public void setActionEnabledListener(ActionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    public abstract boolean isActionEnabled();

    public void notifyActionChange() {
        if (listener != null) listener.setActionEnabled(isActionEnabled());
    }

    public void notifyActionDrawableChange() {
        if (listener != null) listener.onActionDrawableChange(getActionDrawable());
    }

    public void requestActionSnack(int msgRes) {
        if (listener != null) listener.actionSnackRequested(msgRes);
    }

    @SuppressWarnings("SameParameterValue")
    public void requestActionSnack(int msgRes, int actionName, View.OnClickListener action) {
        if (listener != null) listener.actionSnackRequested(msgRes, actionName, action);
    }

    public abstract int getActionDrawable();

    public interface ActionListener {
        void onActionDrawableChange(int resource);

        void setActionEnabled(boolean actionEnabled);

        void actionSnackRequested(int msgRes);

        void actionSnackRequested(int msgRes, int actionName, View.OnClickListener action);
    }
}