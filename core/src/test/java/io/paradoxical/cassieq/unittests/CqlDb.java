package io.paradoxical.cassieq.unittests;

import com.datastax.driver.core.Session;
import io.paradoxical.cassandra.loader.db.CqlUnitDb;

public class CqlDb {
    public static Session create() throws Exception {
        return CqlUnitDb.create("../db/scripts");
    }

    public static Session createFresh() throws Exception {
        return CqlUnitDb.reset("../db/scripts");
    }
}