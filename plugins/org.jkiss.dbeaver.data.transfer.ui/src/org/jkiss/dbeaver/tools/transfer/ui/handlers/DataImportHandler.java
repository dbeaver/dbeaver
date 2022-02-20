/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.core.resources.IFile;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamEntityMapping;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class DataImportHandler extends DataTransferHandler {

    private static final Log log = Log.getLog(DataImportHandler.class);

    @Override
    protected IDataTransferNode<?> adaptTransferNode(Object object)
    {
        final DBSDataManipulator adapted = RuntimeUtils.getObjectAdapter(object, DBSDataManipulator.class);
        if (adapted != null) {
            return new DatabaseTransferConsumer(adapted);
        } else {
            IFile file = RuntimeUtils.getObjectAdapter(object, IFile.class);
            if (file != null) {
                return getNodeByFile(file);
            }
            DBSObjectContainer objectContainer = RuntimeUtils.getObjectAdapter(object, DBSObjectContainer.class);
            if (objectContainer == null) {
                if (object instanceof DBSWrapper) {
                    object = ((DBSWrapper) object).getObject();
                }
                if (object instanceof DBPObject) {
                    object = DBUtils.getPublicObject((DBSObject) object);
                }
                if (object instanceof DBSObjectContainer) {
                    objectContainer = (DBSObjectContainer) object;
                }
            }

            if (objectContainer != null) {
                if (isObjectContainerSupportsImport(objectContainer)) {
                    return new DatabaseTransferConsumer(objectContainer);
                } else {
                    DBWorkbench.getPlatformUI().showError("Wrong container", objectContainer.getName() + " doesn't support direct data import");
                }
            }
            return null;
        }
    }

    private IDataTransferNode getNodeByFile(IFile file) {
        DataTransferProcessorDescriptor processor = getProcessorByFile(file);
        if (processor != null) {
            return new StreamTransferProducer(
                new StreamEntityMapping(file.getFullPath().toFile()),
                processor);
        }
        return null;
    }

    private DataTransferProcessorDescriptor getProcessorByFile(IFile file) {
        String extension = file.getFileExtension();
        if (CommonUtils.isEmpty(extension)) {
            return null;
        }
        extension = extension.toLowerCase(Locale.ENGLISH);
        DataTransferNodeDescriptor producerDesc = DataTransferRegistry.getInstance().getNodeById(StreamTransferProducer.NODE_ID);
        if (producerDesc != null) {
            for (DataTransferProcessorDescriptor processor :  producerDesc.getProcessors()) {
                DBPPropertyDescriptor extList = processor.getProperty("extension");
                if (extList == null) {
                    continue;
                }
                String[] defExtensions = CommonUtils.split(CommonUtils.toString(extList.getDefaultValue()), ",");
                if (ArrayUtils.contains(defExtensions, extension)) {
                    return processor;
                }
            }
        }
        return null;
    }

    public static boolean isObjectContainerSupportsImport(DBSObjectContainer object) {
        try {
            Class<? extends DBSObject> childType = object.getPrimaryChildType(null);
            return DBSDataContainer.class.isAssignableFrom(childType);
        } catch (DBException e) {
            log.error(e);
        }
        return false;
    }

}