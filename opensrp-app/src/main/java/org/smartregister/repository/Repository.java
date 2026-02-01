package org.smartregister.repository;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.AllConstants;
import org.smartregister.CoreLibrary;
import org.smartregister.commonregistry.CommonFtsObject;
import org.smartregister.exception.DatabaseMigrationException;
import org.smartregister.util.DatabaseMigrationUtils;
import org.smartregister.util.Session;
import org.smartregister.view.activity.DrishtiApplication;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import timber.log.Timber;

public class Repository extends SQLiteOpenHelper {
    protected CommonFtsObject commonFtsObject;
    private DrishtiRepository[] repositories;
    private File databasePath = new File(DrishtiApplication.getAppDir() + "/databases/" + AllConstants.DATABASE_NAME);
    private Context context;
    private String dbName;
    private Session session;

    private final String passphrase;

    private static void loadSqlCipherLib() {
        try {
            System.loadLibrary("sqlcipher");
        } catch (UnsatisfiedLinkError e) {
            Timber.e(e);
        }
    }

    public Repository(Context context, Session session, DrishtiRepository... repositories) {
        this(context,
                session != null ? session.repositoryName() : AllConstants.DATABASE_NAME,
                1,
                session,
                null,
                resolvePassphrase(),
                repositories);
    }

    public Repository(Context context, Session session, CommonFtsObject commonFtsObject,
                      DrishtiRepository... repositories) {
        this(context,
                session != null ? session.repositoryName() : AllConstants.DATABASE_NAME,
                1,
                session,
                commonFtsObject,
                resolvePassphrase(),
                repositories);
    }

    public Repository(Context context, String dbName, int version, Session session,
                      CommonFtsObject commonFtsObject, DrishtiRepository... repositories) {
        this(context, dbName, version, session, commonFtsObject, resolvePassphrase(), repositories);
    }

    private Repository(Context context, String dbName, int version, Session session,
                       CommonFtsObject commonFtsObject, String passphrase, DrishtiRepository... repositories) {
        super(context, dbName, passphrase, null, version, 0, null, null, false);
        this.dbName = dbName;
        this.repositories = repositories;
        this.context = context;
        this.session = session;
        this.commonFtsObject = commonFtsObject;
        this.passphrase = passphrase;
        this.databasePath = context != null ? context.getDatabasePath(dbName)
                : new File("/data/data/org.smartregister" + ".indonesia/databases/" + AllConstants.DATABASE_NAME);

        loadSqlCipherLib();
        for (DrishtiRepository repository : repositories) {
            repository.updateMasterRepository(this);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        for (DrishtiRepository repository : repositories) {
            repository.onCreate(database);
        }

        if (this.commonFtsObject != null) {
            for (String ftsTable : commonFtsObject.getTables()) {
                Set<String> searchColumns = new LinkedHashSet<String>();
                searchColumns.add(CommonFtsObject.idColumn);
                searchColumns.add(CommonFtsObject.relationalIdColumn);
                searchColumns.add(CommonFtsObject.phraseColumn);
                searchColumns.add(CommonFtsObject.isClosedColumn);

                String[] mainConditions = this.commonFtsObject.getMainConditions(ftsTable);
                if (mainConditions != null) {
                    for (String mainCondition : mainConditions) {
                        if (!mainCondition.equals(CommonFtsObject.isClosedColumnName)) {
                            searchColumns.add(mainCondition);
                        }
                    }
                }

                String[] sortFields = this.commonFtsObject.getSortFields(ftsTable);
                if (sortFields != null) {
                    for (String sortValue : sortFields) {
                        if (sortValue.startsWith("alerts.")) {
                            sortValue = sortValue.split("\\.")[1];
                        }
                        searchColumns.add(sortValue);
                    }
                }

                String joinedSearchColumns = StringUtils.join(searchColumns, ",");

                String searchSql =
                        "create virtual table " + CommonFtsObject.searchTableName(ftsTable)
                                + " using fts4 (" + joinedSearchColumns + ");";
                database.execSQL(searchSql);

            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase database) {
        super.onOpen(database);
        if (!DatabaseMigrationUtils.performCipherMigrationToV4(database)) {
            throw new DatabaseMigrationException("Database migration to SQLiteCipher v4 was not successful");
        }

        if (!CoreLibrary.getInstance().context().allSharedPreferences().isMigratedToSqlite4()) {
            CoreLibrary.getInstance().context().allSharedPreferences().setMigratedToSqlite4();
            Timber.i("Database migration to Cipher 4 complete");
        }

        // Disable cipher memory security which makes database operations slow
        database.execSQL("PRAGMA cipher_memory_security = OFF;");
        // set journal mode to TRUNCATE
        database.rawExecSQL("PRAGMA journal_mode = TRUNCATE;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if (passphrase == null || passphrase.isEmpty()) {
            throw new RuntimeException("Password has not been set!");
        }
        return super.getReadableDatabase();
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if (passphrase == null || passphrase.isEmpty()) {
            throw new RuntimeException("Password has not been set!");
        }
        return super.getWritableDatabase();
    }

    private boolean isDatabaseWritable(String password) {
        SQLiteDatabase database = SQLiteDatabase
                .openDatabase(databasePath.getPath(), password, null,
                        SQLiteDatabase.OPEN_READONLY, null);
        database.close();
        return true;
    }

    public boolean canUseThisPassword(String password) {
        try {
            return isDatabaseWritable(password);
        } catch (SQLiteException e) {
            Timber.e(e);
            if (e.getMessage().contains("attempt to write a readonly database")) {
                File journal = new File(databasePath.getPath() + "-journal");
                Timber.w("Journal exists: %s", journal.exists());
                if (journal.exists() && journal.canWrite()) {
                    Timber.w("Journal space: %s, its not possible to recover transactions!!! deleting Journal ", journal.getTotalSpace());
                    try {
                        new FileOutputStream(journal).write(new byte[]{});
                        return isDatabaseWritable(password);
                    } catch (FileNotFoundException e1) {
                        Timber.e(e1);
                    } catch (IOException e1) {
                        Timber.e(e1);
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String resolvePassphrase() {
        DrishtiApplication instance = DrishtiApplication.getInstance();
        if (instance == null) {
            return "";
        }
        String password = instance.getPassword();
        return password != null ? password : "";
    }

    public boolean deleteRepository() {
        close();
        boolean deleteTry1 = context.deleteDatabase(dbName);
        boolean deleteTry2 = context.getDatabasePath(dbName).delete();

        return deleteTry1 || deleteTry2;
    }

}
