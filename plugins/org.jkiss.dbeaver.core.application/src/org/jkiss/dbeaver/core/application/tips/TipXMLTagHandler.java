package org.jkiss.dbeaver.core.application.tips;

import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.DomHandler;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
/**
 * This handler is needed to unmarshall tips with html tags.  
 * */
public class TipXMLTagHandler implements DomHandler<String, StreamResult> {

    private static final String TIP_START_TAG = "<tip>";
    private static final String TIP_END_TAG = "</tip>";
    private StringWriter xmlWriter = new StringWriter();

    public StreamResult createUnmarshaller(ValidationEventHandler errorHandler) {
        return new StreamResult(xmlWriter);
    }

    public String getElement(StreamResult rt) {
        String xml = rt.getWriter().toString();
        int beginIndex = xml.lastIndexOf(TIP_START_TAG) + TIP_START_TAG.length();
        int endIndex = xml.lastIndexOf(TIP_END_TAG);
        return xml.substring(beginIndex, endIndex);
    }

    public Source marshal(String n, ValidationEventHandler errorHandler) {
        try {
            String xml = TIP_START_TAG + n.trim() + TIP_END_TAG;
            StringReader xmlReader = new StringReader(xml);
            return new StreamSource(xmlReader);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}
