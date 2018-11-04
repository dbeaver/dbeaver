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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorRegistry;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class MockDataSettings {

    public static final String PROP_REMOVE_OLD_DATA = "removeOldData"; //$NON-NLS-1$
    public static final String PROP_ROWS_NUMBER = "rowsNumber"; //$NON-NLS-1$
    public static final String PROP_BATCH_SIZE = "batchSize"; //$NON-NLS-1$

    public static final String KEY_SELECTED_ATTRIBUTE = "selectedAttribute"; //$NON-NLS-1$
    public static final String KEY_SELECTED_GENERATOR = "selectedGenerator"; //$NON-NLS-1$
    public static final String KEY_PRESET_ID = "presetId"; //$NON-NLS-1$
    public static final String KEY_GENERATOR_SECTION = "GENERATOR_SECTION"; //$NON-NLS-1$

    static final String NO_GENERATOR_ID = "<no generator>";

    static final String NO_GENERATOR_LABEL = MockDataMessages.tools_mockdata_attribute_generator_skip;

    private DBSEntity entity;
    private Collection<DBSAttributeBase> attributes;
    private DBRProgressMonitor monitor;

    private boolean removeOldData;
    private long rowsNumber = 1000;
    private int batchSize = 200;

    private String selectedAttribute; // attribute.name
    private Map<String, MockGeneratorDescriptor> generatorDescriptors = new HashMap<>(); // generatorId -> MockGeneratorDescriptor
    private Map<String, AttributeGeneratorProperties> attributeGenerators = new HashMap<>(); // attribute.name -> generators properties

    // populate attribute generators properties map
    public void init(DBRProgressMonitor monitor, MockDataExecuteWizard wizard) throws DBException {
        this.monitor = monitor;

        List<DBSDataManipulator> databaseObjects = wizard.getDatabaseObjects();
        DBSDataManipulator dataManipulator = databaseObjects.iterator().next(); // TODO only the first
        entity = (DBSEntity) dataManipulator;
        attributes = new ArrayList<>();

        for (DBSAttributeBase attr : CommonUtils.safeCollection(entity.getAttributes(monitor))) {
            if (DBUtils.isPseudoAttribute(attr) || DBUtils.isHiddenObject(attr)) {
                continue;
            }
            attributes.add(attr);
        }

        MockGeneratorRegistry generatorRegistry = MockGeneratorRegistry.getInstance();
        for (DBSAttributeBase attribute : attributes) {
            AttributeGeneratorProperties generatorProperties = new AttributeGeneratorProperties(attribute);
            attributeGenerators.put(attribute.getName(), generatorProperties);

            //((JDBCColumnKeyType) attribute).isInUniqueKey()
            List<DBSEntityReferrer> attributeReferrers = DBUtils.getAttributeReferrers(monitor, (DBSEntityAttribute) attribute);
            if (!CommonUtils.isEmpty(attributeReferrers)) {
                MockGeneratorDescriptor generator = generatorRegistry.getGenerator(MockGeneratorRegistry.FK_GENERATOR_ID);
                putGenerator(generatorProperties, generator);
            } else {
                List<MockGeneratorDescriptor> generators = generatorRegistry.findAllGenerators(dataManipulator.getDataSource(), attribute);
                for (MockGeneratorDescriptor generator : generators) {
                    putGenerator(generatorProperties, generator);
                }
            }
        }
    }

    public DBSEntity getEntity() {
        return entity;
    }

    public Collection<DBSAttributeBase> getAttributes() {
        return attributes;
    }

    public String getSelectedAttribute() {
        return selectedAttribute;
    }

    public void setSelectedAttribute(String selectedAttribute) {
        this.selectedAttribute = selectedAttribute;
    }

    private void putGenerator(AttributeGeneratorProperties generatorProperties, MockGeneratorDescriptor generator) {
        generatorDescriptors.put(generator.getId(), generator);
        generatorProperties.putGeneratorProperties(generator.getId(), new PropertySourceCustom(generator.getProperties(), null));
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

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Map<String, AttributeGeneratorProperties> getAttributeGenerators() {
        return attributeGenerators;
    }

    public AttributeGeneratorProperties getAttributeGeneratorProperties(DBSAttributeBase attribute) {
        return attributeGenerators.get(attribute.getName());
    }

    public DBRProgressMonitor getMonitor() {
        return monitor;
    }

    public void loadFrom(IDialogSettings dialogSettings) {
        removeOldData = dialogSettings.getBoolean(PROP_REMOVE_OLD_DATA);
        try {
            rowsNumber = dialogSettings.getInt(PROP_ROWS_NUMBER);
        } catch (NumberFormatException e) {
            // do nothing
        }
        try {
            batchSize = dialogSettings.getInt(PROP_BATCH_SIZE);
        } catch (NumberFormatException e) {
            // do nothing
        }

        // load selected generators
        selectedAttribute = dialogSettings.get(KEY_SELECTED_ATTRIBUTE);
        VoidProgressMonitor voidProgressMonitor = new VoidProgressMonitor();
        IDialogSettings tableSection = UIUtils.getSettingsSection(dialogSettings, entity.getName());
        for (Map.Entry<String, AttributeGeneratorProperties> entry : attributeGenerators.entrySet()) {

            // search the saved generator
            String attributeName = entry.getKey();
            IDialogSettings attributeSection = UIUtils.getSettingsSection(tableSection, attributeName);
            String savedGeneratorId = attributeSection.get(KEY_SELECTED_GENERATOR);
            AttributeGeneratorProperties attrGeneratorProperties = entry.getValue();

            // set the saved generator
            if (NO_GENERATOR_ID.equals(savedGeneratorId)) {
                attrGeneratorProperties.setSelectedGenerator(null);
                attrGeneratorProperties.setPresetId(null);
            } else if (!CommonUtils.isEmpty(savedGeneratorId)) {
                MockGeneratorDescriptor generatorDescriptor = attrGeneratorProperties.getGenerator(savedGeneratorId);
                if (generatorDescriptor != null) {
                    attrGeneratorProperties.setSelectedGenerator(generatorDescriptor);
                    attrGeneratorProperties.setPresetId(attributeSection.get(KEY_PRESET_ID));
                }

                PropertySourceCustom generatorPropertySource = attrGeneratorProperties.getGeneratorProperties();
                IDialogSettings generatorSection = UIUtils.getSettingsSection(attributeSection, KEY_GENERATOR_SECTION);
                if (generatorPropertySource != null) {
                    Map<Object, Object> properties = generatorPropertySource.getPropertiesWithDefaults();
                    for (Map.Entry<Object, Object> propEntry : properties.entrySet()) {
                        String key = (String) propEntry.getKey();
                        Object savedValue = UIUtils.getSectionValueWithType(generatorSection, key);
                        if (key.equals("nulls") && savedValue instanceof Boolean) {
                            continue; // skip incorrect type TODO can be removed in the future
                        }
                        generatorPropertySource.setPropertyValue(voidProgressMonitor, propEntry.getKey(), savedValue);
                    }
                }
            } else {
                // set the default generator
                autoAssignGenerator(attrGeneratorProperties);
            }
        }
    }

    public void autoAssignGenerator(AttributeGeneratorProperties attrGeneratorProperties) {
        DBSAttributeBase attribute = attrGeneratorProperties.getAttribute();
        if (attribute.isAutoGenerated()) {
            // Do not generate it by default
            attrGeneratorProperties.setSelectedGenerator(null);
            return;
        }
        String attributeName = attribute.getName().toLowerCase();
        Set<String> attrGeneratorIds = attrGeneratorProperties.getGenerators();
        boolean found = false;
        for (String generatorId : attrGeneratorIds) {
            MockGeneratorDescriptor generatorDescriptor = getGeneratorDescriptor(generatorId);
            for (String tag : generatorDescriptor.getTags()) {
                // find & set the appropriate generator
                if (attributeName.contains(tag)) {
                    attrGeneratorProperties.setSelectedGenerator(generatorDescriptor);
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
        }
        if (!found) {
            // set the default generator
            switch (attribute.getDataKind()) {
                case BOOLEAN:
                    setSelectedGenerator(attrGeneratorProperties, MockGeneratorDescriptor.BOOLEAN_RANDOM_GENERATOR_ID);
                    break;
                case DATETIME:
                    setSelectedGenerator(attrGeneratorProperties, MockGeneratorDescriptor.DATETIME_RANDOM_GENERATOR_ID);
                    break;
                case NUMERIC:
                    setSelectedGenerator(attrGeneratorProperties, MockGeneratorDescriptor.NUMERIC_RANDOM_GENERATOR_ID);
                    break;
                case STRING:
                    setSelectedGenerator(attrGeneratorProperties, MockGeneratorDescriptor.STRING_TEXT_GENERATOR_ID);
                    break;
            }
        }
    }

    // prevents set non-acceptable generator
    private void setSelectedGenerator(AttributeGeneratorProperties attrGeneratorProperties, String generatorId) {
        if (generatorId == null) {
            attrGeneratorProperties.setSelectedGenerator(null);
        } else {
            attrGeneratorProperties.setSelectedGenerator(attrGeneratorProperties.getGenerator(generatorId));
        }
    }

    void saveTo(IDialogSettings dialogSettings) {
        dialogSettings.put(PROP_REMOVE_OLD_DATA, removeOldData);
        dialogSettings.put(PROP_ROWS_NUMBER, rowsNumber);
        dialogSettings.put(PROP_BATCH_SIZE, batchSize);

        // save selected generators
        dialogSettings.put(KEY_SELECTED_ATTRIBUTE, selectedAttribute);
        IDialogSettings tableSection = UIUtils.getSettingsSection(dialogSettings, entity.getName());
        for (Map.Entry<String, AttributeGeneratorProperties> attrEntry : attributeGenerators.entrySet()) {
            String attributeName = attrEntry.getKey();

            AttributeGeneratorProperties attrGeneratorProperties = attrEntry.getValue();
            IDialogSettings attributeSection = UIUtils.getSettingsSection(tableSection, attributeName);
            MockGeneratorDescriptor selectedGenerator = attrGeneratorProperties.getSelectedGenerator();
            if (selectedGenerator == null) {
                attributeSection.put(KEY_SELECTED_GENERATOR, NO_GENERATOR_ID);
                attributeSection.put(KEY_PRESET_ID, (String)null);
            } else {
                attributeSection.put(KEY_SELECTED_GENERATOR, selectedGenerator.getId());
                attributeSection.put(KEY_PRESET_ID, attrGeneratorProperties.getPresetId());

                IDialogSettings generatorSection = UIUtils.getSettingsSection(attributeSection, KEY_GENERATOR_SECTION);
                PropertySourceCustom generatorPropertySource = attrGeneratorProperties.getGeneratorProperties();
                if (generatorPropertySource != null) {
                    Map<Object, Object> properties = generatorPropertySource.getPropertiesWithDefaults();
                    for (Map.Entry<Object, Object> propEntry : properties.entrySet()) {
                        UIUtils.putSectionValueWithType(generatorSection, (String) propEntry.getKey(), propEntry.getValue());
                    }
                }
            }
        }
    }

    public class AttributeGeneratorProperties {
        private final DBSAttributeBase attribute;
        private MockGeneratorDescriptor selectedGenerator = null;
        private String presetId = null;
        private Map<String, PropertySourceCustom> generators = new TreeMap<>(); // generatorId -> PropertySourceCustom

        public AttributeGeneratorProperties(DBSAttributeBase attribute) {
            this.attribute = attribute;
        }

        public DBSAttributeBase getAttribute() {
            return attribute;
        }

        public MockGeneratorDescriptor getSelectedGenerator() {
            return selectedGenerator;
        }

        public Set<String> getGenerators() {
            return generators.keySet();
        }

        public void setSelectedGenerator(MockGeneratorDescriptor generator) {
            this.selectedGenerator = generator;
        }

        public MockGeneratorDescriptor autoDetectGenerator() {
            if (!CommonUtils.isEmpty(getGenerators())) {
                String genId = getGenerators().iterator().next();
                selectedGenerator = generatorDescriptors.get(genId);
            }
            this.presetId = null;

            return selectedGenerator;
        }

        public void putGeneratorProperties(String generatorId, PropertySourceCustom propertySource) {
            generators.put(generatorId, propertySource);
        }

        public PropertySourceCustom getGeneratorProperties() {
            return selectedGenerator == null ? null : generators.get(selectedGenerator.getId());
        }

        public String getPresetId() {
            return presetId;
        }

        public void setPresetId(String presetId) {
            this.presetId = presetId;
        }

        public boolean isEmpty() {
            return generators.isEmpty();
        }

        public boolean isSkip() {
            return selectedGenerator == null;
        }

        public MockGeneratorDescriptor getGenerator(String generatorId) {
            return generatorDescriptors.get(generatorId);
        }
    }
}
