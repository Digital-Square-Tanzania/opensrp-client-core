package org.smartregister.worker;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import org.smartregister.AllConstants;
import org.smartregister.job.BaseWorker;
import org.smartregister.sync.intent.SyncIntentService;

import timber.log.Timber;

/**
 * Created by cozej4 on 09/02/2015.
 */
public class SyncServiceWorker  extends BaseWorker {

    public static final String TAG = "SyncServiceWorker";

    public SyncServiceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    protected JobResult onRunJob(@NonNull Params params) {
        String serviceClassName = getInputData().getString("serviceClassName");

        if (serviceClassName == null) {
            Timber.e("Service class name is null");
            return JobResult.FAILURE;
        }

        try {
            // Convert class name back to a Class<?> object
            Class<?> serviceClass = Class.forName(serviceClassName);
            if (!SyncIntentService.class.isAssignableFrom(serviceClass)) {
                Timber.e("Provided class is not a valid SyncIntentService subclass");
                return JobResult.FAILURE;
            }

            // Create an intent to start the SyncIntentService
            Intent intent = new Intent(getApplicationContext(), serviceClass);

            // Create a PendingIntent with appropriate flags
            PendingIntent pendingIntent = createPendingIntent(getApplicationContext(), intent);

            // Execute the pending intent using the BaseWorker helper method
            boolean executed = executePendingIntent(pendingIntent);
            if (executed) {
                Timber.d("SyncServiceWorker started successfully via PendingIntent.");
            } else {
                Timber.e("Failed to execute PendingIntent for SyncServiceWorker.");
            }

            // Determine if the job should be rescheduled based on the extras.
            boolean toReschedule = params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false);
            Timber.d("SyncServiceWorker executed; toReschedule = %s", toReschedule);
            return toReschedule ? JobResult.RESCHEDULE : JobResult.SUCCESS;
        } catch (ClassNotFoundException e) {
            Timber.e(e, "Failed to find service class: %s", serviceClassName);
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
        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
    }
}
