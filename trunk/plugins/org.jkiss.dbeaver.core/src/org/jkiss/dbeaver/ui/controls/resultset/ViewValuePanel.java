package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.data.DBDValueEditor;
import org.jkiss.dbeaver.model.data.DBDValueEditorDialog;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * RSV value view panel
 */
abstract class ViewValuePanel extends Composite {

    private final Label columnImageLabel;
    private final Label columnNameLabel;
    private final Composite viewPlaceholder;

    private DBDValueController previewController;
    private DBDValueEditor valueViewer;

    ViewValuePanel(Composite parent)
    {
        super(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.verticalSpacing = 0;
        gl.horizontalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        setLayout(gl);
        this.setBackground(this.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        this.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite toolBar = UIUtils.createPlaceholder(this, 3);
        ((GridLayout)toolBar.getLayout()).horizontalSpacing = 5;
        toolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolBar.setBackground(toolBar.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        columnImageLabel = new Label(toolBar, SWT.NONE);
        columnImageLabel.setImage(DBIcon.TYPE_OBJECT.getImage());

        columnNameLabel = new Label(toolBar, SWT.NONE);
        columnNameLabel.setText("Col name");
        columnNameLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ToolBar hideToolbar = new ToolBar(toolBar, SWT.HORIZONTAL);
        hideToolbar.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
        ToolItem hideItem = new ToolItem(hideToolbar, SWT.PUSH);
        hideItem.setText("Hide");
        hideItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                hidePanel();
            }
        });

        viewPlaceholder = UIUtils.createPlaceholder(this, 1);
        viewPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));
        viewPlaceholder.setLayout(new FillLayout());
        viewPlaceholder.setBackground(DBeaverUI.getSharedTextColors().getColor(SharedTextColors.NOTE_BACKGROUND_COLOR));

    }

    protected abstract void hidePanel();

    public Composite getViewPlaceholder()
    {
        return viewPlaceholder;
    }

    public void viewValue(final DBDValueController valueController)
    {
        if (previewController == null || valueController.getAttributeMetaData() != previewController.getAttributeMetaData()) {
            // Cleanup previous viewer
            for (Control child : viewPlaceholder.getChildren()) {
                child.dispose();
            }
            previewController = null;

            columnImageLabel.setImage(ResultSetViewer.getAttributeImage(valueController.getAttributeMetaData()));
            columnNameLabel.setText(valueController.getAttributeMetaData().getName());
            if (valueViewer instanceof DBDValueEditorDialog) {
                ((DBDValueEditorDialog)valueViewer).closeValueEditor();
            }
            // Create a new one
            try {
                valueViewer = valueController.getValueHandler().createEditor(valueController);
            } catch (DBException e) {
                UIUtils.showErrorDialog(getShell(), "Value preview", "Can't create value viewer", e);
                return;
            }
            if (valueViewer == null) {
                final Composite placeholder = UIUtils.createPlaceholder(viewPlaceholder, 1);
                placeholder.setBackground(placeholder.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
                placeholder.addPaintListener(new PaintListener() {
                    @Override
                    public void paintControl(PaintEvent e)
                    {
                        Rectangle bounds = placeholder.getBounds();
                        String message = "No editor for [" + valueController.getAttributeMetaData().getTypeName() + "]";
                        Point ext = e.gc.textExtent(message);
                        e.gc.drawText(message, (bounds.width - ext.x) / 2, bounds.height / 3 + 20);
                    }
                });
            }
            previewController = valueController;
            viewPlaceholder.layout();
        }
        if (valueViewer != null) {
            valueViewer.refreshValue();
        }
    }

}
