package org.jkiss.dbeaver.model.project;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.jkiss.dbeaver.DBException;

/**
 * Resource handler
 */
public interface DBPResourceHandler {

    public static final QualifiedName PROP_PROJECT_ID = new QualifiedName("org.jkiss.dbeaver", "project-id");
    public static final QualifiedName PROP_RESOURCE_TYPE = new QualifiedName("org.jkiss.dbeaver", "resource-type");
    
    public static final String RES_TYPE_PROJECT = "project";

    void initializeResource(IResource resource) throws CoreException, DBException;

}
