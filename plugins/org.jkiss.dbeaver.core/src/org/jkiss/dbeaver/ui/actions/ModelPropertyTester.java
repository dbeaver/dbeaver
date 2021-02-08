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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

/**
 * ModelPropertyTester
 */
public class ModelPropertyTester extends PropertyTester
{
    private static final Log log = Log.getLog(ModelPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.model";
    public static final String PROP_CHILD_OF_TYPE = "childOfType";
    public static final String PROP_IS_TABLE_CONTAINER = "isTableContainer";

    public ModelPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        if (!(receiver instanceof DBSObject)) {
            return false;
        }
        Display display = Display.getCurrent();
        if (display == null) {
            return false;
        }

        switch (property) {
            case PROP_CHILD_OF_TYPE: {
                {
                    DBSObject object = (DBSObject) receiver;
                    if (object instanceof DBSObjectContainer) {
                        if (expectedValue instanceof String) {
                            try {
                                Class<?> expectedChildClass = Class.forName((String)expectedValue);
                                Class<? extends DBSObject> childType = ((DBSObjectContainer)object).getPrimaryChildType(null);
                                return expectedChildClass.isAssignableFrom(childType);
                            } catch (Exception e) {
                                return false;
                            }
                        }
                    }
                }
                return false;
            }
            case PROP_IS_TABLE_CONTAINER: {
                DBSObject object = DBUtils.getPublicObject((DBSObject) receiver);
                if (object instanceof DBNContainer) {
                    Object valueObject = ((DBNContainer) object).getValueObject();
                    if (valueObject instanceof DBSObject) {
                        object = (DBSObject) valueObject;
                    }
                }
                if (object instanceof DBSSchema) {
                    return true;
                }
                if (object instanceof DBSObjectContainer) {
                    try {
                        Class<? extends DBSObject> primaryChildType = ((DBSObjectContainer)object).getPrimaryChildType(null);
                        if (DBSDataContainer.class.isAssignableFrom(primaryChildType)) {
                            return true;
                        }
                    } catch (DBException e) {
                        log.debug(e);
                    }
                }

                return false;
            }

        }
        return false;
    }

}
