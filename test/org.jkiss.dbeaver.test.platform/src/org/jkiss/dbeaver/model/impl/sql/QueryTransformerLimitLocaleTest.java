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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Locale;

public class QueryTransformerLimitLocaleTest extends DBeaverUnitTest {

    private Locale defaultLocale;

    @BeforeEach
    public void setTurkishLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
    }

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void keepsQueryWithLowercaseLimit() throws Exception {
        SQLQuery query = Mockito.mock(SQLQuery.class);
        Mockito.when(query.getText()).thenReturn("select id from items limit 10");
        Mockito.when(query.isPlainSelect()).thenReturn(true);
        QueryTransformerLimit transformer = new QueryTransformerLimit(false, true);
        transformer.setParameters(0, 200);

        Assertions.assertEquals("select id from items limit 10", transformer.transformQueryString(query));
    }
}
