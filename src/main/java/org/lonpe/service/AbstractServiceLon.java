
package org.lonpe.service;

import org.lonpe.model.IDcLon;
import org.lonpe.model.impl.ActAcademicaAlumno;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.functions.Function;
import io.vertx.rxjava3.sqlclient.Row;

public abstract class AbstractServiceLon<T extends IDcLon> extends AbsBasicFunctions {


    public Function<Row, T> rowMapper(){
        return row -> {
            return doFrom(row);
        };
    }

    protected abstract @NonNull T doFrom(Row row);

}