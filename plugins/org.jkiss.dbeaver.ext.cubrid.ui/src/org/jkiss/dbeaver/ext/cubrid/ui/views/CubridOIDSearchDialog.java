package org.jkiss.dbeaver.ext.cubrid.ui.views;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.BeanUtils;

public class CubridOIDSearchDialog extends Dialog {

    private Text oidValueText = null;
    private Button findButton;
    private Tree resultTree;
    private JDBCSession session;

    public CubridOIDSearchDialog(Shell activeShell, JDBCSession session) {
        super(activeShell);
        this.session = session;
    }

    public Object open() {
        Shell parent = Display.getDefault().getActiveShell();
        Shell dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
        dialogShell.setText("OID Navigator");

        ImageDescriptor descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
                "org.jkiss.dbeaver.ext.cubrid", // plugin ID
                "icons/cubrid_icon.png");  // relative path in plugin
        Image icon = descriptor.createImage();
        dialogShell.setImage(icon);

        dialogShell.setLayout(new GridLayout(2, false));

        Label title = new Label(dialogShell, SWT.BOLD);
        title.setText("OID Navigator");

        FontData[] fD = title.getFont().getFontData();
        for (FontData fd : fD) {
            fd.setHeight(12); // Increase font size
            fd.setStyle(SWT.BOLD); // Make it bold
        }
        Font boldFont = new Font(dialogShell.getDisplay(), fD);
        title.setFont(boldFont);

        Label desc = new Label(dialogShell, SWT.NONE);
        desc.setText("Navigator data by OID");
        desc.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        // Add horizontal line
        Label separator = new Label(dialogShell, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData sepLayout = new GridData(GridData.FILL_HORIZONTAL);
        sepLayout.horizontalSpan = 2;
        separator.setLayoutData(sepLayout);

        // Layout settings
        GridData titleLayout = new GridData();
        titleLayout.horizontalSpan = 2;
        title.setLayoutData(titleLayout);

        Label oidLabel = new Label(dialogShell, SWT.NONE);
        oidLabel.setText("OID value:");

        Composite inputArea = new Composite(dialogShell, SWT.NONE);
        inputArea.setLayout(new GridLayout(2, false));
        inputArea.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        oidValueText = new Text(inputArea, SWT.LEFT | SWT.BORDER);
        oidValueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        findButton = new Button(inputArea, SWT.CENTER);
        findButton.setText("Find");
        GridData findLayout = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
        findLayout.widthHint = 80;
        findButton.setLayoutData(findLayout);

        if (oidValueText == null || oidValueText.getText().trim().length() <= 0) {
            findButton.setEnabled(false);
        }
        findButton.addSelectionListener(
            new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    find();
                }
            });

        resultTree = new Tree(dialogShell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData treeLayout = new GridData(GridData.FILL_BOTH);
        treeLayout.horizontalSpan = 2;
        treeLayout.heightHint = 150;
        resultTree.setLayoutData(treeLayout);

        // Logic to enable Find button
        oidValueText.addModifyListener(e -> {
            findButton.setEnabled(!oidValueText.getText().trim().isEmpty());
        });

        // OK button
        Button okButton = new Button(dialogShell, SWT.PUSH);
        okButton.setText("OK");
        GridData okLayout = new GridData(SWT.END, SWT.CENTER, true, false, 2, 1);
        okLayout.widthHint = 80;
        okButton.setLayoutData(okLayout);
        okButton.addListener(SWT.Selection, e -> dialogShell.close());

        dialogShell.setSize(650, 400);
        dialogShell.open();

        Display display = Display.getDefault();
        while (!dialogShell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        return null;
    }

    public boolean find() {
        boolean isValueFound = true;
        resultTree.removeAll();
        isValueFound = searchOID(oidValueText.getText());
        if (!resultTree.isDisposed()) {
            resultTree.layout(true);
        }
        return isValueFound;
    }

    public boolean searchOID(String oidString) {
        try {
            Connection conn = session.getOriginal();
            ClassLoader driverCL = conn.getClass().getClassLoader();
            Class<?> cubridConnClass = driverCL.loadClass("cubrid.jdbc.driver.CUBRIDConnection");
            Class<?> oidClass = driverCL.loadClass("cubrid.sql.CUBRIDOIDImpl");
            Method getNewInstanceMethod = oidClass.getMethod("getNewInstance", cubridConnClass, String.class);
            Object oidObject = getNewInstanceMethod.invoke(null, conn, oidString);

            String tableName = (String) BeanUtils.invokeObjectMethod(oidObject, "getTableName");
            String sql = "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";
            JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            JDBCResultSet dbResult = dbStat.executeQuery();
    	    ResultSetMetaData metaData = dbResult.getMetaData();
    	    int columnCount = metaData.getColumnCount();
    	    String[] attrName = new String[columnCount]; 
    	    while (dbResult.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    attrName[i-1] = columnName;
                }
            }

            TreeItem parentTree = new TreeItem(resultTree, 0);
            parentTree.setText(oidString);
            Display.getDefault().asyncExec(() -> parentTree.setExpanded(true));
            TreeItem child = new TreeItem(parentTree, 0);
            child.setText("table name: " + tableName);

            try (ResultSet resultSet = (ResultSet) BeanUtils.invokeObjectMethod(oidObject, "getValues", new Class<?>[] {String[].class}, new Object[] {attrName})) {
                while (resultSet.next()) {
                    for (int i = 1; i <= attrName.length; i++) {
                         String column = attrName[i - 1];
                         Object value = resultSet.getObject(i);
                         child = new TreeItem(parentTree, SWT.NONE);
                         child.setText(column + ": " + value);
                    }
                }
            }
            return true;
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showMessageBox("Error", "OID value possibly contains invalid OID fields. Please check their validity.", true);
            return false;
        }
    }
}
