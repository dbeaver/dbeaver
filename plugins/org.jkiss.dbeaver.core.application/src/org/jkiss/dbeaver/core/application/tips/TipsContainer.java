package org.jkiss.dbeaver.core.application.tips;

import java.util.List;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "tips")
@XmlAccessorType(XmlAccessType.FIELD)
public class TipsContainer {
	
	@XmlAnyElement(TipXMLTagHandler.class)
	private List<String> tips;

	public List<String> getTips() {
		return tips;
	}

	public void setTips(List<String> tips) {
		this.tips = tips;
	}
}
