package com.kids.platform.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class StudentSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("nickname", table, columnPrefix + "_nickname"));
        columns.add(Column.aliased("age", table, columnPrefix + "_age"));
        columns.add(Column.aliased("avatar_style", table, columnPrefix + "_avatar_style"));

        columns.add(Column.aliased("parent_id", table, columnPrefix + "_parent_id"));
        return columns;
    }
}
