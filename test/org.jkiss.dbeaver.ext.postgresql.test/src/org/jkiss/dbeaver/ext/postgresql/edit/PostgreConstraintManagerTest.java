/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreTestUtils;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class PostgreConstraintManagerTest extends DBeaverUnitTest {

    private PostgreTableRegular table;
    private PostgreTableColumn column;

    @BeforeEach
    public void setUp() throws Exception {
        PostgreDataSource dataSource = new PostgreDataSource(configureTestContainer("postgresql"), "PG Test", "postgres");
        PostgreRole user = new PostgreRole(null, "tester", "test", true);
        PostgreDatabase database = dataSource.createDatabaseImpl(monitor, "testdb", user, null, null, null);
        PostgreSchema schema = new PostgreSchema(database, "test_schema", user);
        table = new PostgreTableRegular(schema);
        table.setName("test_table");
        column = PostgreTestUtils.addColumn(table, "id", "int4", 1);
    }

    @Test
    public void generateNotNullConstraintWithoutParentheses() throws DBException {
        PostgreTableConstraint constraint = createConstraint("id_nn", DBSEntityConstraintType.NOT_NULL);

        // Copied constraints are not persisted and cannot use pg_get_constraintdef.
        Assertions.assertFalse(constraint.isPersisted());
        Assertions.assertEquals("CONSTRAINT id_nn NOT NULL id", generateDeclaration(constraint));
    }

    @Test
    public void quoteNotNullConstraintAndColumnNames() throws DBException {
        column.setName("Required Value");
        PostgreTableConstraint constraint = createConstraint("Required Value NN", DBSEntityConstraintType.NOT_NULL);

        Assertions.assertEquals(
            "CONSTRAINT \"Required Value NN\" NOT NULL \"Required Value\"",
            generateDeclaration(constraint)
        );
    }

    @Test
    public void useRenamedColumnInNotNullConstraint() throws DBException {
        PostgreTableConstraint constraint = createConstraint("id_nn", DBSEntityConstraintType.NOT_NULL);
        column.setName("renamed_id");

        Assertions.assertEquals("CONSTRAINT id_nn NOT NULL renamed_id", generateDeclaration(constraint));
    }

    @Test
    public void retainParenthesesForPrimaryKey() throws DBException {
        PostgreTableConstraint constraint = createConstraint("table_pk", DBSEntityConstraintType.PRIMARY_KEY);

        Assertions.assertEquals("CONSTRAINT table_pk PRIMARY KEY (id)", generateDeclaration(constraint));
    }

    @Test
    public void retainParenthesesForCompositeUniqueConstraint() throws DBException {
        PostgreTableConstraint constraint = createConstraint("table_uq", DBSEntityConstraintType.UNIQUE_KEY);
        constraint.addAttributeReference(PostgreTestUtils.addColumn(table, "Other Value", "int4", 2));

        Assertions.assertEquals("CONSTRAINT table_uq UNIQUE (id,\"Other Value\")", generateDeclaration(constraint));
    }

    @Test
    public void retainParenthesesForCheckConstraint() {
        PostgreTableConstraint constraint = new PostgreTableConstraint(table, "id_positive", DBSEntityConstraintType.CHECK);
        constraint.setSource("id > 0");

        Assertions.assertEquals("CONSTRAINT id_positive CHECK (id > 0)", generateDeclaration(constraint));
    }

    @NotNull
    private PostgreTableConstraint createConstraint(@NotNull String name, @NotNull DBSEntityConstraintType type) throws DBException {
        PostgreTableConstraint constraint = new PostgreTableConstraint(table, name, type);
        constraint.addAttributeReference(column);
        return constraint;
    }

    @NotNull
    private String generateDeclaration(@NotNull PostgreTableConstraint constraint) {
        return new PostgreConstraintManager().getNestedDeclaration(
            monitor, table, new DBECommandAbstract<>(constraint, "Create constraint"), Collections.emptyMap()).toString();
    }
}
