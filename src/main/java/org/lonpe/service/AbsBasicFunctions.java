
package org.lonpe.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.sqlclient.Row;
import io.vertx.rxjava3.sqlclient.Tuple;

public class AbsBasicFunctions {

    protected void fTString(final String k, final JsonObject js, final Tuple t) {
        t.addString(js.getString(k));
    }

    protected void fTBoolean(final String k, final JsonObject js, final Tuple t) {
        t.addBoolean(js.getBoolean(k));
    }

    protected void fTLong(final String k, final JsonObject js, final Tuple t) {
        t.addLong(js.getLong(k));
    }

    protected void fTDouble(final String k, final JsonObject js, final Tuple t) {
        t.addDouble(js.getDouble(k));
    }

    protected void fTFloat(final String k, final JsonObject js, final Tuple t) {
        t.addDouble(js.getDouble(k));
    }

    protected void fTBigDecimal(final String k, final JsonObject js, final Tuple t) {
        t.addBigDecimal(BigDecimal.valueOf(js.getDouble(k)));
    }

    protected void fTString(final String k, final Map<String, Object> obj, final Tuple t) {
        t.addString((String) obj.get(k));
    }

    protected void fTLong(final String k, final Map<String, Object> obj, final Tuple t) {
        t.addLong((Long) obj.get(k));
    }

    protected void fTFloat(final String k, final Map<String, Object> obj, final Tuple t) {
        t.addFloat((Float) obj.get(k));
    }

    protected void fTDouble(final String k, final Map<String, Object> obj, final Tuple t) {
        t.addDouble((Double) obj.get(k));
    }

    protected void fTBigDecimal(final String k, final Map<String, Object> obj, final Tuple t) {
        t.addBigDecimal((BigDecimal) obj.get(k));
    }

    protected void fTBoolean(final String k, final Map<String, Object> obj, final Tuple t) {
        t.addBoolean((Boolean) obj.get(k));
    }

    protected void fTInteger(final String k, final JsonObject js, final Tuple t) {
        fTInteger(k, js, t, null, null);
    }

    protected void fTInteger(final String k, final JsonObject js, final Tuple t, final Integer min, final Integer max) {
        Integer i = js.getInteger(k);
        if (i != null) {
            if (min != null && i < min) {
                i = min;
            }
            if (max != null && i > max) {
                i = max;
            }
        }
        t.addInteger(i);
    }

    protected void fTInteger(final String k, final Map<String, Object> obj, final Tuple t) {
        Integer i = (Integer) obj.get(k);
        t.addInteger(i);
    }

    protected void fTLocalDateTime(final String k, final JsonObject js, final Tuple t) {
        final String val = js.getString(k);
        if (val != null) {
            t.addLocalDateTime(LocalDateTime.parse(js.getString(k)));
        } else {
            t.addLocalDateTime(null);
        }

    }

    protected void fTLocalDateTime(final String k, Map<String, Object> obj, final Tuple t) {
        Object o = obj.get(k);
        if (o != null) {
            t.addLocalDateTime(LocalDateTime.parse(o.toString()));
        } else {
            t.addLocalDateTime(null);
        }

    }

    public void asMaybeNullLocalDate(final Row r, final String name, final JsonArray jsa) {
        LocalDate localDate = r.getLocalDate(name);
        if (localDate != null) {
            jsa.add(localDate.toString());
        } else {
            jsa.add(null);
        }
    }

    // protected void lToCell(final Row r, final XSSFRow row, final String sqlName, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     cell.setCellValue(r.getLong(sqlName));
    // }

    // protected void iToCell(final Row r, final XSSFRow row, final String sqlN, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     cell.setCellValue(r.getInteger(sqlN));
    // }

    // protected void bdToCell(final Row r, final XSSFRow row, final String sqlN, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     cell.setCellValue(r.getBigDecimal(sqlN).doubleValue());
    // }

    // protected void sToCell(final Row r, final XSSFRow row, final String sqlN, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     cell.setCellValue(r.getString(sqlN));
    // }

    // protected void bToCell(final Row r, final XSSFRow row, final String sqlN, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     final Boolean aBoolean = r.getBoolean(sqlN);
    //     final Boolean aBoolean2 = aBoolean != null ? aBoolean : Boolean.FALSE;
    //     cell.setCellValue(aBoolean2);
    // }

    // protected void fToCell(final Row r, final XSSFRow row, final String sqlN, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     final Float f = r.getFloat(sqlN);
    //     if (f != null) {
    //         cell.setCellValue(f);
    //     }
    // }

    // protected void shToCell(final XSSFRow row, final String t, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     cell.setCellValue(t);
    // }

    // protected void ldToCell(final Row r, final XSSFRow row, final String sqlN, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     cell.setCellValue(r.getLocalDate(sqlN));

    // }

    // protected void ldtToCell(final Row r, final XSSFRow row, final String sqlN, int nc) {
    //     final XSSFCell cell = row.createCell(nc);
    //     cell.setCellValue(r.getLocalDateTime(sqlN));
    // }

    protected LocalDate convertDateString(final String d) {
        if (d.trim().length() > 9) {
            if (d.length() > 10) {
                return LocalDate.parse(d.substring(0, 10));
            }
            return LocalDate.parse(d);

        }
        return null;

    }

}
