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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

/**
 * {@link MimerUtils#formatDomainDataType}/{@link MimerUtils#buildCommentDDL} - small, shared pure
 * helpers used across most of this plugin's object types (comment DDL alone backs Schema/Sequence/
 * Function/Procedure/Module/Trigger/Domain/Collation/Databank/Index).
 *
 * @author Mimer Information Technology
 */
public class MimerUtilsTest extends DBeaverUnitTest {

    @Mock
    private DBSObject context;

    @Mock
    private MimerDataSource dataSource;

    @BeforeEach
    public void setUp() {
        when(context.getDataSource()).thenReturn(dataSource);
        when(dataSource.getSQLDialect()).thenReturn(new MimerSQLDialect());
    }

    @Test
    public void formatDomainDataTypeReturnsEmptyForNoDataType() {
        Assertions.assertEquals("", MimerUtils.formatDomainDataType("", 0, null, null));
    }

    @Test
    public void formatDomainDataTypeAppendsCharacterLengthWhenPositive() {
        Assertions.assertEquals("VARCHAR(20)", MimerUtils.formatDomainDataType("varchar", 20, null, null));
    }

    @Test
    public void formatDomainDataTypePrefersCharacterLengthOverNumericPrecision() {
        // A character-length-bearing type never also carries a numeric precision in practice, but
        // the method itself checks charLength first - verify that ordering holds.
        Assertions.assertEquals("VARCHAR(20)", MimerUtils.formatDomainDataType("varchar", 20, 5, 2));
    }

    @Test
    public void formatDomainDataTypeAppendsPrecisionAndScaleWhenBothPositive() {
        Assertions.assertEquals("DECIMAL(10,2)", MimerUtils.formatDomainDataType("decimal", 0, 10, 2));
    }

    @Test
    public void formatDomainDataTypeAppendsOnlyPrecisionWhenScaleIsZeroOrAbsent() {
        Assertions.assertEquals("DECIMAL(10)", MimerUtils.formatDomainDataType("decimal", 0, 10, 0));
        Assertions.assertEquals("DECIMAL(10)", MimerUtils.formatDomainDataType("decimal", 0, 10, null));
    }

    @Test
    public void formatDomainDataTypeReturnsBareUppercaseTypeWithNoLengthOrPrecision() {
        Assertions.assertEquals("INTEGER", MimerUtils.formatDomainDataType("integer", 0, null, null));
        Assertions.assertEquals("INTEGER", MimerUtils.formatDomainDataType("integer", 0, 0, 0));
    }

    @Test
    public void buildCommentDDLQuotesTheCommentText() {
        String ddl = MimerUtils.buildCommentDDL(context, "DOMAIN", "\"s\".\"d\"", "a comment");
        Assertions.assertEquals("COMMENT ON DOMAIN \"s\".\"d\" IS 'a comment'", ddl);
    }

    @Test
    public void buildCommentDDLEscapesSingleQuotesInTheComment() {
        String ddl = MimerUtils.buildCommentDDL(context, "SEQUENCE", "\"s\".\"seq\"", "it's a comment");
        Assertions.assertEquals("COMMENT ON SEQUENCE \"s\".\"seq\" IS 'it''s a comment'", ddl);
    }

    @Test
    public void buildCommentDDLTreatsANullCommentAsEmptyString() {
        String ddl = MimerUtils.buildCommentDDL(context, "IDENT", "\"u\"", null);
        Assertions.assertEquals("COMMENT ON IDENT \"u\" IS ''", ddl);
    }
}
