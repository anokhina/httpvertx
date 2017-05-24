package ru.org.sevn.rss;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class RssChanel {
	
	@XmlElement(required=true)
	private String title;
	@XmlElement(required=true)
	private String link; //TODO
	@XmlElement(required=true)
	private String description;
	
	@XmlElement
	private String language = "ru";
	@XmlElement
	private String copyright;
	@XmlElement
	private String managingEditor;
	@XmlElement
	private String webMaster;
	
	@XmlElement
	private Date pubDate = new Date();
	@XmlElement
	private Date lastBuildDate = new Date();
    
	private List<RssCategory> category = new ArrayList<>();
    
	@XmlElement
	private String generator;
	@XmlElement
	private String docs = "http://www.rssboard.org/rss-specification";
	@XmlElement
	private RssCloud cloud;
    
	@XmlElement
	private int ttl = 60;
    
	@XmlElement
	private RssImage image;
    
    @XmlElement
    private List<RssItem> item = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getManagingEditor() {
        return managingEditor;
    }

    public void setManagingEditor(String managingEditor) {
        this.managingEditor = managingEditor;
    }

    public String getWebMaster() {
        return webMaster;
    }

    public void setWebMaster(String webMaster) {
        this.webMaster = webMaster;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public Date getLastBuildDate() {
        return lastBuildDate;
    }

    public void setLastBuildDate(Date lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }

    public List<RssCategory> getCategory() {
        return category;
    }

    public void setCategory(List<RssCategory> category) {
        this.category = category;
    }

    public String getGenerator() {
        return generator;
    }

    public void setGenerator(String generator) {
        this.generator = generator;
    }

    public String getDocs() {
        return docs;
    }

    public void setDocs(String docs) {
        this.docs = docs;
    }

    public RssCloud getCloud() {
        return cloud;
    }

    public void setCloud(RssCloud cloud) {
        this.cloud = cloud;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public RssImage getImage() {
        return image;
    }

    public void setImage(RssImage image) {
        this.image = image;
    }

    public List<RssItem> getItem() {
        return item;
    }

    public void setItem(List<RssItem> item) {
        this.item = item;
    }
	
    
}
