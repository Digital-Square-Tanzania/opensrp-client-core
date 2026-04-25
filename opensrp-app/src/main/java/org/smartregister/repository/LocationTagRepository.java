package org.smartregister.repository;

import android.content.ContentValues;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.domain.LocationProperty;
import org.smartregister.domain.LocationTag;
import org.smartregister.util.PropertiesConverter;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Created by ilakozejumanne on 01/29/20.
 */
public class LocationTagRepository extends BaseRepository {

    protected static final String NAME = "name";
    protected static final String LOCATION_ID = "location_id";
    protected static final String LOCATION_TAG_TABLE = "location_tag";
    protected static final String[] COLUMNS = new String[]{NAME,LOCATION_ID};
    private static final String CREATE_LOCATION_TAG_TABLE =
            "CREATE TABLE " + LOCATION_TAG_TABLE + " (" +
                    NAME + " VARCHAR NOT NULL, " +
                    LOCATION_ID + " VARCHAR NOT NULL, " +
                    "PRIMARY KEY ("+NAME+", "+LOCATION_ID+")) ";

    protected static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HHmm")
            .registerTypeAdapter(LocationProperty.class, new PropertiesConverter()).create();

    public static void createTable(SQLiteDatabase database) {
        database.execSQL(CREATE_LOCATION_TAG_TABLE);
    }

    protected String getLocationTagTableName() {
        return LOCATION_TAG_TABLE;
    }

    /**
     * this method is used to save/update locationTags
     * @param locationTag to be saved or updated if it already exists.
     */
    public void addOrUpdate(LocationTag locationTag) {
        if (StringUtils.isBlank(locationTag.getLocationId()))
            throw new IllegalArgumentException("location id not provided");
        ContentValues contentValues = new ContentValues();
        contentValues.put(NAME, locationTag.getName());
        contentValues.put(LOCATION_ID, locationTag.getLocationId());
        getWritableDatabase().replace(getLocationTagTableName(), null, contentValues);

    }

    /**
     * this method is used to delete all location tags for a location
     * @param locationId id of the location whose tags should be deleted.
     */
    public void deleteLocationTagsByLocationId(String locationId) {
        if (StringUtils.isBlank(locationId))
            throw new IllegalArgumentException("location id not provided");
        getWritableDatabase().delete(getLocationTagTableName(), LOCATION_ID + " = ?", new String[]{locationId});
    }

    /**
     *  this method returns a list of all location tags stored
     * @return a list of all location tags stored
     */
    public List<LocationTag> getAllLocationTags() {
        Cursor cursor = null;
        List<LocationTag> locationsTags = new ArrayList<>();
        try {
            cursor = getReadableDatabase().rawQuery("SELECT * FROM " + getLocationTagTableName(), null);
            while (cursor.moveToNext()) {
                locationsTags.add(readCursor(cursor));
            }
            cursor.close();
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return locationsTags;
    }

    /**
     * Get a list of location tags for the passed locationId
     * @param id of a location to obtain it's tags
     * @return a list of tags for the passed location
     */
    public List<LocationTag> getLocationTagByLocationId(String id) {
        List<LocationTag> locationsTags = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + getLocationTagTableName() +
                " WHERE " + LOCATION_ID + " =?", new String[]{id})) {
            while (cursor.moveToNext()) {
                locationsTags.add(readCursor(cursor));
            }
        } catch (Exception e) {
            Timber.e(e);
        }
        return locationsTags;

    }

    protected LocationTag readCursor(Cursor cursor) {
        LocationTag locationTag = new LocationTag();
        locationTag.setName(cursor.getString(cursor.getColumnIndex(NAME)));
        locationTag.setLocationId(cursor.getString(cursor.getColumnIndex(LOCATION_ID)));
        return locationTag;
    }

}
