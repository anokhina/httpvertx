package ru.org.sevn.rss;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class RssGuid {
	
	@XmlValue
	private String value;
	@XmlAttribute
	private Boolean isPermaLink;
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

    public Boolean getIsPermaLink() {
        return isPermaLink;
    }

    public void setIsPermaLink(Boolean isPermaLink) {
        this.isPermaLink = isPermaLink;
    }
}
