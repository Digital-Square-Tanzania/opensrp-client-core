package org.smartregister.util;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.smartregister.view.activity.DrishtiApplication;

import timber.log.Timber;

public class CrashLyticsTree extends Timber.Tree {
    private static final String CRASHLYTICS_KEY_PRIORITY = "priority";
    private static final String CRASHLYTICS_KEY_TAG = "tag";
    private static final String CRASHLYTICS_KEY_MESSAGE = "message";
    private String userName;

    @Override
    protected void log(int priority, @Nullable String tag, @NotNull String message, @Nullable Throwable t) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG || priority == Log.INFO) {
            return;
        }

        if (userName == null) {
            userName = DrishtiApplication.getInstance().getUsername();
        }

        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

        crashlytics.setCustomKey(CRASHLYTICS_KEY_PRIORITY, priority);
        crashlytics.setCustomKey(CRASHLYTICS_KEY_TAG, tag != null ? tag : "N/A");
        crashlytics.setCustomKey(CRASHLYTICS_KEY_MESSAGE, message);

        crashlytics.setUserId(userName);
        if (t == null) {
            crashlytics.recordException(new Exception(message));
        } else {
            crashlytics.recordException(t);
        }
    }
}
