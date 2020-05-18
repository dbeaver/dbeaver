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

package org.jkiss.dbeaver.ui.editors.sql.convert.impl;

import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * CPPSQLConverter
 */
public class CPPSQLConverter extends SourceCodeSQLConverter {

    @Override
    protected void convertSourceLines(StringBuilder result, String[] sourceLines, String lineDelimiter, Map<String, Object> options) {
        for (int i = 0; i < sourceLines.length; i++) {
            String line = sourceLines[i];
            result.append('"').append(CommonUtils.escapeJavaString(line)).append(lineDelimiter).append('"');
            if (i < sourceLines.length - 1) {
                result.append("\n");
            } else {
                result.append(";");
            }
        }
    }
}
