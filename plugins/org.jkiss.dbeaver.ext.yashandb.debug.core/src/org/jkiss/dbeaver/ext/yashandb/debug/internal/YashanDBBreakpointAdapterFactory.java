
package org.jkiss.dbeaver.ext.yashandb.debug.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.debug.DBGBreakpointDescriptor;
import org.jkiss.dbeaver.ext.yashandb.debug.internal.impl.YashanDBDebugBreakpointDescriptor;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;


public class YashanDBBreakpointAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[] { DBGBreakpointDescriptor.class };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == DBGBreakpointDescriptor.class) {
            if (adaptableObject instanceof YashanDBProcedureStandalone) {
                return adapterType.cast(new YashanDBDebugBreakpointDescriptor(
                    ((YashanDBProcedureStandalone) adaptableObject).getObjectId(),  ((YashanDBProcedureStandalone) adaptableObject).getName(),-1
                ));
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
