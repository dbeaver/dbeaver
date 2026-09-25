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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * {@link MimerDatabankShadow}'s DDL builders - {@code CREATE}/{@code DROP SHADOW}, the three
 * {@code ALTER SHADOW} forms ({@code INTO}, {@code ADD ... PAGES}, {@code TO MASTER} is a plain
 * action string), and the {@code SET SHADOW} online-state family (via
 * {@link MimerUtils#buildSetOnlineDDL}).
 *
 * @author Mimer Information Technology
 */
public class MimerDatabankShadowTest extends DBeaverUnitTest {

    @Mock
    private MimerDataSource dataSource;

    private MimerDatabankShadow newShadow() {
        return new MimerDatabankShadow(new MimerDatabank(dataSource, "abc"), "abc_shadow");
    }

    @Test
    public void createDDLNamesTheDatabankAndFile() {
        MimerDatabankShadow shadow = newShadow();
        shadow.setFileName("abc_shadow.dbf");
        Assertions.assertEquals(
            "CREATE SHADOW \"abc_shadow\" FOR \"abc\" IN 'abc_shadow.dbf'",
            shadow.buildCreateDDL());
    }

    @Test
    public void dropDDLIsAPlainDrop() {
        Assertions.assertEquals("DROP SHADOW \"abc_shadow\"", newShadow().buildDropDDL());
    }

    @Test
    public void alterFileNameDDLIsNullUntilAFileNameIsSet() {
        Assertions.assertNull(newShadow().buildAlterFileNameDDL());
    }

    @Test
    public void alterFileNameDDLEmitsIntoClause() {
        MimerDatabankShadow shadow = newShadow();
        shadow.setFileName("  /data/moved/abc_shadow.dbf  ");
        Assertions.assertEquals(
            "ALTER SHADOW \"abc_shadow\" INTO '/data/moved/abc_shadow.dbf'",
            shadow.buildAlterFileNameDDL());
    }

    @Test
    public void addPagesDDL() {
        Assertions.assertEquals(
            "ALTER SHADOW \"abc_shadow\" ADD 250 PAGES",
            MimerDatabankShadow.buildAddPagesDDL("abc_shadow", 250));
    }

    @Test
    public void setOnlineStateFamily() {
        MimerDatabankShadow shadow = newShadow();
        Assertions.assertEquals(MimerUtils.ONLINE_NOCHANGE, shadow.getOnlineTransition());
        Assertions.assertNull(shadow.buildSetOnlineDDL());

        shadow.setOnlineTransition(MimerUtils.ONLINE_STATE_OFFLINE);
        Assertions.assertEquals("SET SHADOW \"abc_shadow\" OFFLINE", shadow.buildSetOnlineDDL());

        shadow.setOnlineTransition(MimerUtils.ONLINE_STATE_RESET);
        Assertions.assertEquals("SET SHADOW \"abc_shadow\" ONLINE RESET LOG", shadow.buildSetOnlineDDL());
    }
}
