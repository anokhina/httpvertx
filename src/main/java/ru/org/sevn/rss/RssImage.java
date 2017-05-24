package ru.org.sevn.rss;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RssImage {
	@XmlElement(required=true)
	private String url;
	@XmlElement(required=true)
	private String title;
	@XmlElement(required=true)
	private String link;
	
    //Maximum value for width is 144, default value is 88.
	@XmlElement
	private Integer width;
    
    //Maximum value for height is 400, default value is 31
	@XmlElement
	private Integer height;
	@XmlElement
	private String description;
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

}
