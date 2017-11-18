/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.runtime.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.IParameterValues;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;

//FIXME: AF: to be moved to org.jkiss.dbeaver.runtime.core bundle
public class ContentTypeParameterValues implements IParameterValues {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Map getParameterValues()
    {
        final Map values = new HashMap();
        IContentType[] allContentTypes = Platform.getContentTypeManager().getAllContentTypes();
        for (IContentType contentType : allContentTypes) {
            values.put(contentType.getName(), contentType.getId());
        }
        return values;
    }

}
