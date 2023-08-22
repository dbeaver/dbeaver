// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.ui;

import org.jkiss.dbeaver.model.struct.DBStructUtils;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.function.Function;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.eclipse.jface.dialogs.IDialogSettings;
import java.util.Collections;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import java.util.Iterator;
import java.util.ArrayList;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import java.util.List;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.mockdata.ui.internal.MockDataUIMessages;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.util.Collection;
import org.jkiss.dbeaver.model.task.DBTTaskSettings;

class MockDataSettings implements DBTTaskSettings<Collection<DBSObject>>
{
    private static final Log log;
    private static final String PROP_SELECTED_ENTITY = "selectedEntity";
    static final String NO_GENERATOR_LABEL;
    private final List<EntityProperties> entityPropertiesList;
    @Nullable
    private String selectedEntityName;
    private boolean isInitialized;
    
    static {
        log = Log.getLog((Class)MockDataSettings.class);
        NO_GENERATOR_LABEL = MockDataUIMessages.tools_mockdata_attribute_generator_skip;
    }
    
    MockDataSettings(@NotNull final Collection<DBSObject> inputObjects) {
        this.entityPropertiesList = new ArrayList<EntityProperties>(inputObjects.size());
        for (final DBSObject dbsObject : inputObjects) {
            if (dbsObject != null) {
                this.entityPropertiesList.add(new EntityProperties(dbsObject));
            }
        }
    }
    
    void init(@NotNull final DBRProgressMonitor monitor) throws DBException {
        monitor.beginTask("Init mock data settings", 1);
        for (final EntityProperties properties : this.entityPropertiesList) {
            properties.init(monitor);
        }
        this.isInitialized = true;
    }
    
    List<EntityProperties> getEntityPropertiesList() {
        return Collections.unmodifiableList((List<? extends EntityProperties>)this.entityPropertiesList);
    }
    
    void loadFrom(final DBRProgressMonitor monitor, @NotNull final IDialogSettings dialogSettings) {
        this.selectedEntityName = dialogSettings.get("selectedEntity");
        for (final EntityProperties properties : this.entityPropertiesList) {
            properties.loadFrom(monitor, dialogSettings);
        }
    }
    
    void saveTo(@NotNull final IDialogSettings dialogSettings) {
        dialogSettings.put("selectedEntity", (this.selectedEntityName == null) ? "" : this.selectedEntityName);
        for (final EntityProperties properties : this.entityPropertiesList) {
            properties.saveTo(dialogSettings);
        }
    }
    
    @Nullable
    EntityProperties getEntityProperties(@NotNull final DBSEntity entity) {
        return this.entityPropertiesList.stream().filter(properties -> properties.getEntity().equals(entity)).findAny().orElse(null);
    }
    
    @Nullable
    String getSelectedEntityName() {
        return this.selectedEntityName;
    }
    
    void setSelectedEntityName(@Nullable final String selectedEntityName) {
        this.selectedEntityName = selectedEntityName;
    }
    
    void sortEntityProperties(final DBRProgressMonitor monitor) {
        final List<DBSEntity> entities = this.entityPropertiesList.stream().map(EntityProperties::getEntity).collect(Collectors.toList());
        final List<DBSEntity> simpleTables = new ArrayList<DBSEntity>();
        final List<DBSEntity> cyclicTables = new ArrayList<DBSEntity>();
        final List<DBSEntity> views = new ArrayList<DBSEntity>();
        try {
            DBStructUtils.sortTableList(monitor,entities, simpleTables, cyclicTables, views);
        }
        catch (DBException ex) {
            MockDataSettings.log.warn((Object)"Unable to sort database entities!");
            return;
        }
         
        this.entityPropertiesList.sort((properties1, properties2) -> {
            DBSEntity entity1 = properties1.getEntity();
            DBSEntity entity2 = properties2.getEntity();
            int idx1 = views.indexOf(entity1);
            int idx2 = views.indexOf(entity2);
            if (idx1 == -1 && idx2 == -1) {
                idx1 = cyclicTables.indexOf(entity1);
                idx2 = cyclicTables.indexOf(entity2);
                return idx1 == -1 && idx2 == -1 ? simpleTables.indexOf(entity1) - simpleTables.indexOf(entity2) : idx1 - idx2;
            } else {
                return idx1 - idx2;
            }
        });
    }
    
    boolean isInitialized() {
        return this.isInitialized;
    }
}
