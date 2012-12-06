package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DB editor utils
 */
public class DatabaseEditorUtils {

    public static void setPartBackground(IEditorPart editor, Composite composite)
    {
        Composite rootComposite = null;
        for (Composite c = composite; c != null; c = c.getParent()) {
            if (c.getParent() instanceof CTabFolder) {
                ((CTabFolder) c.getParent()).setBorderVisible(false);
                rootComposite = c;
                break;
            }
        }
        if (rootComposite == null) {
            return;
        }

        DBPDataSource dataSource = null;
        if (editor instanceof IDataSourceProvider) {
            dataSource = ((IDataSourceProvider) editor).getDataSource();
        }
        if (dataSource == null) {
            rootComposite.setBackground(null);
            return;
        }
        rootComposite.setBackground(DBeaverCore.getInstance().getSharedTextColors().getColor(new RGB(0xC4, 0xFF, 0xB5)));
    }

}
