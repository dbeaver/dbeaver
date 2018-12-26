package org.jkiss.dbeaver.debug.ui;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class DatabaseScriptDialog extends TitleAreaDialog {
    
    private final DBSObject dbsObject;
    private String scriptValue;
    private IEditorSite subSite;
    private SQLEditorBase sqlViewer;
    private StringEditorInput sqlInput;

    public DatabaseScriptDialog(Shell shell, IWorkbenchPartSite parentSite, String name, String value, DBSObject dbsObject) {
        super(shell);
        this.dbsObject = dbsObject;
        this.subSite = new SubEditorSite(parentSite);
        this.sqlInput = new StringEditorInput(name, value, true, GeneralUtils.getDefaultFileEncoding());
        this.scriptValue = value;
    }
    
    public String getScriptTextValue() {
        return scriptValue;
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        String title = "Specify Script";
        getShell().setText(title);
        Control createdArea = super.createDialogArea(parent);
        sqlViewer = new DatabaseScriptEditor(dbsObject, title);
        try {
            sqlViewer.init(subSite, sqlInput);
            sqlViewer.reloadSyntaxRules();
            Object text = sqlViewer.getAdapter(Control.class);
            if (text instanceof StyledText) {
                ((StyledText) text).setEditable(true);;
            }
        } catch (PartInitException e) {
            DBWorkbench.getPlatformUI().showError(getShell().getText(), null, e);
        }
        Composite panel = UIUtils.createPlaceholder(parent, 1);
        panel.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite editorPH = new Composite(panel, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.verticalIndent = 3;
        gd.horizontalSpan = 1;
        gd.minimumHeight = 100;
        gd.minimumWidth = 100;
        editorPH.setLayoutData(gd);
        editorPH.setLayout(new FillLayout());
        sqlViewer.createPartControl(editorPH);
        return createdArea;
    }
    
    @Override
    protected void okPressed() {
        scriptValue = sqlViewer.getDocument().get();
        super.okPressed();
    }
    
}
