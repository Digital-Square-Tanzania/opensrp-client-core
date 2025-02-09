package org.smartregister.job;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.smartregister.AllConstants;

import java.util.concurrent.TimeUnit;

import timber.log.Timber;

/**
 * BaseWorker leverages WorkManager to schedule jobs and maintains an abstract
 * onRunJob(Params params) method for executing the work (for example, starting services
 * or running pending intents).
 *
 * The custom Params object wraps WorkManager’s input Data so you can access extra
 * parameters (e.g., TO_RESCHEDULE flag) in a manner similar to your original design.
 *
 * The custom JobResult enum includes a RESCHEDULE option; in doWork(), RESCHEDULE is mapped to retry.
 */
public abstract class BaseWorker extends Worker {

    public BaseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Custom enum to represent job execution results.
     */
    public enum JobResult {
        SUCCESS,
        FAILURE,
        RESCHEDULE
    }

    /**
     * A simple Params wrapper that exposes the extras from WorkManager’s input Data.
     */
    public static class Params {
        private final Data extras;

        public Params(Data extras) {
            this.extras = extras;
        }

        /**
         * Returns the extras passed when scheduling the work.
         */
        public Data getExtras() {
            return extras;
        }
    }

    /**
     * Subclasses must implement this method to perform their work.
     * Use the provided Params to access any extras.
     *
     * For example:
     *
     * <pre>
     * &#64;Override
     * protected JobResult onRunJob(@NonNull Params params) {
     *     Intent intent = new Intent(getApplicationContext(), CampaignIntentService.class);
     *     getApplicationContext().startService(intent);
     *     boolean toReschedule = params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false);
     *     return toReschedule ? JobResult.RESCHEDULE : JobResult.SUCCESS;
     * }
     * </pre>
     *
     * @param params A Params instance wrapping the input extras.
     * @return JobResult.SUCCESS if the work completed successfully,
     *         JobResult.FAILURE if it failed,
     *         or JobResult.RESCHEDULE to have WorkManager retry the work.
     */
    protected abstract JobResult onRunJob(@NonNull Params params);

    /**
     * The Worker’s doWork() method calls onRunJob() and maps the returned JobResult
     * to WorkManager’s Result.
     */
    @NonNull
    @Override
    public final Result doWork() {
        Params params = new Params(getInputData());
        JobResult jobResult;
        try {
            jobResult = onRunJob(params);
        } catch (Exception e) {
            Timber.e(e, "Error executing job: %s", getClass().getSimpleName());
            return Result.failure();
        }
        switch (jobResult) {
            case SUCCESS:
                return Result.success();
            case FAILURE:
                return Result.failure();
            case RESCHEDULE:
                return Result.retry();
            default:
                return Result.failure();
        }
    }

    /**
     * Helper method to execute a PendingIntent.
     *
     * @param pendingIntent The PendingIntent to be executed.
     * @return true if the PendingIntent was executed successfully; false otherwise.
     */
    protected boolean executePendingIntent(PendingIntent pendingIntent) {
        try {
            if (pendingIntent != null) {
                pendingIntent.send();
                return true;
            }
        } catch (PendingIntent.CanceledException e) {
            Timber.e(e, "PendingIntent was canceled.");
        }
        return false;
    }

    /**
     * Schedules periodic work using WorkManager.
     *
     * @param context               The application context.
     * @param jobTag                A unique tag/name for the job.
     * @param repeatIntervalMinutes The interval (in minutes) between each run. (Minimum is 15 minutes.)
     * @param flexIntervalMinutes   The flex interval (in minutes) defining the execution window.
     * @param workerClass           The Worker subclass that performs the work.
     */
    public static void scheduleJob(@NonNull Context context,
                                   @NonNull String jobTag,
                                   long repeatIntervalMinutes,
                                   long flexIntervalMinutes,
                                   @NonNull Class<? extends Worker> workerClass) {
        // WorkManager enforces a minimum repeat interval of 15 minutes.
        if (repeatIntervalMinutes < 15) {
            Timber.w("Provided repeatIntervalMinutes (%d) is less than the minimum allowed. Adjusting to 15 minutes.", repeatIntervalMinutes);
            repeatIntervalMinutes = 15;
        }

        // Flag to indicate rescheduling need (for compatibility with your original logic).
        boolean toReschedule = repeatIntervalMinutes < 15;

        // Pass extras via Data; add additional parameters as needed.
        Data inputData = new Data.Builder()
                .putBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, toReschedule)
                .build();

        // Build the periodic work request.
        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(workerClass, repeatIntervalMinutes, TimeUnit.MINUTES,
                        flexIntervalMinutes, TimeUnit.MINUTES)
                        .setInputData(inputData)
                        .build();

        // Enqueue unique periodic work with the KEEP policy.
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(jobTag, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);

        Timber.d("Scheduled periodic job with tag '%s': every %d minutes with a flex interval of %d minutes",
                jobTag, repeatIntervalMinutes, flexIntervalMinutes);
    }

    public static void scheduleJob(@NonNull Context context,
                                   @NonNull String jobTag,
                                   long repeatIntervalMinutes,
                                   long flexIntervalMinutes,
                                   @NonNull Class<? extends Worker> workerClass,
                                   @NonNull Data inputData) {
        if (repeatIntervalMinutes < 15) {
            Timber.w("Provided repeatIntervalMinutes (%d) is less than the minimum allowed. Adjusting to 15 minutes.", repeatIntervalMinutes);
            repeatIntervalMinutes = 15;
        }

        // Combine your custom inputData with any required defaults (if needed)
        Data combinedData = new Data.Builder()
                .putAll(inputData)
                .build();

        PeriodicWorkRequest periodicWorkRequest =
                new PeriodicWorkRequest.Builder(workerClass, repeatIntervalMinutes, TimeUnit.MINUTES,
                        flexIntervalMinutes, TimeUnit.MINUTES)
                        .setInputData(combinedData)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(jobTag, ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);

        Timber.d("Scheduled periodic job with tag '%s': every %d minutes with a flex interval of %d minutes",
                jobTag, repeatIntervalMinutes, flexIntervalMinutes);
    }

    /**
     * Schedules one-time immediate work using WorkManager.
     *
     * @param context     The application context.
     * @param jobTag      A unique tag/name for the job.
     * @param workerClass The Worker subclass that performs the work.
     */
    public static void scheduleJobImmediately(@NonNull Context context,
                                              @NonNull String jobTag,
                                              @NonNull Class<? extends Worker> workerClass) {
        OneTimeWorkRequest oneTimeWorkRequest =
                new OneTimeWorkRequest.Builder(workerClass)
                        .build();

        // Enqueue unique one-time work with the KEEP policy.
        WorkManager.getInstance(context)
                .enqueueUniqueWork(jobTag+"-Immediate", ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);

        Timber.d("Scheduled immediate job with tag '%s'", jobTag+"-Immediate");
    }

    public static void scheduleJobImmediately(@NonNull Context context,
                                              @NonNull String jobTag,
                                              @NonNull Class<? extends Worker> workerClass,
                                              @NonNull Data inputData) {
        OneTimeWorkRequest oneTimeWorkRequest =
                new OneTimeWorkRequest.Builder(workerClass)
                        .setInputData(inputData)
                        .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(jobTag+"-Immediate", ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);

        Timber.d("Scheduling immediate job with tag '%s'", jobTag+"-Immediate");
    }
}
