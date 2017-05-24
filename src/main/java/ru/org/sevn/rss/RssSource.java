package ru.org.sevn.rss;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class RssSource {
	
	@XmlValue
	private String value;
	@XmlAttribute(required = true)
	private String url;
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

}
