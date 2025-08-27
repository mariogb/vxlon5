package org.lonpe.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBLon5 {


 private static final Logger log = LoggerFactory.getLogger(DBLon5.class);

    private Pool pool;

    /**
     * @param config
     */
    public DBLon5(Pool pool) {

        this.pool = pool;

    }

    /**
     * @param sql
     * @param t
     * @return
     */
    public Single<JsonObject> doExecuteForOneResult(String sql, Tuple t) {

        return preparedQuery0(sql, t).map((RowSet<Row> rrs) -> {
            final RowIterator<Row> it = rrs.iterator();
            if (it.hasNext()) {
                final Row next = it.next();
                return next.toJson();
            }
            return new JsonObject();
        });

    }

    /**
     * @param sql select instruction to execute
     * @param tuple a Tuple to apply on respective sql
     * @return a Single RowSet with results
     */
    public Single<RowSet<Row>> doPreparedQuery(final String sql, final Tuple tuple) {
        return pool.preparedQuery(sql).rxExecute(tuple);
        //return client.preparedQuery(sql).rxExecute(tuple);
    }

    /**
     * @param sql select instruction to execute
     * @return a Single RowSet with results
     */
    public Single<RowSet<Row>> doPreparedQuery(final String sql) {
        return pool.preparedQuery(sql).rxExecute();
        //    return client.preparedQuery(sql).rxExecute();
    }

    /**
     * @param sql select instruction to execute
     * @param tuple a Tuple to apply on respective sql
     * @return a Single RowSet with results
     */
    private Single<RowSet<Row>> preparedQuery0(final String sql, final Tuple tuple) {

        return pool.preparedQuery(sql).execute(tuple);

        //return pgConnection.preparedQuery(sql).rxExecute(tuple);
        //return client.preparedQuery(sql).execute(tuple);
    }

    /**
     * @param sql select instruction with two fields: id and pkey
     * @param t a Tuple to apply on respective sql
     * @return a Map with pkey results as keys and ids as values (pkey -> id)
     */
    public Single<Map<String, Long>> doKV(final String sql, final Tuple t) {
        return preparedQuery0(sql, t).map((RowSet<Row> rowSet) -> {
            final Map<String, Long> m = new HashMap<>();
            for (final Row r : rowSet) {
                m.put(r.getString("pkey"), r.getLong("id"));
            }
            return m;
        });
    }

    /**
     * @param sql select instruction to execute
     * @param batch a list of tuples to apply on respective sql
     * @return a Single RowSet with results
     */
    public Single<RowSet<Row>> doBatch(final String sql, final List<Tuple> batch) {
        return pool.preparedQuery(sql).rxExecuteBatch(batch);
        //return client.preparedQuery(sql).executeBatch(batch);
    }

    /**
     * @param sql_delete delete instruction to execute
     * @param id id to apply on respective sql*
     */
    protected void doDelete(final String sql_delete, final Long id) {
        final Tuple tuple = Tuple.of(id);
        pool.preparedQuery(sql_delete).execute(tuple)
                //client.preparedQuery(sql_delete).execute(tuple)
                .subscribe((RowSet<Row> t) -> {
                    log.info("deleted " + id);
                }, (Throwable t) -> {
                    log.error("error with sql delete = {}", sql_delete);

                });

    }
}
