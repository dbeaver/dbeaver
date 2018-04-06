package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class NotificationPopupMessage extends NotificationPopup {

    private final DBPDataSource dataSource;
    private String messageText;

    public NotificationPopupMessage(String text) {
        this(null, text);
    }

    public NotificationPopupMessage(DBPDataSource dataSource, String text) {
        super(PlatformUI.getWorkbench().getDisplay());

        this.dataSource = dataSource;
        this.messageText = text;
        setDelayClose(3000);
    }

    @Override
    protected String getPopupShellTitle() {
        return dataSource == null ? GeneralUtils.getProductName() : dataSource.getContainer().getName();
    }

    @Override
    protected void createContentArea(Composite composite)
    {
        composite.setLayout(new GridLayout(1, true));
        Label linkGoogleNews = new Label(composite, SWT.NONE);
        linkGoogleNews.setText(messageText);
        //linkGoogleNews.setSize(400, 100);
    }

    public static void showMessage(DBPDataSource dataSource, String text) {
        Display.getDefault().syncExec(() -> new NotificationPopupMessage(dataSource, text).open());
    }

}