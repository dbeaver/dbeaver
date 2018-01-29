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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorRegistry;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class MockDataSettings {

    private boolean removeOldData;
    private long rowsNumber = 10;
    private Map<String, MockGeneratorDescriptor> generatorDescriptors = new HashMap<>(); // generatorId -> MockGeneratorDescriptor
    private Map<String, AttributeGeneratorProperties> attributeGenerators = new HashMap<>(); // attribute.name -> generators properties

    // populate attribute generators properties map
    public Collection<? extends DBSAttributeBase> init(MockDataExecuteWizard wizard) throws DBException {
        List<DBSDataManipulator> databaseObjects = wizard.getDatabaseObjects();
        DBSDataManipulator dataManipulator = databaseObjects.iterator().next();
        DBSEntity dbsEntity = (DBSEntity) dataManipulator;
        Collection<? extends DBSAttributeBase> attributes = DBUtils.getRealAttributes(dbsEntity.getAttributes(new VoidProgressMonitor()));
        for (DBSAttributeBase attribute : attributes) {
            AttributeGeneratorProperties generatorProperties = new AttributeGeneratorProperties(attribute);
            attributeGenerators.put(attribute.getName(), generatorProperties);
            MockGeneratorRegistry generatorRegistry = MockGeneratorRegistry.getInstance();
            List<MockGeneratorDescriptor> generators = generatorRegistry.findAllGenerators(dataManipulator.getDataSource(), attribute);
            for (MockGeneratorDescriptor generator : generators) {
                generatorDescriptors.put(generator.getId(), generator);
                generatorProperties.putGeneratorPropertySource(generator.getId(), new PropertySourceCustom(generator.getProperties(), null));
            }
        }
        return attributes;
    }

    public MockGeneratorDescriptor getGeneratorDescriptor(String generatorId) {
        return generatorDescriptors.get(generatorId);
    }

    public MockGeneratorDescriptor findGeneratorForName(String name) {
        for (String generatorId : generatorDescriptors.keySet()) {
            MockGeneratorDescriptor generatorDescriptor = generatorDescriptors.get(generatorId);
            if (name.equals(generatorDescriptor.getLabel())) {
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

    public AttributeGeneratorProperties getAttributeGeneratorProperties(DBSAttributeBase attribute) {
        return attributeGenerators.get(attribute.getName());
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
