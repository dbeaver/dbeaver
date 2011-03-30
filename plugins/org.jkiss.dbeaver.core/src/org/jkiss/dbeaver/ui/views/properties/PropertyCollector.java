/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBEObjectCommanderImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * PropertyCollector
 */
public class PropertyCollector extends PropertySourceAbstract
{
    static final Log log = LogFactory.getLog(PropertyCollector.class);

    private DBEObjectCommander commander;
    private DBEObjectManager manager;

    public PropertyCollector(Object sourceObject, Object object, boolean loadLazyProps)
    {
        super(sourceObject, object, loadLazyProps);

        DBSDataSourceContainer container = null;
        if (sourceObject instanceof IDataSourceContainerProvider) {
            container = ((IDataSourceContainerProvider) sourceObject).getDataSourceContainer();
        } else if (sourceObject instanceof IDataSourceProvider) {
            container = ((IDataSourceProvider) sourceObject).getDataSource().getContainer();
        }
        if (container != null) {
            commander = new DBEObjectCommanderImpl(container);
            manager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(sourceObject.getClass());
        }
    }

    @Override
    protected DBEObjectCommander getObjectCommander()
    {
        return commander;
    }

    @Override
    protected DBEObjectManager getObjectManager()
    {
        return manager;
    }

    public PropertyCollector(Object object, boolean loadLazyProps)
    {
        super(object, object, loadLazyProps);
    }

    public void collectProperties(IPropertyFilter filter)
    {
        this.collectProperties(this.getEditableValue(), filter);
    }

    public void collectProperties(Object object, IPropertyFilter filter)
    {
        List<ObjectPropertyDescriptor> annoProps = ObjectAttributeDescriptor.extractAnnotations(this, object.getClass(), filter);
        for (final ObjectPropertyDescriptor desc : annoProps) {
            if (desc.isCollectionAnno()) {
                DBRRunnableWithProgress loader = new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                    {
                        try {
                            Object value = desc.readValue(getEditableValue(), monitor);
                            addProperty(desc.getId(), desc.getDisplayName(), value);
                        } catch (IllegalAccessException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                };
                // Add as collection
                try {
                    if (desc.isLazy(object, true)) {
                        DBeaverCore.getInstance().runInProgressService(loader);
                    } else {
                        loader.run(null);
                    }
                }
                catch (InvocationTargetException e) {
                    log.error(e.getTargetException());
                } catch (InterruptedException e) {
                    // Do nothing
                }
            } else {
                addProperty(desc);
            }
        }
    }

}
