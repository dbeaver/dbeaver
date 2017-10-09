/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBytes;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transforms binary attribute value into UUID
 */
public class UUIDAttributeTransformer implements DBDAttributeTransformer {

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, "UUID", -1, DBPDataKind.BINARY));

        attribute.setTransformHandler(new UUIDValueHandler(attribute.getValueHandler()));
    }

    private class UUIDValueHandler extends ProxyValueHandler {
        public UUIDValueHandler(DBDValueHandler target) {
            super(target);
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            byte[] bytes = null;
            if (value instanceof byte[]) {
                bytes = (byte[]) value;
            } else if (value instanceof JDBCContentBytes) {
                bytes = ((JDBCContentBytes) value).getRawValue();
            }
            if (bytes != null) {
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                return new UUID(bb.getLong(), bb.getLong()).toString();
            }
            return super.getValueDisplayString(column, value, format);
        }
    }

}
