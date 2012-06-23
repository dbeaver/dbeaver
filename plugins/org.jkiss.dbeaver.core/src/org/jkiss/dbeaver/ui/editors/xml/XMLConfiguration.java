package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.dbeaver.core.DBeaverCore;

public class XMLConfiguration extends SourceViewerConfiguration {
    static final RGB COLOR_XML_COMMENT = new RGB(128, 0, 0);
    static final RGB COLOR_PROC_INSTR = new RGB(128, 128, 128);
    static final RGB COLOR_STRING = new RGB(0, 128, 0);
    static final RGB COLOR_DEFAULT = new RGB(0, 0, 0);
    static final RGB COLOR_TAG = new RGB(0, 0, 128);

	private final ISharedTextColors colorManager;

	public XMLConfiguration() {
		this.colorManager = DBeaverCore.getInstance().getSharedTextColors();
	}
	
	@Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			XMLPartitionScanner.XML_COMMENT,
			XMLPartitionScanner.XML_TAG };
	}

	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return XMLPartitionScanner.XML_PARTITIONING;
	}
	
	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new XMLDoubleClickStrategy();
	}

	@Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(COLOR_XML_COMMENT)));
		reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}

	XMLScanner getXMLScanner() {
		XMLScanner scanner= new XMLScanner(colorManager);
		scanner.setDefaultReturnToken(
			new Token(
				new TextAttribute(
					colorManager.getColor(COLOR_DEFAULT))));
		return scanner;
	}

	XMLTagScanner getXMLTagScanner() {
		XMLTagScanner tagScanner= new XMLTagScanner(colorManager);
		tagScanner.setDefaultReturnToken(
			new Token(
				new TextAttribute(
					colorManager.getColor(COLOR_TAG))));
		return tagScanner;
	}
}