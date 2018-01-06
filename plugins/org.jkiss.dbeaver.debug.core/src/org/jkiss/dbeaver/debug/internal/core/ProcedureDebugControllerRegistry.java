package org.jkiss.dbeaver.debug.internal.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.debug.DBGControllerRegistry;
import org.jkiss.dbeaver.debug.DBGProcedureController;
import org.jkiss.dbeaver.debug.core.DebugCore;

public class ProcedureDebugControllerRegistry implements DBGControllerRegistry<DBGProcedureController> {
    
    private static Log log = Log.getLog(ProcedureDebugControllerRegistry.class);

    private final Map<String, IConfigurationElement> factories = new HashMap<>();

    @Override
    public DBGProcedureController createController(String dataTypeProviderId) {
        if (dataTypeProviderId == null) {
            return null;
        }
        
        IConfigurationElement element = factories.get(dataTypeProviderId);
        if (element == null) {
            return null;
        }
        try {
            Object executable = element.createExecutableExtension(DebugCore.EP_PROCEDURE_CONTROLLERS_CONTROLLER_CLASS);
            if (executable instanceof DBGProcedureController) {
                DBGProcedureController controller = (DBGProcedureController) executable;
                return controller;
            }
        } catch (CoreException e) {
            Log.log(log, e.getStatus());
        }
        return null;
    }

    public void init() {
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        final String namespace = DebugCore.BUNDLE_SYMBOLIC_NAME;
        final String epName = DebugCore.EP_PROCEDURE_CONTROLLERS_ID;
        final String elementName = DebugCore.EP_PROCEDURE_CONTROLLERS_CONTROLLER;
        final String identifierName = DebugCore.EP_PROCEDURE_CONTROLLERS_CONTROLLER_PROVIDER_ID;
        IExtensionPoint extensionPoint = registry.getExtensionPoint(namespace, epName);
        IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
        for (IConfigurationElement element : configurationElements) {
            String name = element.getName();
            if (elementName.equals(name)) {
                String configuredIdentifier = element.getAttribute(identifierName);
                factories.put(configuredIdentifier, element);
            }
        }
    }

    public void dispose() {
        factories.clear();
    }

}
