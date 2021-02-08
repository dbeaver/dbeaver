/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;

import java.util.List;

/**
 * SQLDialectMetadata
 */
public interface SQLDialectMetadata {

    String getId();

    String getLabel();

    String getDescription();

    DBPImage getIcon();

    boolean isAbstract();

    boolean isHidden();

    @NotNull
    SQLDialect createInstance() throws DBException;

    @NotNull
    List<String> getReservedWords();

    @NotNull
    List<String> getDataTypes();

    @NotNull
    List<String> getFunctions();

    @NotNull
    List<String> getDDLKeywords();

    @NotNull
    List<String> getDMLKeywords();

    @NotNull
    List<String> getExecuteKeywords();

    @NotNull
    List<String> getTransactionKeywords();

    @NotNull
    String getScriptDelimiter();

    Object getProperty(String name);

    @Nullable
    SQLDialectMetadata getParentDialect();

    @NotNull
    List<SQLDialectMetadata> getSubDialects(boolean addNested);

    DBDInsertReplaceMethod[] getSupportedInsertReplaceMethods();

}
