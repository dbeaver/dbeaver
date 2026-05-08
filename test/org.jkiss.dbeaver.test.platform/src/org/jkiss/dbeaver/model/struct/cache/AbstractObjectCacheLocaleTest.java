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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Locale;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Regression coverage for case-insensitive object lookup in
 * {@link AbstractObjectCache} under non-English JVM default locales.
 *
 * Turkish/Azeri/Lithuanian locales fold ASCII letters differently from
 * {@link Locale#ENGLISH}: {@code "items".toUpperCase(tr_TR)} is {@code "İTEMS"}
 * (LATIN CAPITAL LETTER I WITH DOT ABOVE), not ASCII {@code "ITEMS"}. Before
 * the fix, both {@link AbstractObjectCache#getCachedObject(String)} and
 * {@code getObjectName} folded names with the JVM default locale, so a table
 * stored under the cached name {@code "items"} could no longer be located by
 * a lookup with the name {@code "ITEMS"} on a Turkish JVM - the two folded
 * keys are {@code "İTEMS"} and {@code "ITEMS"} and the map returned
 * {@code null}.
 */
public class AbstractObjectCacheLocaleTest extends DBeaverUnitTest {

    private static final Locale TURKISH = Locale.forLanguageTag("tr-TR");

    private static Locale originalDefaultLocale;

    @BeforeClass
    public static void setTurkishDefaultLocale() {
        originalDefaultLocale = Locale.getDefault();
        Locale.setDefault(TURKISH);
    }

    @AfterClass
    public static void restoreDefaultLocale() {
        Locale.setDefault(originalDefaultLocale);
    }

    /**
     * Concrete cache used purely as a test harness over the abstract base.
     */
    private static final class TestObjectCache extends AbstractObjectCache<DBSObject, DBSObject> {
        @NotNull
        @Override
        public Collection<DBSObject> getAllObjects(@NotNull DBRProgressMonitor monitor, @Nullable DBSObject owner)
            throws DBException
        {
            return getCachedObjects();
        }
    }

    private static DBSObject namedObject(@NotNull String name) {
        DBSObject obj = Mockito.mock(DBSObject.class);
        Mockito.when(obj.getName()).thenReturn(name);
        return obj;
    }

    /**
     * DB stores "items"; caller asks for "ITEMS". Pre-fix this lookup returned null
     * on a Turkish JVM because the two folds were "İTEMS" vs "ITEMS".
     */
    @Test
    public void getCachedObject_caseInsensitive_findsLowercaseEntryViaUppercaseKey_underTurkishLocale() {
        TestObjectCache cache = new TestObjectCache();
        cache.setCaseSensitive(false);
        DBSObject items = namedObject("items");
        cache.cacheObject(items);

        assertSame(
            "Case-insensitive lookup under Turkish locale must find a cached lowercase name via an uppercase key",
            items,
            cache.getCachedObject("ITEMS")
        );
    }

    @Test
    public void getCachedObject_caseInsensitive_findsUppercaseEntryViaLowercaseKey_underTurkishLocale() {
        TestObjectCache cache = new TestObjectCache();
        cache.setCaseSensitive(false);
        DBSObject items = namedObject("ITEMS");
        cache.cacheObject(items);

        assertSame(
            "Case-insensitive lookup under Turkish locale must find a cached uppercase name via a lowercase key",
            items,
            cache.getCachedObject("items")
        );
    }

    /**
     * Case-sensitive lookups must keep their existing exact-match semantics
     * regardless of the JVM default locale - the fix only touches the
     * !caseSensitive code path.
     */
    @Test
    public void getCachedObject_caseSensitive_requiresExactMatch_underTurkishLocale() {
        TestObjectCache cache = new TestObjectCache();
        // caseSensitive defaults to true; do not toggle it.
        DBSObject items = namedObject("items");
        cache.cacheObject(items);

        assertSame(items, cache.getCachedObject("items"));
        assertNull(
            "Case-sensitive lookup must not collapse case under any locale",
            cache.getCachedObject("ITEMS")
        );
    }

    /**
     * Identifiers with no i/I are unaffected by the Turkish-locale fold;
     * pinned here to catch any future regression in the ASCII path.
     */
    @Test
    public void getCachedObject_caseInsensitive_asciiOnlyNames_stillResolve_underTurkishLocale() {
        TestObjectCache cache = new TestObjectCache();
        cache.setCaseSensitive(false);
        DBSObject orders = namedObject("orders");
        cache.cacheObject(orders);

        assertSame(orders, cache.getCachedObject("ORDERS"));
        assertSame(orders, cache.getCachedObject("Orders"));
        assertSame(orders, cache.getCachedObject("orders"));
    }
}
