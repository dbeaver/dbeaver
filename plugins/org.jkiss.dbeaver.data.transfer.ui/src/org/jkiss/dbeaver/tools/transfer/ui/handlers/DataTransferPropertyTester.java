/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.handlers;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.utils.CommonUtils;

/**
 * DataTransferPropertyTester
 */
public class DataTransferPropertyTester extends PropertyTester
{
    private static final Log log = Log.getLog(DataTransferPropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.tools.transfer";
    public static final String PROP_SUPPORTS_IMPORT_FROM = "supportsImportFrom";
    public static final String PROP_SUPPORTS_IMPORT_TO = "supportsImportTo";

    public DataTransferPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        if (!(receiver instanceof DBNNode)) {
            return false;
        }
        Display display = Display.getCurrent();
        if (display == null) {
            return false;
        }

        switch (property) {
            case PROP_SUPPORTS_IMPORT_FROM: {
                IFile inputFile = (IFile) receiver;
                String extension = inputFile.getFileExtension();
                if (!CommonUtils.isEmpty(extension)) {
                    DataTransferNodeDescriptor producerDesc = DataTransferRegistry.getInstance().getNodeById(StreamTransferProducer.NODE_ID);
                    if (producerDesc != null) {
                        for (DataTransferProcessorDescriptor processor : producerDesc.getProcessors()) {

                        }
                    }
                }
                return false;
            }
            case PROP_SUPPORTS_IMPORT_TO: {
                if (receiver instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) receiver).getObject();
                    if (object instanceof DBSObjectContainer) {
                        return isObjectContainerSupportsImport((DBSObjectContainer) object);
                    }
                }
                return false;
            }
        }
        return false;
    }

    public static boolean isObjectContainerSupportsImport(DBSObjectContainer object) {
        try {
            Class<? extends DBSObject> childType = object.getChildType(new VoidProgressMonitor());
            return DBSDataContainer.class.isAssignableFrom(childType);
        } catch (DBException e) {
            log.error(e);
        }
        return false;
    }

}
