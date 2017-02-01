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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.jkiss.dbeaver.ui.editors.EditorUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * SQL content type describer
 */
public class SQLContentTypeDescriber implements IContentDescriber {

    @Override
    public int describe(InputStream contents, IContentDescription description) throws IOException
    {
        return INDETERMINATE;
    }

    @Override
    public QualifiedName[] getSupportedOptions()
    {
        return new QualifiedName[] { EditorUtils.QN_DATA_SOURCE_ID};
    }
}
