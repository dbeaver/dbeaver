package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.internal.YashanDBMessages;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBDependencyGroup implements DBSObject {
    private final DBSObject owner;
    private final boolean dependents;

    public YashanDBDependencyGroup(DBSObject owner, boolean dependents) {
        this.owner = owner;
        this.dependents = dependents;
    }

    @NotNull
    public static Collection<YashanDBDependencyGroup> of(@NotNull DBSObject owner) {
        return Collections.unmodifiableCollection(Arrays.asList(
                new YashanDBDependencyGroup(owner, false),
                new YashanDBDependencyGroup(owner, true)
        ));
    }

    @Association
    public Collection<YashanDBDependency> getEntries(DBRProgressMonitor monitor) throws DBException {
        return YashanDBDependency.readDependencies(monitor, owner, dependents);
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return dependents
                ? YashanDBMessages.edit_yashandb_dependencies_dependent_name
                : YashanDBMessages.edit_yashandb_dependencies_dependency_name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return dependents
                ? YashanDBMessages.edit_yashandb_dependencies_dependent_description
                : YashanDBMessages.edit_yashandb_dependencies_dependency_description;
    }

    @Override
    public boolean isPersisted() {
        return owner.isPersisted();
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return owner;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return owner.getDataSource();
    }
}
