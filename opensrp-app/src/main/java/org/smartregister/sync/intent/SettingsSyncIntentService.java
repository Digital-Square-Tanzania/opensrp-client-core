package org.smartregister.sync.intent;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.job.BaseWorker;
import org.smartregister.job.SyncServiceJob;
import org.smartregister.sync.helper.SyncSettingsServiceHelper;
import org.smartregister.worker.SyncServiceWorker;

import static org.smartregister.util.Log.logError;

import androidx.work.Data;

/**
 * Created by ndegwamartin on 14/09/2018.
 */
public class SettingsSyncIntentService extends BaseSyncIntentService {
    public static final String SETTINGS_URL = "/rest/settings/sync";

    private static final String TAG = SettingsSyncIntentService.class.getCanonicalName();

    protected SyncSettingsServiceHelper syncSettingsServiceHelper;

    public static final String EVENT_SYNC_COMPLETE = "event_sync_complete";

    public SettingsSyncIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean isSuccessfulSync = processSettings(intent);
        if (isSuccessfulSync) {
            // Build the input data with the dynamic service class name
            Data inputData = new Data.Builder()
                    .putString("serviceClassName", "org.smartregister.sync.intent.SyncIntentService") // fully qualified class name
                    .putBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false) // if needed
                    .build();

            BaseWorker.scheduleJobImmediately(
                    this,
                    SyncServiceWorker.TAG,
                    SyncServiceWorker.class,
                    inputData
            );
        }
    }

    protected boolean processSettings(Intent intent) {
        Log.d("ssssssss", "In Settings Sync Intent Service...");
        boolean isSuccessfulSync = true;
        if (intent != null) {
            try {
                super.onHandleIntent(intent);
                int count = syncSettingsServiceHelper.processIntent();
                if (count > 0) {
                    intent.putExtra(AllConstants.INTENT_KEY.SYNC_TOTAL_RECORDS, count);
                }
            } catch (JSONException e) {
                isSuccessfulSync = false;
                logError(TAG + " Error fetching client settings");
            }
        }
        return isSuccessfulSync;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = CoreLibrary.getInstance().context();
        syncSettingsServiceHelper = new SyncSettingsServiceHelper(context.configuration().dristhiBaseURL(), context.getHttpAgent());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}

