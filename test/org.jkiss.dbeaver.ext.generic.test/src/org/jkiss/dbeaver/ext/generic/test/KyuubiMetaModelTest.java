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
package org.jkiss.dbeaver.ext.generic.test;

import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.GenericMetaModelRegistry;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericFunctionResultType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.ext.generic.model.GenericUtils;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KyuubiMetaModelTest extends DBeaverUnitTest {

    @Test
    public void functionAliasesHaveDistinctUniqueNames() throws Exception {
        DBPDataSourceContainer container = configureTestContainer("kyuubi_hive");
        GenericMetaModel metaModel = GenericMetaModelRegistry.getInstance().getMetaModel(container);
        GenericMetaObject procedureObject = metaModel.getMetaObject(GenericConstants.OBJECT_PROCEDURE);
        GenericDataSource dataSource = new GenericDataSource(monitor, metaModel, container, new GenericSQLDialect());
        GenericCatalog catalog = new GenericCatalog(dataSource, "test");
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(JDBCConstants.SPECIFIC_NAME)).thenReturn("org.apache.spark.sql.catalyst.expressions.Ceil");
        when(resultSet.getString(JDBCConstants.FUNCTION_NAME)).thenReturn("ceil", "ceiling");

        GenericProcedure ceil = metaModel.createProcedureImpl(
            catalog, "ceil",
            GenericUtils.safeGetStringTrimmed(procedureObject, resultSet, JDBCConstants.SPECIFIC_NAME),
            null, DBSProcedureType.FUNCTION, GenericFunctionResultType.NO_TABLE);
        GenericProcedure ceiling = metaModel.createProcedureImpl(
            catalog, "ceiling",
            GenericUtils.safeGetStringTrimmed(procedureObject, resultSet, JDBCConstants.SPECIFIC_NAME),
            null, DBSProcedureType.FUNCTION, GenericFunctionResultType.NO_TABLE);

        assertEquals("ceil", ceil.getUniqueName());
        assertEquals("ceiling", ceiling.getUniqueName());
        assertNotEquals(ceil.getUniqueName(), ceiling.getUniqueName());
    }

    @Test
    public void standardMetaModelPreservesSpecificNames() throws Exception {
        GenericMetaModel metaModel = GenericMetaModelRegistry.getInstance().getStandardMetaModel().getInstance();
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(JDBCConstants.FUNCTION_NAME)).thenReturn("function");
        when(resultSet.getString(JDBCConstants.SPECIFIC_NAME)).thenReturn("function_overload");

        assertEquals("function_overload", GenericUtils.safeGetStringTrimmed(
            metaModel.getMetaObject(GenericConstants.OBJECT_PROCEDURE), resultSet, JDBCConstants.SPECIFIC_NAME));
    }
}
