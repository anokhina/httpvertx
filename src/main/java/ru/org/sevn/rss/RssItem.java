package ru.org.sevn.rss;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "item")
public class RssItem {

    @XmlElement(required=true)
	private String title;
    @XmlElement(required=true)
	private String link;
    @XmlElement(required=true)
	private String description;

    @XmlElement
	private String author;
    @XmlElement
	private List<RssCategory> category = new ArrayList<>();
    @XmlElement
	private String comments;
    @XmlElement
    private RssGuid guid;
    @XmlElement
	private Date pubDate;
    @XmlElement
    private RssSource source;
    @XmlElement
    private RssEnclosure enclosure;

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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<RssCategory> getCategory() {
        return category;
    }

    public void setCategory(List<RssCategory> category) {
        this.category = category;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public RssGuid getGuid() {
        return guid;
    }

    public void setGuid(RssGuid guid) {
        this.guid = guid;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public RssSource getSource() {
        return source;
    }

    public void setSource(RssSource source) {
        this.source = source;
    }

    public RssEnclosure getEnclosure() {
        return enclosure;
    }

    public void setEnclosure(RssEnclosure enclosure) {
        this.enclosure = enclosure;
    }
}
