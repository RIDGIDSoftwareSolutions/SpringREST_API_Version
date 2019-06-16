package com.ridgid.oss.common.helper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.ridgid.oss.common.helper.ExceptionHelpers.throwAsRuntimeExceptionUnableToSetField;
import static com.ridgid.oss.common.helper.TemporalType.*;

/**
 *
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class FieldPopulationHelpers {

    private FieldPopulationHelpers() {
    }

    /**
     * @param idx
     * @param obj
     */
    public static void deterministicallyPopulateBaseFields(int idx,
                                                           Object obj) {
        deterministicallyPopulateBaseFields
                (
                        idx,
                        obj,
                        FieldPopulationHelpers::defaultFieldExclusionPredicate,
                        FieldPopulationHelpers::defaultPopulateCompositePredicate,
                        FieldPopulationHelpers::defaultAmbiguousTemporalTypeMapper,
                        FieldPopulationHelpers::defaulLengthOrScaleMapper
                );
    }

    /**
     * @param idx
     * @param obj
     * @param ambiguousTemporalTypeMapper
     */
    public static void deterministicallyPopulateBaseFields(int idx,
                                                           Object obj,
                                                           BiPredicate<Field, Class<?>> populateCompositePredicate,
                                                           Function<Field, TemporalType> ambiguousTemporalTypeMapper,
                                                           Function<Field, Integer> lengthOrScaleMapper) {
        deterministicallyPopulateBaseFields
                (
                        idx,
                        obj,
                        FieldPopulationHelpers::defaultFieldExclusionPredicate,
                        populateCompositePredicate,
                        ambiguousTemporalTypeMapper,
                        lengthOrScaleMapper
                );
    }

    /**
     * @param idx
     * @param obj
     * @param fieldExclusionPredicate
     * @param ambiguousTemporalTypeMapper
     */
    public static void deterministicallyPopulateBaseFields(int idx,
                                                           Object obj,
                                                           Predicate<Field> fieldExclusionPredicate,
                                                           BiPredicate<Field, Class<?>> populateCompositePredicate,
                                                           Function<Field, TemporalType> ambiguousTemporalTypeMapper,
                                                           Function<Field, Integer> lengthOrScaleMapper) {
        int fieldIdx = 0;
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (deterministicallyPopulateBaseField
                    (
                            obj,
                            field,
                            idx,
                            fieldIdx,
                            fieldExclusionPredicate,
                            populateCompositePredicate,
                            ambiguousTemporalTypeMapper,
                            lengthOrScaleMapper
                    )
            ) fieldIdx++;
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @return
     */
    public static boolean deterministicallyPopulateBaseField(Object obj,
                                                             Field field,
                                                             int idx,
                                                             int fieldIdx) {
        return deterministicallyPopulateBaseField
                (
                        obj,
                        field,
                        idx,
                        fieldIdx,
                        FieldPopulationHelpers::defaultFieldExclusionPredicate,
                        FieldPopulationHelpers::defaultPopulateCompositePredicate,
                        FieldPopulationHelpers::defaultAmbiguousTemporalTypeMapper,
                        FieldPopulationHelpers::defaulLengthOrScaleMapper
                );
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param ambiguousTemporalTypeMapper
     * @return
     */
    public static boolean deterministicallyPopulateBaseField(Object obj,
                                                             Field field,
                                                             int idx,
                                                             int fieldIdx,
                                                             BiPredicate<Field, Class<?>> populateCompositePredicate,
                                                             Function<Field, TemporalType> ambiguousTemporalTypeMapper,
                                                             Function<Field, Integer> lengthOrScaleMapper) {
        return deterministicallyPopulateBaseField
                (
                        obj,
                        field,
                        idx,
                        fieldIdx,
                        FieldPopulationHelpers::defaultFieldExclusionPredicate,
                        populateCompositePredicate,
                        ambiguousTemporalTypeMapper,
                        lengthOrScaleMapper
                );
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param ambiguousTemporalTypeMapper
     * @return
     */
    public static boolean deterministicallyPopulateBaseField(Object obj,
                                                             Field field,
                                                             int idx,
                                                             int fieldIdx,
                                                             Predicate<Field> fieldExclusionPredicate,
                                                             BiPredicate<Field, Class<?>> populateCompositePredicate,
                                                             Function<Field, TemporalType> ambiguousTemporalTypeMapper,
                                                             Function<Field, Integer> lengthOrScaleMapper) {
        if (!field.isAccessible()) field.setAccessible(true);
        if (fieldExclusionPredicate.test(field)) return false;
        Class<?> ft = field.getType();
        if (ft.isEnum())
            deterministicallyPopulateEnumField(obj, field, ft, idx, fieldIdx);
            // Integral Numeric Primitive & Primitive Wrapper Types
        else if (ft.equals(Byte.class) || ft.equals(Byte.TYPE))
            deterministicallyPopulateByteField(obj, field, idx, fieldIdx);
        else if (ft.equals(Short.class) || ft.equals(Short.TYPE))
            deterministicallyPopulateShortField(obj, field, idx, fieldIdx);
        else if (ft.equals(Integer.class) || ft.equals(Integer.TYPE))
            deterministicallyPopulateIntegerField(obj, field, idx, fieldIdx);
        else if (ft.equals(Long.class) || ft.equals(Long.TYPE))
            deterministicallyPopulateLongField(obj, field, idx, fieldIdx);
        else if (ft.equals(BigInteger.class))
            deterministicallyPopulateBigIntegerField(obj, field, idx, fieldIdx);
            // Floating-Point & Decimal Numeric Primitive & Primitive Wrapper Types
        else if (ft.equals(Float.class) || ft.equals(Float.TYPE))
            deterministicallyPopulateFloatField(obj, field, idx, fieldIdx);
        else if (ft.equals(Double.class) || ft.equals(Double.TYPE))
            deterministicallyPopulateDoubleField(obj, field, idx, fieldIdx);
        else if (ft.equals(BigDecimal.class))
            deterministicallyPopulateBigDecimalField(obj, field, idx, fieldIdx, lengthOrScaleMapper);
            // Boolean Primitive and Primitive Wrapper Types
        else if (ft.equals(Boolean.class) || ft.equals(Boolean.TYPE))
            deterministicallyPopulateBooleanField(obj, field, idx, fieldIdx);
            // Character Primitive and Primitive Wrapper Types
        else if (ft.equals(Character.class) || ft.equals(Character.TYPE))
            deterministicallyPopulateCharacterField(obj, field, idx, fieldIdx);
            // String Type
        else if (ft.equals(String.class))
            deterministicallyPopulateStringField(obj, field, idx, fieldIdx, lengthOrScaleMapper);
            // Array Types (Character & Byte Arrays)
        else if (ft.isArray() && (ft.getComponentType().equals(Character.class) || ft.getComponentType().equals(Character.TYPE)))
            deterministicallyPopulateCharacterArrayField(obj, field, idx, fieldIdx, lengthOrScaleMapper);
        else if (ft.isArray() && (ft.getComponentType().equals(Byte.class) || ft.getComponentType().equals(Byte.TYPE)))
            deterministicallyPopulateByteArrayField(obj, field, idx, fieldIdx, lengthOrScaleMapper);
            // Temporal Types
        else if (ft.equals(Date.class))
            deterministicallyPopulateDateField(obj, field, idx, fieldIdx, ambiguousTemporalTypeMapper);
        else if (ft.equals(Calendar.class))
            deterministicallyPopulateCalendarField(obj, field, idx, fieldIdx, ambiguousTemporalTypeMapper);
        else if (ft.equals(java.sql.Date.class))
            deterministicallyPopulateSqlDateField(obj, field, idx, fieldIdx);
        else if (ft.equals(Time.class))
            deterministicallyPopulateSqlTimeField(obj, field, idx, fieldIdx);
        else if (ft.equals(Timestamp.class))
            deterministicallyPopulateSqlTimestampField(obj, field, idx, fieldIdx);
        else if (ft.equals(LocalDate.class))
            deterministicallyPopulateLocalDateField(obj, field, idx, fieldIdx);
        else if (ft.equals(LocalTime.class))
            deterministicallyPopulateLocalTimeField(obj, field, idx, fieldIdx);
        else if (ft.equals(LocalDateTime.class))
            deterministicallyPopulateLocalDateTimeField(obj, field, idx, fieldIdx);
        else if (ft.equals(Instant.class))
            deterministicallyPopulateInstantField(obj, field, idx, fieldIdx);
        else if (ft.equals(Duration.class))
            deterministicallyPopulateDurationField(obj, field, idx, fieldIdx);
        else if (ft.equals(OffsetDateTime.class))
            deterministicallyPopulateOffsetDateTimeField(obj, field, idx, fieldIdx);
        else if (ft.equals(OffsetTime.class))
            deterministicallyPopulateOffsetTimeField(obj, field, idx, fieldIdx);
        else if (ft.equals(ZonedDateTime.class))
            deterministicallyPopulateZonedDateTimeField(obj, field, idx, fieldIdx);
        else if (ft.equals(TimeZone.class))
            deterministicallyPopulateTimeZoneField(obj, field, idx, fieldIdx);
            // Localization Types
        else if (ft.equals(Currency.class))
            deterministicallyPopulateCurrencyField(obj, field, idx, fieldIdx);
        else if (ft.equals(Locale.class))
            deterministicallyPopulateLocaleField(obj, field, idx, fieldIdx);
            // Network Types
        else if (ft.equals(URL.class))
            deterministicallyPopulateNetURLField(obj, field, idx, fieldIdx);
            // Misc Types
        else if (ft.equals(UUID.class))
            deterministicallyPopulateUUIDField(obj, field, idx, fieldIdx);
            // Embedded Types
        else if (populateCompositePredicate.test(field, ft))
            deterministicallyPopulateCompositeField(obj, field, idx, fieldIdx);
        else return false;
        return true;
    }

    /**
     * @param obj
     * @param field
     * @param ft
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateEnumField(Object obj, Field field, Class<?> ft, int idx, int fieldIdx) {
        int numEnumVals = ft.getEnumConstants().length;
        try {
            field.set(obj, ft.getEnumConstants()[(idx + fieldIdx) % numEnumVals]);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateByteField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, (byte) (Math.abs(idx + fieldIdx) % (Byte.MAX_VALUE * 2 + 1) - Byte.MAX_VALUE - 1));
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateShortField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, (short) (Math.abs(idx + fieldIdx) % (Short.MAX_VALUE * 2 + 1) - Short.MAX_VALUE - 1));
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateIntegerField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, (int) (Math.abs((long) idx + (long) fieldIdx) % (Integer.MAX_VALUE * 2L + 1L) - Integer.MAX_VALUE - 1L));
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateLongField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, Math.abs((long) idx + (long) fieldIdx));
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateBigIntegerField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, BigInteger.valueOf((long) idx + (long) fieldIdx));
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateFloatField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, idx + (float) Integer.MAX_VALUE / fieldIdx);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateDoubleField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, idx + (double) Integer.MAX_VALUE / fieldIdx);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param lengthOrScaleMapper
     */
    public static void deterministicallyPopulateBigDecimalField(Object obj,
                                                                Field field,
                                                                int idx,
                                                                int fieldIdx,
                                                                Function<Field, Integer> lengthOrScaleMapper) {
        try {
            int scale = lengthOrScaleMapper.apply(field);
            field.set(obj, BigDecimal.valueOf(idx + (double) Integer.MAX_VALUE / fieldIdx).setScale(scale, RoundingMode.HALF_EVEN));
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateBooleanField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, idx + fieldIdx % 2 == 0 ? false : true);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateCharacterField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            field.set(obj, (char) (Character.SPACE_SEPARATOR + (idx + fieldIdx) % 95));
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param lengthOrScaleMapper
     */
    public static void deterministicallyPopulateStringField(Object obj,
                                                            Field field,
                                                            int idx,
                                                            int fieldIdx,
                                                            Function<Field, Integer> lengthOrScaleMapper) {
        try {
            int length = lengthOrScaleMapper.apply(field);
            String val = "";
            for (int i = 0; i < length; i++)
                val += (char) (Character.SPACE_SEPARATOR + (idx + fieldIdx) % 95);
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param lengthOrScaleMapper
     */
    public static void deterministicallyPopulateCharacterArrayField(Object obj,
                                                                    Field field,
                                                                    int idx,
                                                                    int fieldIdx,
                                                                    Function<Field, Integer> lengthOrScaleMapper) {
        try {
            int length = lengthOrScaleMapper.apply(field);
            char[] val = new char[length];
            for (int i = 0; i < length; i++)
                val[i] = (char) (Character.SPACE_SEPARATOR + (idx + fieldIdx) % 95);
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param lengthOrScaleMapper
     */
    public static void deterministicallyPopulateByteArrayField(Object obj,
                                                               Field field,
                                                               int idx,
                                                               int fieldIdx,
                                                               Function<Field, Integer> lengthOrScaleMapper) {
        try {
            int length = lengthOrScaleMapper.apply(field);
            byte[] val = new byte[length];
            for (int i = 0; i < length; i++)
                val[i] = (byte) (Math.abs(idx + fieldIdx) % (Byte.MAX_VALUE * 2 + 1) - Byte.MAX_VALUE - 1);
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param ambiguousTemporalTypeMapper
     */
    @SuppressWarnings("deprecation")
    public static void deterministicallyPopulateDateField(Object obj,
                                                          Field field,
                                                          int idx,
                                                          int fieldIdx,
                                                          Function<Field, TemporalType> ambiguousTemporalTypeMapper) {
        try {
            TemporalType tt = ambiguousTemporalTypeMapper.apply(field);
            Date val = tt.equals(DATE)
                    ? new Date
                    (
                            100 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12,
                            (idx + fieldIdx) % 28 + 1
                    )
                    : tt.equals(TIME)
                    ? new Date
                    (
                            -1900,
                            0,
                            1,
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60
                    )
                    : new Date
                    (
                            100 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12,
                            (idx + fieldIdx) % 28 + 1,
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     * @param ambiguousTemporalTypeMapper
     */
    public static void deterministicallyPopulateCalendarField(Object obj,
                                                              Field field,
                                                              int idx,
                                                              int fieldIdx,
                                                              Function<Field, TemporalType> ambiguousTemporalTypeMapper) {
        try {
            TemporalType tt = ambiguousTemporalTypeMapper.apply(field);
            Calendar val = Calendar.getInstance();
            if (tt.equals(DATE))
                val.set
                        (
                                2000 + (idx * 100 + fieldIdx) % 50,
                                (idx + fieldIdx) % 12,
                                (idx + fieldIdx) % 28 + 1
                        );
            else if (tt.equals(TIME))
                val.set
                        (
                                0,
                                0,
                                1,
                                (idx + fieldIdx) % 24,
                                (idx + fieldIdx) % 60,
                                (idx + fieldIdx) % 60
                        );
            else
                val.set
                        (
                                2000 + (idx * 100 + fieldIdx) % 50,
                                (idx + fieldIdx) % 12,
                                (idx + fieldIdx) % 28 + 1,
                                (idx + fieldIdx) % 24,
                                (idx + fieldIdx) % 60,
                                (idx + fieldIdx) % 60
                        );
            field.set(obj, val);
        } catch (
                IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }

    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateSqlDateField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            @SuppressWarnings("deprecation")
            java.sql.Date val = new java.sql.Date
                    (
                            100 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12,
                            (idx + fieldIdx) % 28 + 1
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateSqlTimeField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            @SuppressWarnings("deprecation")
            Time val = new Time
                    (
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateSqlTimestampField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            @SuppressWarnings("deprecation")
            Timestamp val = new Timestamp
                    (
                            100 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12,
                            (idx + fieldIdx) % 28 + 1,
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60,
                            0
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateLocalDateField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            LocalDate val = LocalDate.of
                    (
                            2000 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12 + 1,
                            (idx + fieldIdx) % 28 + 1
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateLocalTimeField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            LocalTime val = LocalTime.of
                    (
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60,
                            0
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateLocalDateTimeField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            LocalDateTime val = LocalDateTime.of
                    (
                            2000 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12 + 1,
                            (idx + fieldIdx) % 28 + 1,
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60,
                            0
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateInstantField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            Instant val = Instant.ofEpochMilli
                    (
                            (int) (((long) Math.pow(idx, fieldIdx) + idx * fieldIdx + idx + fieldIdx) % 1_000_000_000)
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateDurationField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            Duration val = Duration.ofSeconds
                    (
                            (long) Math.pow(fieldIdx, idx),
                            (long) Math.pow(idx, fieldIdx % 1_000_000_000)
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateOffsetDateTimeField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            OffsetDateTime val = OffsetDateTime.of
                    (
                            2000 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12 + 1,
                            (idx + fieldIdx) % 28 + 1,
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60,
                            0,
                            ZoneOffset.UTC
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateOffsetTimeField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            OffsetTime val = OffsetTime.of
                    (
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60,
                            0,
                            ZoneOffset.UTC
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateZonedDateTimeField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            ZonedDateTime val = ZonedDateTime.of
                    (
                            2000 + (idx * 100 + fieldIdx) % 50,
                            (idx + fieldIdx) % 12 + 1,
                            (idx + fieldIdx) % 28 + 1,
                            (idx + fieldIdx) % 24,
                            (idx + fieldIdx) % 60,
                            (idx + fieldIdx) % 60,
                            0,
                            ZoneOffset.UTC
                    );
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateTimeZoneField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            int numTimezones = TimeZone.getAvailableIDs().length;
            TimeZone val = TimeZone.getTimeZone(TimeZone.getAvailableIDs()[(idx + fieldIdx) % numTimezones]);
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateCurrencyField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            int numCurrencies = Currency.getAvailableCurrencies().size();
            Currency val = Currency
                    .getAvailableCurrencies()
                    .stream()
                    .skip((idx + fieldIdx) % numCurrencies)
                    .findFirst()
                    .orElse(Currency.getInstance(("USD")));
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateLocaleField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            int numLocales = Locale.getAvailableLocales().length;
            Locale val = Locale.getAvailableLocales()[(idx + fieldIdx) % numLocales];
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateNetURLField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            URL val = new URL("http://sample.url.org/idx/" + idx + "/fieldIdx/" + fieldIdx);
            field.set(obj, val);
        } catch (IllegalAccessException | MalformedURLException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateUUIDField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            UUID val = new UUID((long) Math.pow(idx, fieldIdx), (long) idx * fieldIdx);
            field.set(obj, val);
        } catch (IllegalAccessException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    /**
     * @param obj
     * @param field
     * @param idx
     * @param fieldIdx
     */
    public static void deterministicallyPopulateCompositeField(Object obj, Field field, int idx, int fieldIdx) {
        try {
            Constructor<?> constructor = field.getType().getConstructor(field.getType());
            Object embeddable = constructor.newInstance();
            deterministicallyPopulateBaseFields(idx * 100_000 + fieldIdx, embeddable);
            field.set(obj, embeddable);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throwAsRuntimeExceptionUnableToSetField(obj, field, idx, fieldIdx, e);
        }
    }

    private static boolean defaultFieldExclusionPredicate(Field field) {
        return false;
    }

    private static boolean defaultPopulateCompositePredicate(Field field, Class<?> fieldType) {
        return false;
    }

    private static TemporalType defaultAmbiguousTemporalTypeMapper(Field field) {
        return TIMESTAMP;
    }

    private static Integer defaulLengthOrScaleMapper(Field field) {
        return field.getType() == BigDecimal.class
                ? 4
                : 20;
    }

}