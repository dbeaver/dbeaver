 
package org.jkiss.dbeaver.mockdata.ui;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.console.TextConsoleViewer;
import org.jkiss.dbeaver.ui.UIUtils;
import java.io.IOException;
import org.eclipse.swt.widgets.Control;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.eclipse.ui.console.MessageConsole;
import org.jkiss.dbeaver.Log;
import org.eclipse.jface.wizard.WizardPage;

/**
 * 导入向导日志页面
 * @author saorionesan
 *
 */
public class MockDataWizardPageLog extends WizardPage
{
    private static final Log log;
    private MessageConsole console;
    private OutputStreamWriter writer;
    
    static {
        log = Log.getLog(MockDataWizardPageLog.class);
    }
    
    public MockDataWizardPageLog(final String task) {
        super(task);
        this.setTitle("Mock data progress");
        this.setDescription("测试数据生成日志");
    }
    
    public boolean isPageComplete() {
        return true;
    }
    
    public void createControl(final Composite parent) {
        final Composite composite = new Composite(parent, 2048);
        composite.setLayoutData(new GridData(1808));
        composite.setLayout((Layout)new FillLayout());
        this.console = new MessageConsole("mock-data-log-console", null);
        new LogConsoleViewer(composite);
        this.console.setWaterMarks(3145728, 4194304);
        try {
			this.writer = new OutputStreamWriter(this.console.newMessageStream(), StandardCharsets.UTF_8.name()); //由 StandardCharsets.UTF_8 改为GBK 编码
		} catch (UnsupportedEncodingException e) {
			log.warn("设置GBK 编码失败,重新设置为UTF8");
			this.writer = new OutputStreamWriter(this.console.newMessageStream(),StandardCharsets.UTF_8);
		}
        this.setControl((Control)composite);
    }
    
    void appendLog(final String line) {
        this.appendLog(line, false);
    }
    
    void appendLog(final String line, final boolean error) {
        if (this.getShell().isDisposed()) {
            return;
        }
        try {
            this.writer.write(line);
            this.writer.flush();
        }
        catch (IOException e) {
            MockDataWizardPageLog.log.debug((Object)e);
        }
    }
    
    public void clearLog() {
        if (this.getShell().isDisposed()) {
            return;
        }
        UIUtils.syncExec(() -> {
            synchronized (this) {
                this.console.clearConsole();
            }
        });
    }
    
    private class LogConsoleViewer extends TextConsoleViewer implements IDocumentListener
    {
        LogConsoleViewer(final Composite composite) {
            super(composite, (TextConsole)MockDataWizardPageLog.this.console);
        }
        
        public void setDocument(final IDocument document) {
            final IDocument oldDocument = this.getDocument();
            super.setDocument(document);
            if (oldDocument != null) {
                oldDocument.removeDocumentListener((IDocumentListener)this);
            }
            if (document != null) {
                document.addDocumentListener((IDocumentListener)this);
            }
        }
        
        public void documentAboutToBeChanged(final DocumentEvent event) {
        }
        
        public void documentChanged(final DocumentEvent event) {
            this.revealEndOfDocument();
        }
    }
}
