package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import java.util.function.Function;

public class RepositoryBase {
    public <T> T getOne(ResultSet resultSet, Function<Row, T> mapper) {
        Row row = resultSet.one();

        if(row == null){
            return null;
        }

        return mapper.apply(row);
    }
}
