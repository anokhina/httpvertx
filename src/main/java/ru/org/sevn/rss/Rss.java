package ru.org.sevn.rss;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/*
http://www.rssboard.org/rss-specification 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "rss")
public class Rss {
	
	@XmlAttribute
	private String version = "2.0"; 
	
	@XmlElement
	private List<RssChanel> chanel = new ArrayList<RssChanel>();

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<RssChanel> getChanel() {
		return chanel;
	}

	public void setChanel(List<RssChanel> chanel) {
		this.chanel = chanel;
	}

	public Rss addChanel(RssChanel ch) {
		this.chanel.add(ch);
		return this;
	}
}
