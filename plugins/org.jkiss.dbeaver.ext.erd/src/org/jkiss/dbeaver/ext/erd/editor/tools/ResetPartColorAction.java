package org.jkiss.dbeaver.ext.erd.editor.tools;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.part.IColorizedPart;

import java.util.HashMap;
import java.util.Map;

public class ResetPartColorAction extends SelectionAction {

    private IStructuredSelection selection;

    public ResetPartColorAction(ERDEditorPart part, IStructuredSelection selection) {
        super(part);
        this.selection = selection;

        this.setText("Remove color");
        this.setToolTipText("Reset figure color");
        this.setId("removeFigureColor");
    }

    protected boolean calculateEnabled() {
        for (Object item : selection.toArray()) {
            if (item instanceof IColorizedPart && ((IColorizedPart) item).getCustomBackgroundColor() != null) {
                return true;
            }
        }
        return false;
    }

    protected void init() {
        super.init();
    }

    public void run() {
        this.execute(this.createColorCommand(selection.toArray()));
    }

    private Command createColorCommand(final Object[] objects) {
        return new Command() {
            private final Map<IColorizedPart, Color> oldColors = new HashMap<>();
            @Override
            public void execute() {
                for (Object item : objects) {
                    if (item instanceof IColorizedPart) {
                        IColorizedPart colorizedPart = (IColorizedPart) item;
                        oldColors.put(colorizedPart, colorizedPart.getCustomBackgroundColor());
                        colorizedPart.customizeBackgroundColor(null);
                    }
                }
            }

            @Override
            public void undo() {
                for (Object item : objects) {
                    if (item instanceof IColorizedPart) {
                        IColorizedPart colorizedPart = (IColorizedPart) item;
                        colorizedPart.customizeBackgroundColor(oldColors.get(colorizedPart));
                    }
                }
            }

            @Override
            public void redo() {
                for (Object item : objects) {
                    if (item instanceof IColorizedPart) {
                        IColorizedPart colorizedPart = (IColorizedPart) item;
                        colorizedPart.customizeBackgroundColor(null);
                    }
                }
            }
        };
    }


}
