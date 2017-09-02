package org.jkiss.dbeaver.ext.erd.editor.tools;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.part.IColorizedPart;
import org.jkiss.dbeaver.ext.erd.part.NodePart;
import org.jkiss.dbeaver.ui.UIUtils;

public class SetPartColorAction extends SelectionAction {

    private IStructuredSelection selection;

    public SetPartColorAction(ERDEditorPart part, IStructuredSelection selection) {
        super(part);
        this.selection = selection;

        this.setText("Set color");
        this.setToolTipText("Set figure color");
        this.setId("setFigureColor");
    }

    protected boolean calculateEnabled() {
        for (Object item : selection.toArray()) {
            if (item instanceof NodePart) {
                return true;
            }
        }
        return false;
    }

    protected void init() {
        super.init();
    }

    public void run() {
        final Shell shell = UIUtils.createCenteredShell(getWorkbenchPart().getSite().getShell());
        try {
            ColorDialog colorDialog = new ColorDialog(shell);
            RGB color = colorDialog.open();
            if (color == null) {
                return;
            }
            Color newColor = new Color(Display.getCurrent(), color);
            for (Object item : selection.toArray()) {
                if (item instanceof IColorizedPart) {
                    ((IColorizedPart) item).customizeBackgroundColor(newColor);
                }
            }

        } finally {
            shell.dispose();
        }
    }
}
