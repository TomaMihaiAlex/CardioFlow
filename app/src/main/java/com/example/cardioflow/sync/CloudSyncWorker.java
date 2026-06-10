package com.example.cardioflow.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.cardioflow.database.DatabaseManager;
import com.example.cardioflow.services.CloudSync;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Golește coada `offline_requests` în Firestore.
 *
 * Rulează pe thread de fundal (WorkManager), deci putem aștepta sincron finalizarea
 * scrierilor Firestore cu {@link Tasks#await}. Fiecare rând trimis cu succes este șters;
 * dacă Firebase încă nu e configurat sau o scriere eșuează, întoarcem retry() iar
 * WorkManager reîncearcă conform politicii de backoff.
 */
public class CloudSyncWorker extends Worker {
    private static final String TAG = "CloudSyncWorker";
    private static final long WRITE_TIMEOUT_SEC = 15;
    private static final int MAX_RETRY = 5;

    public CloudSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        DatabaseManager dbm = DatabaseManager.getInstance(getApplicationContext());
        List<DatabaseManager.OfflineRequest> pending = dbm.getOfflineRequests();
        if (pending.isEmpty()) {
            return Result.success();
        }

        Log.d(TAG, "Golesc coada offline: " + pending.size() + " cereri");
        boolean retryNeeded = false;

        for (DatabaseManager.OfflineRequest req : pending) {
            CollectionReference ref = CloudSync.collectionFor(req.collection);
            if (ref == null) {
                // Firebase încă neconfigurat → reîncercăm tot pachetul mai târziu.
                Log.w(TAG, "Firestore indisponibil, amân sincronizarea");
                return Result.retry();
            }

            try {
                Map<String, Object> data = CloudSync.buildDocument(req.type, req.payload);
                Tasks.await(ref.add(data), WRITE_TIMEOUT_SEC, TimeUnit.SECONDS);
                dbm.deleteOfflineRequest(req.id);
                Log.d(TAG, "Sincronizat și șters din coadă: id=" + req.id);
            } catch (Exception e) {
                Log.e(TAG, "Eșec sincronizare id=" + req.id, e);
                dbm.incrementOfflineRetry(req.id);
                if (req.retryCount + 1 >= MAX_RETRY) {
                    // Renunțăm la cererile cronic eșuate ca să nu blocăm coada la nesfârșit.
                    Log.w(TAG, "Depășit MAX_RETRY, șterg id=" + req.id);
                    dbm.deleteOfflineRequest(req.id);
                } else {
                    retryNeeded = true;
                }
            }
        }

        return retryNeeded ? Result.retry() : Result.success();
    }
}
