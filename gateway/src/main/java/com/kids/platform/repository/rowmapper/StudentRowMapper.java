package com.kids.platform.repository.rowmapper;

import com.kids.platform.domain.Student;
import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Student}, with proper type conversions.
 */
@Service
public class StudentRowMapper implements BiFunction<Row, String, Student> {

    private final ColumnConverter converter;

    public StudentRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Student} stored in the database.
     */
    @Override
    public Student apply(Row row, String prefix) {
        Student entity = new Student();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setNickname(converter.fromRow(row, prefix + "_nickname", String.class));
        entity.setAge(converter.fromRow(row, prefix + "_age", Integer.class));
        entity.setAvatarStyle(converter.fromRow(row, prefix + "_avatar_style", String.class));
        entity.setParentId(converter.fromRow(row, prefix + "_parent_id", Long.class));
        return entity;
    }
}
