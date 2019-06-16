package com.ridgid.oss.orm.jpa.helper;

import com.ridgid.oss.common.helper.FieldReflectionHelpers;

import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ridgid.oss.common.helper.FieldReflectionHelpers.getFieldValueOrThrowRuntimeException;

/**
 *
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class JPANativeQueryHelpers {

    private JPANativeQueryHelpers() {
    }

    /**
     * @param tableName
     * @param primaryKeyColumnNames
     * @param primaryKeyFieldNames
     * @param entityColumnNames
     * @param entityFieldNames
     * @return
     */
    public static String createNativeInsertQueryStringFrom(String tableName,
                                                           List<String> primaryKeyColumnNames,
                                                           List<String> primaryKeyFieldNames,
                                                           List<String> entityColumnNames,
                                                           List<String> entityFieldNames) {
        return createNativeInsertQueryStringFrom
                (
                        null,
                        tableName,
                        primaryKeyColumnNames,
                        primaryKeyFieldNames,
                        entityColumnNames,
                        entityFieldNames
                );
    }

    public static String createNativeInsertQueryStringFrom(String schemaName,
                                                           String tableName,
                                                           List<String> primaryKeyColumnNames,
                                                           List<String> primaryKeyFieldNames,
                                                           List<String> entityColumnNames,
                                                           List<String> entityFieldNames) {
        Objects.requireNonNull(tableName, "tableName must be non-null");
        if (primaryKeyColumnNames.size() != primaryKeyFieldNames.size())
            throw new RuntimeException("primary key column names and primary key field names must match in number");
        if (entityColumnNames.size() != entityFieldNames.size())
            throw new RuntimeException("entity column names and entity field names must match in number");
        String schemaPart = schemaName == null ? "" : "\"" + schemaName + "\".";
        String fieldsPart
                = Stream.concat(primaryKeyColumnNames.stream(), entityColumnNames.stream())
                .map(cn -> "\"" + cn + "\"")
                .collect(Collectors.joining(","));
        String valuesPart
                = Stream.concat(primaryKeyColumnNames.stream(), entityColumnNames.stream())
                .map(fn -> "?")
                .collect(Collectors.joining(","));
        return String.format
                (
                        "insert into %s\"%s\" ( %s ) values ( %s )",
                        schemaPart,
                        tableName,
                        fieldsPart,
                        valuesPart
                );
    }

    /**
     * @param q
     * @param offset
     * @param fieldNames
     * @param obj
     */
    public static void setInsertQueryColumnValues(Query q,
                                                  Object obj,
                                                  int offset,
                                                  List<String> fieldNames) {
        for (int i = 0; i < fieldNames.size(); i++) {
            Field f = FieldReflectionHelpers.getFieldOrThrowRuntimeException(obj.getClass(), fieldNames.get(i));
            if (f.isAnnotationPresent(Convert.class))
                setConvertedParameterValue(q, obj, offset, i, f);
            else
                setBasicParameterValue(q, obj, offset, i, f);
        }
    }

    private static void setConvertedParameterValue(Query q,
                                                   Object obj,
                                                   int offset,
                                                   int i,
                                                   Field f) {
        try {
            @SuppressWarnings("unchecked")
            AttributeConverter converter
                    = (AttributeConverter) f
                    .getAnnotation(Convert.class)
                    .converter()
                    .getConstructor()
                    .newInstance();
            //noinspection unchecked
            q.setParameter
                    (
                            i + offset + 1,
                            converter.convertToDatabaseColumn(getFieldValueOrThrowRuntimeException(obj, f))
                    );
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setBasicParameterValue(Query q,
                                               Object obj,
                                               int offset,
                                               int i,
                                               Field f) {
        Class<?> ft = f.getType();
        if (ft == Calendar.class)
            setCalendarParameterValue(q, obj, i + offset + 1, f);
        else if (ft == Date.class)
            setDateParameterValue(q, obj, i + offset + 1, f);
        else
            q.setParameter(i + offset + 1, getFieldValueOrThrowRuntimeException(obj, f));
    }

    /**
     * @param query
     * @param obj
     * @param idx
     * @param field
     */
    public static void setDateParameterValue(Query query,
                                             Object obj,
                                             int idx,
                                             Field field) {
        TemporalType tt = JPAFieldReflectionHelpers.getJPATemporalTypeForAmbiguousTemporalField(field);
        query.setParameter(idx, (Date) getFieldValueOrThrowRuntimeException(obj, field), tt);
    }

    /**
     * @param query
     * @param obj
     * @param idx
     * @param field
     */
    public static void setCalendarParameterValue(Query query,
                                                 Object obj,
                                                 int idx,
                                                 Field field) {
        TemporalType tt = JPAFieldReflectionHelpers.getJPATemporalTypeForAmbiguousTemporalField(field);
        query.setParameter(idx, (Calendar) getFieldValueOrThrowRuntimeException(obj, field), tt);
    }

    /**
     * @param tableName
     * @return
     */
    public static String createNativeDeleteQueryStringFrom(String tableName) {
        return "delete from \"" + tableName + "\"";
    }

    /**
     * @param schemaName
     * @param tableName
     * @return
     */
    public static String createNativeDeleteQueryStringFrom(String schemaName,
                                                           String tableName) {
        if (schemaName == null
                || schemaName.isEmpty()
                || schemaName.matches(" +")
                || schemaName.trim().isEmpty())
            return createNativeDeleteQueryStringFrom(tableName);
        return "delete from \"" + schemaName + "\".\"" + tableName + "\"";
    }
}