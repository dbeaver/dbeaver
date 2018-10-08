package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TipsXmlHandler extends DefaultHandler {

    private static final String TIP = "tip";
    private boolean tipTagStarted;
    private StringBuilder tipTagContent = new StringBuilder();
    private List<String> tips = new ArrayList<>();
    private static final String BR = "br";
    private static final List<String> HTML_TAGS = Arrays.asList(BR, "b", "i", "u", "q");
    private static final String TAG_BRACKET_BEGIN = "<";
    private static final String TAG_BRACKET_END = ">";
    private static final String SLASH = "/";

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (HTML_TAGS.contains(qName) && tipTagStarted) {
            tipTagContent.append(TAG_BRACKET_BEGIN).append(qName).append(TAG_BRACKET_END);
        }
        if (qName.equalsIgnoreCase(TIP)) {
            this.tipTagStarted = true;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (tipTagStarted) {
            tipTagContent.append(new String(ch, start, length));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(TIP)) {
            this.tips.add(tipTagContent.toString());
            this.tipTagStarted = false;
            tipTagContent = new StringBuilder();
        }

        if (!qName.equals(BR) && HTML_TAGS.contains(qName) && tipTagStarted) {
            tipTagContent.append(TAG_BRACKET_BEGIN).append(SLASH).append(qName).append(TAG_BRACKET_END);
        }
    }

    public List<String> getTips() {
        return tips;
    }
}
