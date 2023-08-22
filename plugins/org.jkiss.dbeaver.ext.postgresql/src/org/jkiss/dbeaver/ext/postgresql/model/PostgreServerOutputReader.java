/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.output.DBCOutputSeverity;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReaderExt;
import org.jkiss.dbeaver.model.impl.AsyncServerOutputReader;
import org.jkiss.utils.BeanUtils;

import java.util.Map;

public class PostgreServerOutputReader extends AsyncServerOutputReader implements DBCServerOutputReaderExt {
    private static final String PSQL_WARNING_CLASS = "org.postgresql.util.PSQLWarning";
    private static final String PSQL_WARNING_GET_SERVER_ERROR_MESSAGE_METHOD = "getServerErrorMessage";
    private static final String SERVER_ERROR_MESSAGE_MESSAGE_PARTS = "mesgParts";
    private static final Character SERVER_ERROR_MESSAGE_SEVERITY_LOCALIZED = 'S';
    private static final Character SERVER_ERROR_MESSAGE_SEVERITY = 'V';

    @NotNull
    @Override
    public DBCOutputSeverity[] getSupportedSeverities(@NotNull DBCExecutionContext context) {
        return PostgreOutputSeverity.values();
    }

    @Override
    protected void dumpWarning(@NotNull DBCOutputWriter output, @NotNull Throwable warning) {
        output.println(getSeverity(warning), warning.getMessage());
    }

    /**
     * Retrieves severity of the warning.
     *
     * @see <a href="https://www.postgresql.org/docs/current/protocol-error-fields.html">55.8. Error and Notice Message Fields</a>
     */
    @Nullable
    private static DBCOutputSeverity getSeverity(@NotNull Throwable warning) {
        if (!PSQL_WARNING_CLASS.equals(warning.getClass().getName())) {
            return null;
        }
        try {
            final Object obj = BeanUtils.invokeObjectMethod(warning, PSQL_WARNING_GET_SERVER_ERROR_MESSAGE_METHOD);
            final Map<Character, String> parts = BeanUtils.getFieldValue(obj, SERVER_ERROR_MESSAGE_MESSAGE_PARTS);
            final String severity;
            if (parts.containsKey(SERVER_ERROR_MESSAGE_SEVERITY)) {
                severity = parts.get(SERVER_ERROR_MESSAGE_SEVERITY);
            } else {
                severity = parts.get(SERVER_ERROR_MESSAGE_SEVERITY_LOCALIZED);
            }
            return PostgreOutputSeverity.valueOf(severity);
        } catch (Throwable ignored) {
            return null;
        }
    }


    private enum PostgreOutputSeverity implements DBCOutputSeverity {
        DEBUG("Debug"),
        LOG("Log"),
        INFO("Info"),
        NOTICE("Notice"),
        WARNING("Warning"),
        ERROR("Error");

        private final String name;

        PostgreOutputSeverity(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }
    }
}
