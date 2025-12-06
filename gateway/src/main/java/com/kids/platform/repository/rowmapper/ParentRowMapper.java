package com.kids.platform.repository.rowmapper;

import com.kids.platform.domain.Parent;
import io.r2dbc.spi.Row;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Parent}, with proper type conversions.
 */
@Service
public class ParentRowMapper implements BiFunction<Row, String, Parent> {

    private final ColumnConverter converter;

    public ParentRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Parent} stored in the database.
     */
    @Override
    public Parent apply(Row row, String prefix) {
        Parent entity = new Parent();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setFirstName(converter.fromRow(row, prefix + "_first_name", String.class));
        entity.setEmail(converter.fromRow(row, prefix + "_email", String.class));
        entity.setIsPremium(converter.fromRow(row, prefix + "_is_premium", Boolean.class));
        return entity;
    }
}
