/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transforms binary(16) attribute value into UUID.
 * <p>
 * This specific UUID storage format saves space and improves INSERT performance as described in the linked article.
 * </p>
 * 
 * @see <a href="https://www.percona.com/blog/2014/12/19/store-uuid-optimized-way/">Store UUID in an optimized way</a>
 */
public class UUIDOrderedAttributeTransformer implements DBDAttributeTransformer {

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, "UUID (Ordered)", 16, DBPDataKind.BINARY));

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
            	// byte shift operations from Ebean ORM project pull request #1308 
            	long mostSigBits = ((long)bytes[4] << 56) // XXXXXXXX-____-____-...
        			+ ((long)(bytes[5] & 255) << 48)
        			+ ((long)(bytes[6] & 255) << 40)
        			+ ((long)(bytes[7] & 255) << 32)
        			+ ((long)(bytes[2] & 255) << 24)      // ________-XXXX-____-...
        			+ ((bytes[3] & 255) << 16)
        			+ ((bytes[0] & 255) <<  8)            // ________-____-XXXX-...
        			+ ((bytes[1] & 255) <<  0);
            	long leastSigBits = ((long)bytes[8] << 56)// ________-____-____-XXXX-...
        			+ ((long)(bytes[9] & 255) << 48)
        			+ ((long)(bytes[10] & 255) << 40)     // ________-____-____-____-XXXXXXXXXXXX
        			+ ((long)(bytes[11] & 255) << 32)
        			+ ((long)(bytes[12] & 255) << 24)
        			+ ((bytes[13] & 255) << 16)
        			+ ((bytes[14] & 255) <<  8)
        			+ ((bytes[15] & 255) <<  0);
            	return new UUID(mostSigBits, leastSigBits).toString();
            }
            return super.getValueDisplayString(column, value, format);
        }
    }

}
