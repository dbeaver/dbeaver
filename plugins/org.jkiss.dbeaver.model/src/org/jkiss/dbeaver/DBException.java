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

package org.jkiss.dbeaver;

import org.jkiss.utils.CommonUtils;

/**
 * DBException
 */
public class DBException extends Exception {
    private static final long serialVersionUID = 1L;

    public static final int ERROR_CODE_NONE = -1;

    public DBException(String message) {
        super(message);
    }

    public DBException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBException dbe) {
            if (obj == this) {
                return true;
            }
            Throwable ex1 = dbe;
            Throwable ex2 = this;
            while (ex1 != null) {
                if (!CommonUtils.equalObjects(ex1.getMessage(), ex2.getMessage())) {
                    return false;
                }
                ex1 = ex1.getCause();
                ex2 = ex2.getCause();
                if ((ex1 == null && ex2 != null) || (ex2 == null && ex1 != null)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

}
