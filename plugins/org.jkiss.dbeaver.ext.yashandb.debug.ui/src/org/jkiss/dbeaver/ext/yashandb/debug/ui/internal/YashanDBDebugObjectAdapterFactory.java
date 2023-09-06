package org.jkiss.dbeaver.ext.yashandb.debug.ui.internal;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.debug.DBGDebugObject;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBProcedureStandalone;
import org.jkiss.dbeaver.ext.yashandb.ui.editors.YashanDBSourceDeclarationEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;

public class YashanDBDebugObjectAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] CLASSES = new Class[]{DBGDebugObject.class};

    private static final DBGDebugObject DEBUG_OBJECT = new DBGDebugObject() {
    };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adapterType == DBGDebugObject.class) {
            if (adaptableObject instanceof YashanDBSourceDeclarationEditor &&
                    ((YashanDBSourceDeclarationEditor) adaptableObject).getSourceObject() instanceof YashanDBProcedureStandalone) {
                return adapterType.cast(DEBUG_OBJECT);
            }
            if (adaptableObject instanceof IDatabaseEditorInput &&
                    ((IDatabaseEditorInput) adaptableObject).getDatabaseObject() instanceof YashanDBProcedureStandalone) {
                return adapterType.cast(DEBUG_OBJECT);
            }
            if (adaptableObject instanceof DBNDatabaseNode &&
                    ((DBNDatabaseNode) adaptableObject).getObject() instanceof YashanDBProcedureStandalone) {
                return adapterType.cast(DEBUG_OBJECT);
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return CLASSES;
    }

}
