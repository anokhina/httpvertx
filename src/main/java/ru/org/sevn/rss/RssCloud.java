package ru.org.sevn.rss;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class RssCloud {
	@XmlAttribute
	private String domain;
	@XmlAttribute
	private String port;
	@XmlAttribute
	private String path;
	@XmlAttribute
	private String registerProcedure;
	@XmlAttribute
	private String protocol;
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public String getPort() {
		return port;
	}
	public void setPort(String port) {
		this.port = port;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getRegisterProcedure() {
		return registerProcedure;
	}
	public void setRegisterProcedure(String registerProcedure) {
		this.registerProcedure = registerProcedure;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
}
