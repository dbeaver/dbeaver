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

package org.jkiss.dbeaver.model;

import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * Database keyword type
 */
public enum DBPIdentifierCase {
    UPPER {
        @Override
        public String transform(String value)
        {
            return value.toUpperCase(Locale.ENGLISH);
        }
    },
    LOWER {
        @Override
        public String transform(String value)
        {
            return value.toLowerCase(Locale.ENGLISH);
        }
    },
    MIXED {
        @Override
        public String transform(String value)
        {
            return value;
        }
    };

    public static String capitalizeCaseName(String name) {
        return CommonUtils.capitalizeWord(name.toLowerCase(Locale.ENGLISH));
    }

    public abstract String transform(String value);

}
