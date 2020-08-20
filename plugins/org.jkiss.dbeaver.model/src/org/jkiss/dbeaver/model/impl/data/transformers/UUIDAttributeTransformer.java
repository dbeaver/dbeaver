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
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Transforms binary attribute value into UUID.
 *
 *  Ordered UUID storage format saves space and improves INSERT performance as described in the linked article.
 *  @see <a href="https://www.percona.com/blog/2014/12/19/store-uuid-optimized-way/">Store UUID in an optimized way</a>
 *
 */
public class UUIDAttributeTransformer implements DBDAttributeTransformer {
    private static final String PROP_TYPE = "type";
    private static final String PROP_CASE = "case";
    private static final String PROP_ORDERED = "Ordered";
    private static final String PROP_MIXED_ENDIAN = "Mixed-Endian";
    private static final String PROP_UPPER_CASE = "Upper case";

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        boolean isOrdered = false;
        boolean isMixedEndian = false;
        boolean isUpperCase = false;
        if (options.get(PROP_TYPE).equals(PROP_ORDERED)) {
            isOrdered = true;
        } else if (options.get(PROP_TYPE).equals(PROP_MIXED_ENDIAN)) {
            isMixedEndian = true;
        }
        if (options.get(PROP_CASE).equals(PROP_UPPER_CASE)) {
            isUpperCase = true;
        }
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, "UUID", -1, DBPDataKind.BINARY));

        attribute.setTransformHandler(new UUIDValueHandler(attribute.getValueHandler(), isOrdered, isMixedEndian, isUpperCase));
    }

    private class UUIDValueHandler extends ProxyValueHandler {
        private boolean isOrdered;
        private boolean isMixedEndian;
        private boolean isUpperCase;

        public UUIDValueHandler(DBDValueHandler target, boolean isOrdered, boolean isMixedEndian, boolean isUpperCase) {
            super(target);
            this.isOrdered = isOrdered;
            this.isMixedEndian = isMixedEndian;
            this.isUpperCase = isUpperCase;
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
                if (bytes.length < 16) {
                    throw new IllegalArgumentException("UUID length must be at least 16 bytes (actual length = " + bytes.length + ")");
                }
                if (isOrdered) {
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
                    if (isUpperCase) {
                        return new UUID(mostSigBits, leastSigBits).toString().toUpperCase(Locale.ENGLISH);
                    }
                    return new UUID(mostSigBits, leastSigBits).toString();
                } else if (isMixedEndian) {
                    try {
                        if (isUpperCase) {
                            return GeneralUtils.getMixedEndianUUIDFromBytes(bytes).toString().toUpperCase(Locale.ENGLISH);
                        }
                        return GeneralUtils.getMixedEndianUUIDFromBytes(bytes).toString();
                    } catch (Exception e) {
                        return String.valueOf(value);
                    }
                }
                else {
                    try {
                        if (isUpperCase) {
                            return GeneralUtils.getUUIDFromBytes(bytes).toString().toUpperCase(Locale.ENGLISH);
                        }
                        return GeneralUtils.getUUIDFromBytes(bytes).toString();
                    } catch (Exception e) {
                        return String.valueOf(value);
                    }
                }
            }
            return super.getValueDisplayString(column, value, format);
        }
    }

}
