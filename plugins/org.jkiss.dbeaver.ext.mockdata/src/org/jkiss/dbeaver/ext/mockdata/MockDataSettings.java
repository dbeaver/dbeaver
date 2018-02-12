/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorRegistry;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MockDataSettings {

    public static final String FK_GENERATOR_ID = "fkGenerator"; //$NON-NLS-1$

    private DBSEntity dbsEntity;
    private Collection<DBSAttributeBase> attributes;

    private boolean removeOldData;
    private long rowsNumber = 1000;

    private Map<String, MockGeneratorDescriptor> generatorDescriptors = new HashMap<>(); // generatorId -> MockGeneratorDescriptor
    private Map<String, AttributeGeneratorProperties> attributeGenerators = new HashMap<>(); // attribute.name -> generators properties

    // populate attribute generators properties map
    public void init(MockDataExecuteWizard wizard) throws DBException {
        List<DBSDataManipulator> databaseObjects = wizard.getDatabaseObjects();
        DBSDataManipulator dataManipulator = databaseObjects.iterator().next(); // TODO only the first
        dbsEntity = (DBSEntity) dataManipulator;
        attributes = new ArrayList<>();

        try {
            DBeaverUI.run(wizard.getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        attributes.addAll(DBUtils.getRealAttributes(dbsEntity.getAttributes(monitor)));

                        MockGeneratorRegistry generatorRegistry = MockGeneratorRegistry.getInstance();
                        for (DBSAttributeBase attribute : attributes) {
                            AttributeGeneratorProperties generatorProperties = new AttributeGeneratorProperties(attribute);
                            attributeGenerators.put(attribute.getName(), generatorProperties);

                            //((JDBCColumnKeyType) attribute).isInUniqueKey()
                            List<DBSEntityReferrer> attributeReferrers = DBUtils.getAttributeReferrers(monitor, (DBSEntityAttribute) attribute);
                            if (!CommonUtils.isEmpty(attributeReferrers)) {
                                MockGeneratorDescriptor generator = generatorRegistry.getGenerator(FK_GENERATOR_ID);
                                putGenerator(generatorProperties, generator);
                            } else {
                                List<MockGeneratorDescriptor> generators = generatorRegistry.findAllGenerators(dataManipulator.getDataSource(), attribute);
                                for (MockGeneratorDescriptor generator : generators) {
                                    putGenerator(generatorProperties, generator);
                                }
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError("Transfer init failed", "Can't start data transfer", e.getTargetException());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Collection<DBSAttributeBase> getAttributes() {
        return attributes;
    }

    private void putGenerator(AttributeGeneratorProperties generatorProperties, MockGeneratorDescriptor generator) {
        generatorDescriptors.put(generator.getId(), generator);
        generatorProperties.putGeneratorPropertySource(generator.getId(), new PropertySourceCustom(generator.getProperties(), null));
    }

    public MockGeneratorDescriptor getGeneratorDescriptor(String generatorId) {
        return generatorDescriptors.get(generatorId);
    }

    public MockGeneratorDescriptor findGeneratorForName(DBSAttributeBase attribute, String generatorName) {
        AttributeGeneratorProperties attributeGeneratorProperties = attributeGenerators.get(attribute.getName());
        for (String generatorId : attributeGeneratorProperties.getGenerators()) {
            MockGeneratorDescriptor generatorDescriptor = generatorDescriptors.get(generatorId);
            if (generatorName.equals(generatorDescriptor.getLabel())) {
                return generatorDescriptor;
            }
        }
        return null;
    }

    public boolean isRemoveOldData() {
        return removeOldData;
    }

    public void setRemoveOldData(boolean removeOldData) {
        this.removeOldData = removeOldData;
    }

    public long getRowsNumber() {
        return rowsNumber;
    }

    public void setRowsNumber(long rowsNumber) {
        this.rowsNumber = rowsNumber;
    }

    public Map<String, AttributeGeneratorProperties> getAttributeGenerators() {
        return attributeGenerators;
    }

    public AttributeGeneratorProperties getAttributeGeneratorProperties(DBSAttributeBase attribute) {
        return attributeGenerators.get(attribute.getName());
    }

    public void loadFrom(IRunnableContext runnableContext, IDialogSettings dialogSettings) {
        removeOldData = dialogSettings.getBoolean("removeOldData");
        try {
            rowsNumber = dialogSettings.getInt("rowsNumber");
        } catch (NumberFormatException e) {
            // do nothing
        }

        // load selected generators
        IDialogSettings tableSection = UIUtils.getSettingsSection(dialogSettings, dbsEntity.getName());
        for (Map.Entry<String, AttributeGeneratorProperties> entry : attributeGenerators.entrySet()) {
            String attributeName = entry.getKey();
            String selectedGenerator = tableSection.get(attributeName);
            if (selectedGenerator != null) {
                entry.getValue().setSelectedGeneratorId(selectedGenerator);
            }
        }
    }

    void saveTo(IDialogSettings dialogSettings) {
        dialogSettings.put("removeOldData", removeOldData);
        dialogSettings.put("rowsNumber", rowsNumber);

        // save selected generators
        IDialogSettings tableSection = UIUtils.getSettingsSection(dialogSettings, dbsEntity.getName());
        for (Map.Entry<String, AttributeGeneratorProperties> entry : attributeGenerators.entrySet()) {
            tableSection.put(entry.getKey(), entry.getValue().getSelectedGeneratorId());
        }

    }

    public static class AttributeGeneratorProperties {
        private final DBSAttributeBase attribute;
        private String selectedGeneratorId = null; // id
        private Map<String, PropertySourceCustom> generators = new HashMap<>(); // generatorId -> PropertySourceCustom
        public AttributeGeneratorProperties(DBSAttributeBase attribute) {
            this.attribute = attribute;
        }
        public DBSAttributeBase getAttribute() { return attribute; }

        public String getSelectedGeneratorId() {
            if (selectedGeneratorId == null && !CommonUtils.isEmpty(getGenerators())) {
                selectedGeneratorId = getGenerators().iterator().next();
            }
            return selectedGeneratorId;
        }

        public Set<String> getGenerators() {
            return generators.keySet();
        }

        public String setSelectedGeneratorId(String selectedGeneratorId) {
            if (selectedGeneratorId == null && !CommonUtils.isEmpty(getGenerators())) {
                selectedGeneratorId = getGenerators().iterator().next();
            }
            this.selectedGeneratorId = selectedGeneratorId;
            return selectedGeneratorId;
        }

        public void putGeneratorPropertySource(String generatorId, PropertySourceCustom propertySource) {
            generators.put(generatorId, propertySource);
        }

        public PropertySourceCustom getGeneratorPropertySource(String generatorId) {
            if (generatorId == null) {
                generatorId = getSelectedGeneratorId();
            }
            return generators.get(generatorId);
        }

        public boolean isEmpty() {
            return generators.isEmpty();
        }
    }
}
