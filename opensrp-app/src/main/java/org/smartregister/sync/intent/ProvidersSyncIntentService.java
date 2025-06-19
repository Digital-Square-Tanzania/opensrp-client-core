package org.smartregister.sync.intent;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.CoreLibrary;
import org.smartregister.domain.FetchStatus;
import org.smartregister.receiver.SyncStatusBroadcastReceiver;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.ProviderRepository;
import org.smartregister.service.HTTPAgent;
import org.smartregister.util.NetworkUtils;

import timber.log.Timber;

/**
 * Sync service for fetching and saving provider/team member data.
 */
public class ProvidersSyncIntentService extends BaseSyncIntentService {

    private static final String PROVIDERS_URL_PATTERN = "/gateway/api/v1/openmrs/teammember/team/%s";
    private Context context;
    private HTTPAgent httpAgent;

    public ProvidersSyncIntentService() {
        super("ProvidersSyncIntentService");
    }

    public ProvidersSyncIntentService(String name) {
        super(name);
    }

    protected void init(@NonNull Context context) {
        this.context = context;
        httpAgent = CoreLibrary.getInstance().context().getHttpAgent();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init(getBaseContext());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
        handleSync();
    }

    protected void handleSync() {
        sendSyncStatusBroadcastMessage(FetchStatus.fetchStarted);
        doSync();
    }

    private void doSync() {
        if (!NetworkUtils.isNetworkAvailable()) {
            Timber.e("No network connection. Provider sync aborted.");
            complete(FetchStatus.noConnection);
            return;
        }
        try {
            AllSharedPreferences allSharedPreferences = new AllSharedPreferences(getDefaultSharedPreferences(getApplicationContext()));

            String teamId = allSharedPreferences.fetchDefaultTeamId(allSharedPreferences.fetchRegisteredANM());
            if (teamId == null || teamId.isEmpty()) {
                Timber.e("Team ID is missing in sync configuration.");
                complete(FetchStatus.fetchedFailed);
                return;
            }
            String baseUrl = android.net.Uri.parse(getFormattedBaseUrl()).getHost();
            if (android.net.Uri.parse(getFormattedBaseUrl()).getPort() != -1) {
                baseUrl += ":" + android.net.Uri.parse(getFormattedBaseUrl()).getPort();
            }
            String url = baseUrl + String.format(PROVIDERS_URL_PATTERN, teamId);
            Timber.i("Fetching providers from url: %s", url);
            if (httpAgent == null) {
                Timber.e("HTTPAgent is null. Provider sync aborted.");
                complete(FetchStatus.fetchedFailed);
                return;
            }
            org.smartregister.domain.Response resp = httpAgent.fetch(url);
            if (resp == null || resp.isFailure() || resp.payload() == null) {
                Timber.e("Failed to fetch providers. Response: %s", resp != null ? resp.status() : "null");
                complete(FetchStatus.fetchedFailed);
                return;
            }
            JSONArray teamMembers = new JSONArray((String) resp.payload());
            ProviderRepository providerRepository = CoreLibrary.getInstance().context().getProviderRepository();
            int savedCount = 0;
            for (int i = 0; i < teamMembers.length(); i++) {
                JSONObject member = teamMembers.optJSONObject(i);
                if (member != null) {
                    String openMrsUuid = member.optString("uuid", null);
                    String username = member.optString("username", null);
                    String firstName = member.optString("firstName", null);
                    String middleName = member.optString("middleName", null);
                    String lastName = member.optString("lastName", null);
                    String roleName = member.optString("role", null);
                    String teamName = member.optString("team", null);
                    String locationName = member.optString("location", null);
                    String createdAt = member.optString("createdAt", null);
                    providerRepository.addProvider(openMrsUuid, username, firstName, middleName, lastName, roleName, teamName, locationName, createdAt);
                    savedCount++;
                }
            }
            Timber.i("Successfully saved %d providers.", savedCount);
            complete(FetchStatus.fetched);
        } catch (Exception e) {
            Timber.e(e, "Failed to sync providers.");
            complete(FetchStatus.fetchedFailed);
        }
    }

    private void sendSyncStatusBroadcastMessage(FetchStatus fetchStatus) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        sendBroadcast(intent);
    }

    protected void complete(FetchStatus fetchStatus) {
        Intent intent = new Intent();
        intent.setAction(SyncStatusBroadcastReceiver.ACTION_SYNC_STATUS);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_FETCH_STATUS, fetchStatus);
        intent.putExtra(SyncStatusBroadcastReceiver.EXTRA_COMPLETE_STATUS, true);
        sendBroadcast(intent);
    }

    @NonNull
    protected String getFormattedBaseUrl() {
        String baseUrl = CoreLibrary.getInstance().context().
                configuration().dristhiBaseURL();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.lastIndexOf("/"));
        }
        return baseUrl;
    }
}
