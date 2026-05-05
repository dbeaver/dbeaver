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

package org.jkiss.dbeaver.model.sql.translate;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;

/**
 * SQL translator
 */
final class SQLTranslateContext {

    @NotNull
    private final SQLDialect sourceDialect;
    @NotNull
    private final SQLDialect targetDialect;
    @NotNull
    private final DBPPreferenceStore preferenceStore;
    @NotNull
    private final SQLSyntaxManager syntaxManager;

    public SQLTranslateContext(
        @NotNull SQLDialect sourceDialect,
        @NotNull SQLDialect targetDialect,
        @NotNull DBPPreferenceStore preferenceStore
    ) {
        this.sourceDialect = sourceDialect;
        this.targetDialect = targetDialect;
        this.preferenceStore = preferenceStore;

        syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(targetDialect, preferenceStore);
    }

    @NotNull
    public SQLDialect getSourceDialect() {
        return sourceDialect;
    }

    @NotNull
    public SQLDialect getTargetDialect() {
        return targetDialect;
    }

    @NotNull
    public DBPPreferenceStore getPreferenceStore() {
        return preferenceStore;
    }

    @NotNull
    public SQLSyntaxManager getSyntaxManager() {
        return syntaxManager;
    }

}
