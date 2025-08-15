package org.jkiss.dbeaver.ext.cubrid.ui.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class CubridOIDSearchDialog extends BaseDialog {

    private static DBIcon cubridIcon = new DBIcon("platform:/plugin/org.jkiss.dbeaver.ext.cubrid/icons/cubrid_icon.png");
    private Text oidValueText;
    private Button findButton;
    private Tree resultTree;
    private Font boldFont;
    private CubridOIDSearch oidSearch;

    public CubridOIDSearchDialog(Shell parentShell, JDBCSession session) {
        super(parentShell, "OID Navigator", cubridIcon);
        this.oidSearch = new CubridOIDSearch(session);
    }

    @Override
    protected Point getInitialSize() {
        Point calculatedSize = super.getInitialSize();
        calculatedSize.x = 500;
        return calculatedSize;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        Label title = new Label(container, SWT.NONE);
        title.setText("OID Navigator");
        FontData[] fD = title.getFont().getFontData();
        for (FontData fd : fD) {
            fd.setHeight(10);
            fd.setStyle(SWT.BOLD);
        }
        boldFont = new Font(container.getDisplay(), fD);
        title.setFont(boldFont);

        Label desc = new Label(container, SWT.NONE);
        desc.setText("Navigator data by OID");

        Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData sepLayout = new GridData(GridData.FILL_HORIZONTAL);
        separator.setLayoutData(sepLayout);

        Composite inputArea = new Composite(container, SWT.NONE);
        inputArea.setLayout(new GridLayout(3, false));
        inputArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label oidLabel = new Label(inputArea, SWT.NONE);
        oidLabel.setText("OID value:");

        oidValueText = new Text(inputArea, SWT.LEFT | SWT.BORDER);
        oidValueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        findButton = new Button(inputArea, SWT.PUSH);
        findButton.setText("Find");
        findButton.setEnabled(false);
        GridData buttonLayout = new GridData(SWT.FILL, SWT.CENTER, false, false);
        buttonLayout.widthHint = 80;
        findButton.setLayoutData(buttonLayout);
        findButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                resultTree.removeAll();
                oidSearch.searchOID(oidValueText.getText(), resultTree);
            }
        });

        oidValueText.addModifyListener(e -> {
            findButton.setEnabled(!oidValueText.getText().trim().isEmpty());
        });

        resultTree = new Tree(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData treeLayout = new GridData(GridData.FILL_BOTH);
        treeLayout.horizontalSpan = 2;
        treeLayout.heightHint = 150;
        resultTree.setLayoutData(treeLayout);

        return container;
    }

    @Override
    public boolean close() {
        boldFont.dispose();
        return super.close();
    }
}
