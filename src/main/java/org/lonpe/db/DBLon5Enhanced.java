package org.lonpe.db;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Function;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Pool;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.RowIterator;
import io.vertx.rxjava3.sqlclient.RowSet;
import io.vertx.rxjava3.sqlclient.SqlConnection;
import io.vertx.rxjava3.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class DBLon5Enhanced {

    private static final Logger log = LoggerFactory.getLogger(DBLon5Enhanced.class);
    private static final Logger metricsLog = LoggerFactory.getLogger("db.metrics");
    
    private final Pool pool;
    private final DBConfig config;
    private final DBMetrics metrics;

    public static class DBConfig {
        private final Duration queryTimeout;
        private final int maxRetries;
        private final Duration retryDelay;
        private final boolean enableMetrics;
        private final boolean enableStatementLogging;

        public DBConfig() {
            this(Duration.ofSeconds(30), 3, Duration.ofMillis(100), true, false);
        }

        public DBConfig(Duration queryTimeout, int maxRetries, Duration retryDelay, 
                       boolean enableMetrics, boolean enableStatementLogging) {
            this.queryTimeout = queryTimeout;
            this.maxRetries = maxRetries;
            this.retryDelay = retryDelay;
            this.enableMetrics = enableMetrics;
            this.enableStatementLogging = enableStatementLogging;
        }

        // Getters
        public Duration getQueryTimeout() { return queryTimeout; }
        public int getMaxRetries() { return maxRetries; }
        public Duration getRetryDelay() { return retryDelay; }
        public boolean isEnableMetrics() { return enableMetrics; }
        public boolean isEnableStatementLogging() { return enableStatementLogging; }
    }

    public static class DBMetrics {
        private volatile long totalQueries = 0;
        private volatile long successfulQueries = 0;
        private volatile long failedQueries = 0;
        private volatile long totalExecutionTime = 0;

        public void recordQuery(long executionTimeMs, boolean success) {
            totalQueries++;
            totalExecutionTime += executionTimeMs;
            if (success) {
                successfulQueries++;
            } else {
                failedQueries++;
            }
        }

        public double getAverageExecutionTime() {
            return totalQueries > 0 ? (double) totalExecutionTime / totalQueries : 0;
        }

        public double getSuccessRate() {
            return totalQueries > 0 ? (double) successfulQueries / totalQueries : 0;
        }

        // Getters
        public long getTotalQueries() { return totalQueries; }
        public long getSuccessfulQueries() { return successfulQueries; }
        public long getFailedQueries() { return failedQueries; }
    }

    public static class DBException extends RuntimeException {
        private final String sql;
        private final Tuple parameters;

        public DBException(String message, String sql, Tuple parameters, Throwable cause) {
            super(message, cause);
            this.sql = sql;
            this.parameters = parameters;
        }

        public String getSql() { return sql; }
        public Tuple getParameters() { return parameters; }
    }

    public DBLon5Enhanced(Pool pool) {
        this(pool, new DBConfig());
    }

    public DBLon5Enhanced(Pool pool, DBConfig config) {
        this.pool = pool;
        this.config = config;
        this.metrics = new DBMetrics();
    }

    // Enhanced query execution with metrics and error handling
    private <T> Single<T> executeWithMetrics(String sql, Tuple tuple, 
                                           Function<RowSet<Row>, T> mapper) {
        return Single.fromCallable(() -> {
            validateInput(sql, tuple);
            return tuple;
        })
        .flatMap(validatedTuple -> {
            final long startTime = System.currentTimeMillis();
            final String queryId = generateQueryId();
            
            MDC.put("queryId", queryId);
            MDC.put("sql", config.isEnableStatementLogging() ? sql : "[HIDDEN]");
            
            return pool.preparedQuery(sql)
                    .rxExecute(validatedTuple)
                    .timeout(config.getQueryTimeout().toMillis(), TimeUnit.MILLISECONDS)
                    .map(mapper)
                    .doOnSuccess(result -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        recordMetrics(sql, executionTime, true);
                        log.debug("Query executed successfully in {}ms", executionTime);
                    })
                    .doOnError(throwable -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        recordMetrics(sql, executionTime, false);
                        log.error("Query failed after {}ms: {}", executionTime, throwable.getMessage());
                    })
                    .onErrorResumeNext(throwable -> {
                        return Single.error(new DBException("Query execution failed", sql, validatedTuple, throwable));
                    })
                    .doFinally(() -> MDC.clear())
                    .retry(config.getMaxRetries())
                    .delaySubscription(config.getRetryDelay().toMillis(), TimeUnit.MILLISECONDS);
        });
    }
    

    // Enhanced single result query
    public Single<Optional<JsonObject>> findOne(String sql, Tuple tuple) {
        return executeWithMetrics(sql, tuple, rowSet -> {
            RowIterator<Row> iterator = rowSet.iterator();
            return iterator.hasNext() ? Optional.of(iterator.next().toJson()) : Optional.empty();
        });
    }

    public Single<JsonObject> findOneOrEmpty(String sql, Tuple tuple) {
        return findOne(sql, tuple).map(opt -> opt.orElse(new JsonObject()));
    }

    // Enhanced single result query with custom mapper
    public <T> Single<Optional<T>> findOne(String sql, Tuple tuple, Function<Row, T> mapper) {
        return executeWithMetrics(sql, tuple, rowSet -> {
            RowIterator<Row> iterator = rowSet.iterator();
            return iterator.hasNext() ? Optional.of(mapper.apply(iterator.next())) : Optional.empty();
        });
    }

    // Enhanced list query
    public Single<List<JsonObject>> findList(String sql, Tuple tuple) {
        return executeWithMetrics(sql, tuple, rowSet -> {
            return rowSet.stream()
                    .map(Row::toJson)
                    .collect(java.util.stream.Collectors.toList());
        });
    }

    public <T> Single<List<T>> findList(String sql, Tuple tuple, Function<Row, T> rowMapper) {
        return executeWithMetrics(sql, tuple, rowSet -> {
            return rowSet.stream()
                    .map(row -> {
                        try {
                            return rowMapper.apply(row);
                        } catch (Throwable e) {
                            throw new RuntimeException("Error mapping row", e);
                        }
                    })
                    .collect(Collectors.toList());
        });
    }

    // Observable for streaming large result sets
    public Observable<JsonObject> findStream(String sql, Tuple tuple) {
        return executeWithMetrics(sql, tuple, rowSet -> rowSet)
                .flatMapObservable(rowSet -> Observable.fromIterable(rowSet))
                .map(Row::toJson);
    }

    public <T> Observable<T> findStream(String sql, Tuple tuple, Function<Row, T> rowMapper) {
        return executeWithMetrics(sql, tuple, rowSet -> rowSet)
                .flatMapObservable(rowSet -> Observable.fromIterable(rowSet))
                .map(rowMapper);
    }

    // Enhanced key-value mapping
    public Single<Map<String, Long>> findKeyValueMap(String sql, Tuple tuple) {
        return findKeyValueMap(sql, tuple, "pkey", "id");
    }

    public Single<Map<String, Long>> findKeyValueMap(String sql, Tuple tuple, 
                                                   String keyColumn, String valueColumn) {
        return executeWithMetrics(sql, tuple, rowSet -> {
            Map<String, Long> map = new HashMap<>();
            for (Row row : rowSet) {
                String key = row.getString(keyColumn);
                Long value = row.getLong(valueColumn);
                if (key != null && value != null) {
                    map.put(key, value);
                }
            }
            return map;
        });
    }

    // Enhanced batch operations
    public Single<Integer> executeBatch(String sql, List<Tuple> batch) {
        if (batch.isEmpty()) {
            return Single.just(0);
        }

        return Single.defer(() -> {
            validateInput(sql, null);
            final long startTime = System.currentTimeMillis();
            
            return pool.preparedQuery(sql)
                    .rxExecuteBatch(batch)
                    .timeout(config.getQueryTimeout().toMillis() * batch.size(), TimeUnit.MILLISECONDS)
                    .map(RowSet::rowCount)
                    .doOnSuccess(rowCount -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        recordMetrics(sql + " [BATCH]", executionTime, true);
                        log.info("Batch executed successfully: {} rows affected in {}ms", rowCount, executionTime);
                    })
                    .doOnError(throwable -> {
                        long executionTime = System.currentTimeMillis() - startTime;
                        recordMetrics(sql + " [BATCH]", executionTime, false);
                        log.error("Batch execution failed after {}ms", executionTime, throwable);
                    })
                    .onErrorResumeNext(throwable -> 
                        Single.error(new DBException("Batch execution failed", sql, null, throwable))
                    );
        });
    }

    // Enhanced delete operation
    public Single<Boolean> delete(String sql, Long id) {
        return executeWithMetrics(sql, Tuple.of(id), rowSet -> rowSet.rowCount() > 0)
                .doOnSuccess(deleted -> {
                    if (deleted) {
                        log.info("Successfully deleted record with id: {}", id);
                    } else {
                        log.warn("No record found to delete with id: {}", id);
                    }
                });
    }

    // Enhanced insert operation
    public Single<Long> insert(String sql, Tuple tuple) {
        return executeWithMetrics(sql, tuple, rowSet -> {
            RowIterator<Row> iterator = rowSet.iterator();
            if (iterator.hasNext()) {
                Row row = iterator.next();
                return row.getLong("id"); // Assuming RETURNING id
            }
            throw new IllegalStateException("Insert operation did not return an ID");
        });
    }

    // Enhanced update operation
    public Single<Integer> update(String sql, Tuple tuple) {
        return executeWithMetrics(sql, tuple, RowSet::rowCount);
    }

    // Transaction support
    public <T> Single<T> inTransaction(Function<SqlConnection, Single<T>> operation) {
        return pool.rxGetConnection()
                .flatMap(connection -> {
                    return connection.rxBegin()
                            .flatMap(transaction -> {
                                return operation.apply(connection)
                                        .flatMap(result -> {
                                            return transaction.rxCommit()
                                                    .andThen(Single.just(result));
                                        })
                                        .onErrorResumeNext(error -> {
                                            return transaction.rxRollback()
                                                    .andThen(Single.error(error));
                                        });
                            })
                            .doFinally(connection::close);
                })
                .doOnSuccess(result -> log.debug("Transaction completed successfully"))
                .doOnError(error -> log.error("Transaction failed", error));
    }

    // Health check
    public Single<Boolean> healthCheck() {
        return executeWithMetrics("SELECT 1", Tuple.tuple(), rowSet -> true)
                .onErrorReturn(throwable -> false);
    }

    // Metrics access
    public DBMetrics getMetrics() {
        return metrics;
    }

    // Utility methods
    private void validateInput(String sql, Tuple tuple) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL statement cannot be null or empty");
        }
    }

    private String generateQueryId() {
        return String.valueOf(System.nanoTime());
    }

    private void recordMetrics(String sql, long executionTime, boolean success) {
        if (config.isEnableMetrics()) {
            metrics.recordQuery(executionTime, success);
            
            if (metricsLog.isInfoEnabled()) {
                metricsLog.info("SQL: {} | Time: {}ms | Success: {}", 
                    sql.length() > 100 ? sql.substring(0, 100) + "..." : sql,
                    executionTime, success);
            }
        }
    }

    // Graceful shutdown
    public Completable close() {
        log.info("Closing database connection pool...");
        log.info("Final metrics - Total queries: {}, Success rate: {:.2f}%, Avg time: {:.2f}ms",
                metrics.getTotalQueries(), metrics.getSuccessRate() * 100, metrics.getAverageExecutionTime());
        
        return pool.rxClose()
                .doOnComplete(() -> log.info("Database connection pool closed successfully"))
                .doOnError(error -> log.error("Error closing database connection pool", error));
    }

    // Builder pattern for easier configuration
    public static class Builder {
        private Pool pool;
        private DBConfig config = new DBConfig();

        public Builder pool(Pool pool) {
            this.pool = pool;
            return this;
        }

        public Builder queryTimeout(Duration timeout) {
            this.config = new DBConfig(timeout, config.getMaxRetries(), config.getRetryDelay(),
                    config.isEnableMetrics(), config.isEnableStatementLogging());
            return this;
        }

        public Builder maxRetries(int retries) {
            this.config = new DBConfig(config.getQueryTimeout(), retries, config.getRetryDelay(),
                    config.isEnableMetrics(), config.isEnableStatementLogging());
            return this;
        }

        public Builder enableMetrics(boolean enable) {
            this.config = new DBConfig(config.getQueryTimeout(), config.getMaxRetries(), config.getRetryDelay(),
                    enable, config.isEnableStatementLogging());
            return this;
        }

        public Builder enableStatementLogging(boolean enable) {
            this.config = new DBConfig(config.getQueryTimeout(), config.getMaxRetries(), config.getRetryDelay(),
                    config.isEnableMetrics(), enable);
            return this;
        }

        public DBLon5Enhanced build() {
            if (pool == null) {
                throw new IllegalStateException("Pool is required");
            }
            return new DBLon5Enhanced(pool, config);
        }
    }
}