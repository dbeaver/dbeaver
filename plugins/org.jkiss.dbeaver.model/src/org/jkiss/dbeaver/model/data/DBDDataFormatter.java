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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.Nullable;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Data formatter
 */
public interface DBDDataFormatter {

    String TYPE_NAME_NUMBER = "number"; //$NON-NLS-1$
    String TYPE_NAME_DATE = "date"; //$NON-NLS-1$
    String TYPE_NAME_TIME = "time"; //$NON-NLS-1$
    String TYPE_NAME_TIMESTAMP = "timestamp"; //$NON-NLS-1$

    void init(Locale locale, Map<Object, Object> properties);

    @Nullable
    String getPattern();

    @Nullable
    String formatValue(Object value);

    @Nullable
    Object parseValue(String value, @Nullable Class<?> typeHint) throws ParseException;
    
}
