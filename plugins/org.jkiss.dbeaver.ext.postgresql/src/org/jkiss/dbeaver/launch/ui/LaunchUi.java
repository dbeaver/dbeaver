package org.jkiss.dbeaver.launch.ui;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;

public class LaunchUi {

    public static final String BUNDLE_SYMBOLIC_NAME = "org.jkiss.dbeaver.launch.ui"; //$NON-NLS-1$

    public static final String DEBUG_AS_MENU_ID = "org.jkiss.dbeaver.launch.ui.menus.menuContribution.debug"; //$NON-NLS-1$
    
    public static DBSObject extractDatabaseObject(IEditorPart editor) {
        if (editor != null) {
            IEditorInput editorInput = editor.getEditorInput();
//FIXME:AF: oh no, it should be sufficient to get adapter here, but for now the core implementation is too specific 
            if (editorInput instanceof DatabaseEditorInput<?>) {
                @SuppressWarnings("unchecked")
                DatabaseEditorInput<DBNDatabaseNode> databaseInput = (DatabaseEditorInput<DBNDatabaseNode>) editorInput;
                DBSObject databaseObject = databaseInput.getDatabaseObject();
                return databaseObject;
            }
        }
        return null;
    }

}
