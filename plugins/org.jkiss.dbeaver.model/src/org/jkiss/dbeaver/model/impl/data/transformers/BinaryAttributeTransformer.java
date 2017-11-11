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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterString;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentBytes;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transforms string value into binary
 */
public class BinaryAttributeTransformer implements DBDAttributeTransformer {

    private static final Log log = Log.getLog(BinaryAttributeTransformer.class);

    private static final String PROP_FORMAT = "format";
    private static final String PROP_ENCODING = "encoding";

    private static final String FORMAT_NATIVE = "native";
    private static final String FORMAT_HEX = "hex";

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
        DBPDataSource dataSource = session.getDataSource();
        String formatterId = options.getOrDefault(PROP_FORMAT, FORMAT_HEX);

        DBDBinaryFormatter formatter;
        if (FORMAT_NATIVE.equals(formatterId) && dataSource instanceof SQLDataSource) {
            formatter = ((SQLDataSource) dataSource).getSQLDialect().getNativeBinaryFormatter();
        } else {
            formatter = DBValueFormatting.getBinaryPresentation(formatterId);
        }
        if (formatter == null) {
            formatter = new BinaryFormatterString();
        }

        String encodingName = options.getOrDefault(PROP_ENCODING, GeneralUtils.UTF8_ENCODING);
        Charset charset;
        try {
            charset = Charset.forName(encodingName);
        } catch (Exception e) {
            log.warn(e);
            charset = Charset.defaultCharset();
        }

        attribute.setTransformHandler(new BinaryValueHandler(attribute.getValueHandler(), charset, formatter));
    }

    private class BinaryValueHandler extends ProxyValueHandler {
        private final Charset charset;
        private final DBDBinaryFormatter formatter;
        public BinaryValueHandler(DBDValueHandler target, Charset charset, DBDBinaryFormatter formatter) {
            super(target);
            this.charset = charset;
            this.formatter = formatter;
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (value == null) {
                return super.getValueDisplayString(column, null, format);
            }
            ByteBuffer bb = charset.encode(CommonUtils.toString(value));
            byte[] bytes = bb.array();
            return formatter.toString(bytes, 0, bytes.length);
        }
    }

}
