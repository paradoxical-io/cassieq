package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Session;
import io.paradoxical.cassandra.loader.db.CqlUnitDb;

public class CqlDb {
    public static Session create() throws Exception {
        return CqlUnitDb.create("../db/scripts");
    }

    public static Session createFresh() throws Exception {
        return CqlUnitDb.unCached("../db/scripts");
    }
}