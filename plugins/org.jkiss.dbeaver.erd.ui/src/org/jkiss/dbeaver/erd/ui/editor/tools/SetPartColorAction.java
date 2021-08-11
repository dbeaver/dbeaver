package org.jkiss.dbeaver.erd.ui.editor.tools;

import org.eclipse.gef3.commands.Command;
import org.eclipse.gef3.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.part.ICustomizablePart;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.HashMap;
import java.util.Map;

public class SetPartColorAction extends SelectionAction {

    private IStructuredSelection selection;

    public SetPartColorAction(ERDEditorPart part, IStructuredSelection selection) {
        super(part);
        this.selection = selection;

        this.setText(ERDUIMessages.erd_tool_color_action_text_set_color);
        this.setToolTipText(ERDUIMessages.erd_tool_color_action_tip_text_set_figure_color);
        this.setId("setFigureColor"); //$NON-NLS-1$
    }

    protected boolean calculateEnabled() {
        for (Object item : selection.toArray()) {
            if (item instanceof ICustomizablePart) {
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
            private final Map<ICustomizablePart, Color> oldColors = new HashMap<>();
            private Color newColor;
            @Override
            public void execute() {
                final Shell shell = UIUtils.createCenteredShell(getWorkbenchPart().getSite().getShell());
                try {
                    ColorDialog colorDialog = new ColorDialog(shell);
                    RGB color = colorDialog.open();
                    if (color == null) {
                        return;
                    }
                    newColor = new Color(Display.getCurrent(), color);
                    for (Object item : objects) {
                        if (item instanceof ICustomizablePart) {
                            ICustomizablePart colorizedPart = (ICustomizablePart) item;
                            oldColors.put(colorizedPart, colorizedPart.getCustomBackgroundColor());
                            colorizedPart.setCustomBackgroundColor(newColor);
                        }
                    }

                } finally {
                    UIUtils.disposeCenteredShell(shell);
                }
            }

            @Override
            public void undo() {
                for (Object item : objects) {
                    if (item instanceof ICustomizablePart) {
                        ICustomizablePart colorizedPart = (ICustomizablePart) item;
                        colorizedPart.setCustomBackgroundColor(oldColors.get(colorizedPart));
                    }
                }
            }

            @Override
            public void redo() {
                for (Object item : objects) {
                    if (item instanceof ICustomizablePart) {
                        ICustomizablePart colorizedPart = (ICustomizablePart) item;
                        colorizedPart.setCustomBackgroundColor(newColor);
                    }
                }
            }
        };
    }


}
