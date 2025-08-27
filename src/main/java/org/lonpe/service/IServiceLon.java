
package org.lonpe.service;

import org.lonpe.model.IDcLon;


import io.reactivex.rxjava3.functions.Function;
import io.vertx.rxjava3.sqlclient.Row;

public interface IServiceLon<T extends IDcLon> {
    T doFrom(final Row r);

    Function<Row, T> rowMapper();


    String getSqlFullString();


}