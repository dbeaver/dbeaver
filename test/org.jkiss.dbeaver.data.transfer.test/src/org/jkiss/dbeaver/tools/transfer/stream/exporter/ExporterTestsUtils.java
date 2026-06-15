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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExporterTestsUtils {

    private ExporterTestsUtils() {
    }

    @NotNull
    public static IStreamDataExporterSite getIStreamDataExporterSiteMock(
        @NotNull String tableName,
        @NotNull String columnName,
        @NotNull StringWriter stringWriter,
        @NotNull String outputEncoding
    ) {
        DBPNamedObject mockSource = mock(DBPNamedObject.class);
        when(mockSource.getName()).thenReturn(tableName);

        DBDValueHandler valueHandler = ExporterTestsUtils.getDbdValueHandlerMock();

        DBDAttributeBinding mockBinding = mock(DBDAttributeBinding.class);
        when(mockBinding.getName()).thenReturn(columnName);
        when(mockBinding.getValueHandler()).thenReturn(mock(DBDValueHandler.class));
        when(mockBinding.getValueHandler()).thenReturn(valueHandler);


        PrintWriter pw = new PrintWriter(stringWriter);

        IStreamDataExporterSite mockSite = mock(IStreamDataExporterSite.class);
        when(mockSite.getOutputEncoding()).thenReturn(outputEncoding);
        when(mockSite.getWriter()).thenReturn(pw);
        when(mockSite.getAttributes()).thenReturn(new DBDAttributeBinding[]{mockBinding});
        when(mockSite.getSource()).thenReturn(mockSource);
        return mockSite;
    }

    @NotNull
    public static DBDValueHandler getDbdValueHandlerMock() {
        return new DBDValueHandler() {
            @NotNull
            @Override
            public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
                return String.class;
            }

            @Nullable
            @Override
            public String getValueContentType(@NotNull DBSTypedObject attribute) {
                return null;
            }

            @Nullable
            @Override
            public Object fetchValueObject(
                @NotNull DBCSession session,
                @NotNull DBCResultSet resultSet,
                @NotNull DBSTypedObject type,
                int index
            ) throws DBCException {
                return null;
            }

            @Override
            public void bindValueObject(
                @NotNull DBCSession session,
                @NotNull DBCStatement statement,
                @NotNull DBSTypedObject type,
                int index,
                @Nullable Object value
            ) throws DBCException {

            }

            @Nullable
            @Override
            public Object getValueFromObject(
                @NotNull DBCSession session,
                @NotNull DBSTypedObject type,
                @Nullable Object object,
                boolean copy,
                boolean validateValue
            ) throws DBCException {
                return object;
            }

            @Nullable
            @Override
            public Object createNewValueObject(@NotNull DBCSession session, @NotNull DBSTypedObject type) throws DBCException {
                return null;
            }

            @Override
            public void releaseValueObject(@Nullable Object value) {

            }

            @NotNull
            @Override
            public DBCLogicalOperator[] getSupportedOperators(@NotNull DBSTypedObject attribute) {
                return new DBCLogicalOperator[0];
            }

            @NotNull
            @Override
            public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
                return value == null ? "" : value.toString();
            }
        };
    }
}
