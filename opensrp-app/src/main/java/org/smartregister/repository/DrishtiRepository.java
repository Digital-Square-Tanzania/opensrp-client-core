package org.smartregister.repository;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

public abstract class DrishtiRepository {
    protected Repository masterRepository;

    public void updateMasterRepository(Repository repository) {
        this.masterRepository = repository;
    }

    abstract protected void onCreate(SQLiteDatabase database);

}
