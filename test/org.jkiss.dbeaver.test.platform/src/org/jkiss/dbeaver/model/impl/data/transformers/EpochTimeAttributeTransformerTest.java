/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDRowIdentifier;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
public class EpochTimeAttributeTransformerTest extends DBeaverUnitTest {
    private static final String NANOS = "nanoseconds";
    private static final String MILLIS = "milliseconds";
    private static final String SECONDS = "seconds";

    private final EpochTimeAttributeTransformer transformer = new EpochTimeAttributeTransformer();

    private final DBCSession session = mock(DBCSession.class);
    private final DBSTypedObject column = mock(DBSTypedObject.class);
    private final DBDValueHandler handler = mock(DBDValueHandler.class);

    private DBDAttributeBinding attributeBinding;
    private DBDValueHandler proxyHandler;

    @Before
    public void init() {
        attributeBinding = new DBDAttributeBindingTestDouble(handler);
    }

    private void setOptions(@Nullable String unit, @Nullable String timezoneID) {
        Map<String, Object> map = new HashMap<>(2, 1);
        if (unit != null) {
            map.put(EpochTimeAttributeTransformer.PROP_UNIT, unit);
        }
        if (timezoneID != null) {
            map.put(EpochTimeAttributeTransformer.ZONE_ID, timezoneID);
        }
        try {
            transformer.transformAttribute(session, attributeBinding, Collections.emptyList(), Collections.unmodifiableMap(map));
        } catch (DBException e) {
            throw new RuntimeException(e);
        }
        proxyHandler = attributeBinding.getValueHandler();
    }

    private String getDisplayString(Object o) {
        return proxyHandler.getValueDisplayString(column, o, DBDDisplayFormat.UI);
    }

    private Object getValue(Object o) {
        try {
            return proxyHandler.getValueFromObject(session, column, o, false, false);
        } catch (DBCException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMillisAndUTC() {
        setOptions(MILLIS, "UTC");

        assertEquals("1970-01-01 00:00:00.000", getDisplayString(0));
        assertEquals(0L, getValue("1970-01-01 00:00:00.000"));

        assertEquals("1970-01-01 00:00:00.042", getDisplayString(42));
        assertEquals(42L, getValue("1970-01-01 00:00:00.042"));

        assertEquals("1969-12-31 23:59:59.999", getDisplayString(-1));
        assertEquals(-1L, getValue("1969-12-31 23:59:59.999"));
    }

    @Test
    public void testSecondsAndParis() {
        setOptions(SECONDS, "Europe/Paris");

        assertEquals("1970-01-01 01:00:00", getDisplayString(0));
        assertEquals(0L, getValue("1970-01-01 01:00:00"));

        assertEquals("1970-01-01 01:00:42", getDisplayString(42));
        assertEquals(42L, getValue("1970-01-01 01:00:42"));

        assertEquals("1970-01-01 00:59:59", getDisplayString(-1));
        assertEquals(-1L, getValue("1970-01-01 00:59:59"));
    }

    @Test
    public void testNanosAndUTC() {
        setOptions(NANOS, "UTC");

        assertEquals("1970-01-01 00:00:00.000000000", getDisplayString(0));
        assertEquals(0L, getValue("1970-01-01 00:00:00.000000000"));

        assertEquals("1970-01-01 00:00:00.000000420", getDisplayString(420));
        assertEquals(420L, getValue("1970-01-01 00:00:00.000000420"));

        assertEquals("1970-01-01 00:00:01.000000420", getDisplayString(1_000_000_420));
        assertEquals(1_000_000_420L, getValue("1970-01-01 00:00:01.000000420"));

        assertEquals("1969-12-31 23:59:59.999999580", getDisplayString(-420));
        assertEquals(-420L, getValue("1969-12-31 23:59:59.999999580"));

        assertEquals("1969-12-31 23:59:58.999999580", getDisplayString(-1_000_000_420));
        assertEquals(-1_000_000_420L, getValue("1969-12-31 23:59:58.999999580"));
    }

    private static class DBDAttributeBindingTestDouble extends DBDAttributeBinding {
        protected DBDAttributeBindingTestDouble(@NotNull DBDValueHandler valueHandler) {
            super(valueHandler);
        }

        @Nullable
        @Override
        public DBDAttributeBinding getParentObject() {
            return null;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            return null;
        }

        @Override
        public int getOrdinalPosition() {
            return 0;
        }

        @Override
        public boolean isRequired() {
            return false;
        }

        @Override
        public boolean isAutoGenerated() {
            return false;
        }

        @NotNull
        @Override
        public String getLabel() {
            return null;
        }

        @NotNull
        @Override
        public String getName() {
            return null;
        }

        @Nullable
        @Override
        public DBCAttributeMetaData getMetaAttribute() {
            return null;
        }

        @Nullable
        @Override
        public DBSEntityAttribute getEntityAttribute() {
            return null;
        }

        @Nullable
        @Override
        public DBDRowIdentifier getRowIdentifier() {
            return null;
        }

        @Override
        public String getRowIdentifierStatus() {
            return null;
        }

        @Nullable
        @Override
        public List<DBSEntityReferrer> getReferrers() {
            return null;
        }

        @Nullable
        @Override
        public Object extractNestedValue(@NotNull Object ownerValue, int itemIndex) throws DBCException {
            return null;
        }

        @NotNull
        @Override
        public String getTypeName() {
            return null;
        }

        @NotNull
        @Override
        public String getFullTypeName() {
            return null;
        }

        @Override
        public int getTypeID() {
            return 0;
        }

        @NotNull
        @Override
        public DBPDataKind getDataKind() {
            return null;
        }

        @Nullable
        @Override
        public Integer getScale() {
            return null;
        }

        @Nullable
        @Override
        public Integer getPrecision() {
            return null;
        }

        @Override
        public long getMaxLength() {
            return 0;
        }

        @Override
        public long getTypeModifiers() {
            return 0;
        }
    }
}
