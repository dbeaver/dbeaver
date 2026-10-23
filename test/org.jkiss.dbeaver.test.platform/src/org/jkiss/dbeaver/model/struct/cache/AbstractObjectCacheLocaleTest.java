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
package org.jkiss.dbeaver.model.struct.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class AbstractObjectCacheLocaleTest extends DBeaverUnitTest {

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
    public void findsCaseInsensitiveObjectWithLetterI() {
        DBSObject items = Mockito.mock(DBSObject.class);
        Mockito.when(items.getName()).thenReturn("items");
        TestObjectCache cache = new TestObjectCache();
        cache.setCaseSensitive(false);
        cache.setCache(new ArrayList<>(List.of(items)));

        Assertions.assertSame(items, cache.getCachedObject("ITEMS"));
        Assertions.assertSame(items, cache.getCachedObject("items"));
    }

    private static class TestObjectCache extends AbstractObjectCache<DBSObject, DBSObject> {
        @NotNull
        @Override
        public Collection<DBSObject> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable DBSObject owner) {
            return getCachedObjects();
        }

        @Nullable
        @Override
        public DBSObject getObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject owner, @NotNull String name) {
            return getCachedObject(name);
        }
    }
}
