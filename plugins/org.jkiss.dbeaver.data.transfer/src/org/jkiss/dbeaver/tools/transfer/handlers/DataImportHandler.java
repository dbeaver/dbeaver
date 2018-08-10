/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferProducer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.Map;

public class DataImportHandler extends DataTransferHandler implements IElementUpdater {

    @Override
    protected IDataTransferNode adaptTransferNode(Object object)
    {
        final DBSDataManipulator adapted = RuntimeUtils.getObjectAdapter(object, DBSDataManipulator.class);
        if (adapted != null) {
            return new DatabaseTransferConsumer(adapted);
        } else {
            IFile file = RuntimeUtils.getObjectAdapter(object, IFile.class);
            if (file != null) {
                return importDataFromFile(file);
            }
            return null;
        }
    }

    private IDataTransferNode importDataFromFile(IFile file) {
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
                String[] defExtensions = CommonUtils.toString(extList.getDefaultValue()).split(",");
                if (ArrayUtils.contains(defExtensions, extension)) {
                    return importDataFromFile(file, producerDesc, processor);
                }
            }
        }
        return null;
    }

    private IDataTransferNode importDataFromFile(IFile file, DataTransferNodeDescriptor producer, DataTransferProcessorDescriptor processor) {
        return new StreamTransferProducer(file.getFullPath().toFile());
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider != null && selectionProvider.getSelection() instanceof IStructuredSelection) {
            Object selectedObject = ((IStructuredSelection)selectionProvider.getSelection()).getFirstElement();
            if (selectedObject instanceof DBNNode) {
                element.setText("Import " + ((DBNNode) selectedObject).getNodeType() + " Data");
            }
        }
    }

}