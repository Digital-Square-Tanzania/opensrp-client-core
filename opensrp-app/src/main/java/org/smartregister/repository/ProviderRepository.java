package org.smartregister.repository;

import android.content.ContentValues;

import net.sqlcipher.database.SQLiteDatabase;

/**
 * Repository class for managing Manifest and Provider records in the local SQLite database.
 * <p>
 * This class provides methods to create tables, add, update, retrieve, and delete Manifest records,
 * as well as insert Provider records. It also handles schema migrations for the Manifest table.
 * </p>
 * <p>
 * The Manifest table stores metadata about forms and app versions, while the Provider table stores provider user information.
 * </p>
 *
 * @author cozej4 https://github.com/cozej4
 * @since 2025-06-19
 */
public class ProviderRepository extends BaseRepository {

    protected static final String ID = "id";
    protected static final String CREATED_AT = "created_at";

    // Provider table and columns
    protected static final String PROVIDER_TABLE = "provider";
    protected static final String OPENMRS_UUID = "openmrs_uuid";
    protected static final String USERNAME = "username";
    protected static final String FIRST_NAME = "first_name";
    protected static final String MIDDLE_NAME = "middle_name";
    protected static final String LAST_NAME = "last_name";
    protected static final String ROLE_NAME = "role_name";
    protected static final String TEAM_NAME = "team_name";
    protected static final String LOCATION_NAME = "location_name";


    private static final String CREATE_PROVIDER_TABLE_SQL =
            "CREATE TABLE " + PROVIDER_TABLE + " (" +
            ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            OPENMRS_UUID + " VARCHAR," +
            USERNAME + " VARCHAR," +
            FIRST_NAME + " VARCHAR," +
            MIDDLE_NAME + " VARCHAR," +
            LAST_NAME + " VARCHAR," +
            ROLE_NAME + " VARCHAR," +
            TEAM_NAME + " VARCHAR," +
            LOCATION_NAME + " VARCHAR," +
            CREATED_AT + " VARCHAR NOT NULL)";

    /**
     * Creates the Provider table in the provided SQLite database.
     *
     * @param database The SQLiteDatabase instance on which to execute table creation.
     */
    public static void createTable(SQLiteDatabase database) {
        createProviderTable(database);
    }

    /**
     * Creates the Provider table in the database.
     *
     * @param database The SQLiteDatabase instance.
     */
    private static void createProviderTable(SQLiteDatabase database) {
        database.execSQL(CREATE_PROVIDER_TABLE_SQL);
    }


    /**
     * Inserts a provider record into the provider table.
     *
     * @param openMrsUuid   The OpenMRS UUID of the provider.
     * @param username      The username of the provider.
     * @param firstName     The first name of the provider.
     * @param middleName    The middle name of the provider.
     * @param lastName      The last name of the provider.
     * @param roleName      The role name of the provider.
     * @param teamName      The team name of the provider.
     * @param locationName  The location name of the provider.
     * @param createdAt     The creation date as a String.
     */
    public void addProvider(String openMrsUuid, String username, String firstName, String middleName, String lastName,
                            String roleName, String teamName, String locationName, String createdAt) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(OPENMRS_UUID, openMrsUuid);
        contentValues.put(USERNAME, username);
        contentValues.put(FIRST_NAME, firstName);
        contentValues.put(MIDDLE_NAME, middleName);
        contentValues.put(LAST_NAME, lastName);
        contentValues.put(ROLE_NAME, roleName);
        contentValues.put(TEAM_NAME, teamName);
        contentValues.put(LOCATION_NAME, locationName);
        contentValues.put(CREATED_AT, createdAt);
        getWritableDatabase().insert(PROVIDER_TABLE, null, contentValues);
    }
}