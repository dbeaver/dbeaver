/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.cli.model.option;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;

public class AbstractFileOption {
    private static final Log log = Log.getLog(AbstractFileOption.class);


    @Nullable
    protected Path getPath(@Nullable String filePath) {
        if (CommonUtils.isEmpty(filePath)) {
            return null;
        }
        try {
            return DBFUtils.getPathFromURI(filePath);
        } catch (DBException e) {
            log.error("Error getting path from URI: " + filePath + " " + e.getMessage(), e);
        }
        return null;
    }
}
