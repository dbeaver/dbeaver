/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class AltibaseUtils {

    private static final Log log = Log.getLog(AltibaseUtils.class);

    static <PARENT extends DBSObject> Object resolveLazyReference(
            DBRProgressMonitor monitor,
            PARENT parent,
            DBSObjectCache<PARENT, ?> cache,
            DBSObjectLazy<?> referrer,
            Object propertyId) throws DBException {

        final Object reference = referrer.getLazyReference(propertyId);

        if (reference instanceof String) {
            Object object;
            if (monitor != null) {
                object = cache.getObject(
                        monitor,
                        parent,
                        (String) reference);
            } else {
                object = cache.getCachedObject((String) reference);
            }

            if (object != null) {
                return object;
            } else {
                log.warn("Object '" + reference + "' not found");
                return reference;
            }
        } else {
            return reference;
        }
    }
}
