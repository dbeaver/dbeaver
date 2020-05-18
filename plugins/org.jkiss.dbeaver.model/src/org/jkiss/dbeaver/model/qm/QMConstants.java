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

package org.jkiss.dbeaver.model.qm;

/**
 * QM log constants
 */
public class QMConstants {

    public static final String PROP_PREFIX = "qm.";

    public static final String PROP_OBJECT_TYPES = PROP_PREFIX + "objectTypes";
    public static final String PROP_QUERY_TYPES = PROP_PREFIX + "queryTypes";
    public static final String PROP_ENTRIES_PER_PAGE = PROP_PREFIX + "maxEntries";
    public static final String PROP_HISTORY_DAYS = PROP_PREFIX + "historyDays";
    public static final String PROP_STORE_LOG_FILE = PROP_PREFIX + "storeLogs";
    public static final String PROP_LOG_DIRECTORY = PROP_PREFIX + "logDirectory";

}
