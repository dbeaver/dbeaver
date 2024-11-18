/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.kingbase.model.impls;

import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSetting;

/**
 * KingbaseServerKingbaseSQL
 */
public class KingbaseServerKingbaseSQL extends KingbaseServerExtensionBase {
    public static final String TYPE_ID = "kingbase";

    public KingbaseServerKingbaseSQL(KingbaseDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public boolean supportsEntityMetadataInResults() {
        return true;
    }

    @Override
    public String getServerTypeName() {
        return "Kingbase";
    }

    @Override
    public boolean supportsKBConstraintExpressionColumn() {
        return true;
    }

    @Override
    public boolean supportsHasOidsColumn() {
        return false;
    }


    @Override
    public boolean supportsDatabaseSize() {
        return true;
    }

    @Override
    public boolean supportsBackslashStringEscape() {
        final KingbaseSetting setting = dataSource.getSetting(KingbaseConstants.OPTION_STANDARD_CONFORMING_STRINGS);
        return setting != null && "off".equals(setting.getValue());
    }

    @Override
    public boolean supportsDisablingAllTriggers() {
        return true;
    }

    @Override
    public boolean supportsGeneratedColumns() {
        return true;
    }

    @Override
    public boolean supportsKeyAndIndexRename() {
        return true;
    }

    @Override
    public boolean supportsAlterUserChangePassword() {
        return true;
    }

    @Override
    public boolean supportsCopyFromStdIn() {
        return true;
    }
}
