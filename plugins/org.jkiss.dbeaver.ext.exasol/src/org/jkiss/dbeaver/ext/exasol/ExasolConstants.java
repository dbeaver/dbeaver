/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;

/**
 * Exasol constants
 *
 * @author Karl Griesser
 */
public class ExasolConstants {

    // Display Categories
    public static final String CAT_AUTH = "Authorities";
    public static final String CAT_BASEOBJECT = "Base Object";
    public static final String CAT_DATETIME = "Date & Time";
    public static final String CAT_OWNER = "Owner";
    public static final String CAT_SOURCE = "Source";
    public static final String CAT_PERFORMANCE = "Performance";

    public static final String DRV_CLIENT_NAME = "clientname";
    public static final String DRV_CLIENT_VERSION = "clientversion";
    public static final String DRV_QUERYTIMEOUT = "querytimeout";
    public static final String DRV_CONNECT_TIMEOUT = "connecttimeout";
    public static final String DRV_BACKUP_HOST_LIST = DBConstants.INTERNAL_PROP_PREFIX + "backupHostList";
    public static final String DRV_USE_BACKUP_HOST_LIST = DBConstants.INTERNAL_PROP_PREFIX + "useBackupHostList";
    public static final String CONSUMER_GROUP_CLASS = "org.jkiss.dbeaver.ext.exasol.model.ExasolConsumerGroup";
    public static final String PRIORITY_GROUP_CLASS = "org.jkiss.dbeaver.ext.exasol.model.ExasolPriorityGroup";


    public static final DBDPseudoAttribute PSEUDO_ATTR_ROWID = new DBDPseudoAttribute(
            DBDPseudoAttributeType.ROWID,
            "ROWID",
            "$alias.ROWID",
            null,
            "Unique row identifier",
            true);

    public static final String TYPE_GEOMETRY = "GEOMETRY";
    public static final String TYPE_DECIMAL = "DECIMAL";
    public static final String TYPE_VARCHAR = "VARCHAR";
    public static final String TYPE_CHAR = "CHAR";
    public static final String TYPE_HASHTYPE = "HASHTYPE";

    public static final String KEYWORD_ENABLE = "ENABLE";
    public static final String KEYWORD_DISABLE = "DISABLE";
}
