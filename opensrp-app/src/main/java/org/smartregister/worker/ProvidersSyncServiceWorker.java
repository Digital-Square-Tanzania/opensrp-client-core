package org.smartregister.worker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.smartregister.AllConstants;
import org.smartregister.job.BaseWorker;
import org.smartregister.sync.intent.ProvidersSyncIntentService;

import timber.log.Timber;

/**
 * Created by cozej4 on 19/06/2025.
 */
public class ProvidersSyncServiceWorker extends BaseWorker {

    public static final String TAG = "SyncServiceWorker";

    public ProvidersSyncServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected JobResult onRunJob(@NonNull Params params) {
        try {
            Intent intent = new Intent(getApplicationContext(), ProvidersSyncIntentService.class);
            PendingIntent pendingIntent = createPendingIntent(getApplicationContext(), intent);

            boolean executed = executePendingIntent(pendingIntent);
            if (executed) {
                Timber.d("ProvidersSyncServiceWorker started ProvidersSyncIntentService successfully via PendingIntent.");
            } else {
                Timber.e("Failed to execute PendingIntent for ProvidersSyncIntentService.");
            }

            boolean toReschedule = params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false);
            Timber.d("ProvidersSyncServiceWorker executed; toReschedule = %s", toReschedule);
            return toReschedule ? JobResult.RESCHEDULE : JobResult.SUCCESS;
        } catch (Exception e) {
            Timber.e(e, "Failed to start ProvidersSyncIntentService.");
            return JobResult.FAILURE;
        }
    }

    /**
     * Helper method to create a PendingIntent for starting a service.
     *
     * @param context the application context.
     * @param intent  the Intent used to start the service.
     * @return a PendingIntent configured with the correct flags.
     */
    private PendingIntent createPendingIntent(Context context, Intent intent) {
        int flags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= 31) { // API level 31+
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(context, 0, intent, flags);
    }
}
