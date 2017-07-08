/*
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.winurl;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WinUrl implements Serializable {
	
	public static final String PROP_sharedText = "sharedText";
	public static final String PROP_sharedSubj = "sharedSubj";
	public static final String PROP_sharedName = "sharedName";
	public static final String PROP_fileName = "fileName";
	private String sharedText;
	private String sharedSubj;
	private String sharedName;
	private String fileName;
	
	public WinUrl() {}
	public WinUrl(String url, String name) {
		sharedSubj = name;
		sharedText = url;
	}
	
	public void copyFrom(WinUrl ed) {
		setSharedName(ed.getSharedName());
		setSharedSubj(ed.getSharedSubj());
		setSharedText(ed.getSharedText());
		setFileName(ed.getFileName());
	}
	
	public String urlContent() {
	    String urlContent = "[InternetShortcut]\n";
	    if (sharedText != null) urlContent += ("URL=" + sharedText + "\n");
	    if (sharedSubj != null) urlContent += ("Comment=" + sharedSubj + "\n");
	    return urlContent;
	}
	public String getSharedText() {
		return sharedText;
	}
	public WinUrl setSharedText(String sharedText) {
		this.sharedText = sharedText;
		return this;
	}
	public String getSharedSubj() {
		return sharedSubj;
	}
	public WinUrl setSharedSubj(String sharedSubj) {
		this.sharedSubj = sharedSubj;
		return this;
	}
	public String getSharedName() {
		return sharedName;
	}
	public WinUrl setSharedName(String sharedName) {
		this.sharedName = sharedName;
		return this;
	}
	public String getFileName() {
		return fileName;
	}
	public WinUrl setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}
	public static WinUrl parseUrlContent(String s, String defaultSubj) {
		WinUrl ed = new WinUrl();
		String parsed[] = getUrlAndName(s);
		ed.setSharedText(trim(parsed[0]));
		ed.setSharedSubj(trim(parsed[1]));
		if (ed.getSharedSubj() == null) {
			ed.setSharedSubj(defaultSubj);
		}
		ed.setSharedName(defaultSubj);
//System.err.println(ed.urlContent());
		return ed;
	}
	private static String trim(String s) {
        if (s != null) {
            return s.trim();
        }
        return s;
    }
	public static String[] getUrlAndName(String input) {
		String[] urlname = new String[2];
        {
            String pref = "\nURL=";
            int idx = input.indexOf(pref);
            if (idx >=0) {
                int idx2 = input.indexOf("\n", idx+1); //TODO eol
                if (idx2 >= 0) {
                    urlname[0] = input.substring(idx + pref.length(), idx2);
                } else {
                    urlname[0] = input.substring(idx + pref.length());
                }
            }
        }
        {
            String pref = "\nComment=";
            int idx = input.indexOf(pref);
            if (idx >=0) {
                int idx2 = input.indexOf("\n", idx+1);
                if (idx2 >= 0) {
                    urlname[1] = input.substring(idx + pref.length(), idx2);
                } else {
                    urlname[1] = input.substring(idx + pref.length());
                }
            }
        }        
        /*
        Pattern p = Pattern.compile("(?i)^(\\[InternetShortcut\\]).*(\n.*)*\nURL=(.*)((\n.*)*)?$");
        Matcher m = p.matcher(input);
        if (m.matches()) {
        	
        	int groups = m.groupCount();
        	
        	if(groups > 2) {
        		urlname[0] = m.group(3);//m.group("URL");
        	}
        	input = null;
        	if(groups > 3) {
        		input = m.group(4);//m.group("CMT");
        	}
        	if (input != null) {
                Pattern pCmt = Pattern.compile("(?i)^(\n)*(Comment=(.*))?(\n.*)*$");
                Matcher mCmt = pCmt.matcher(input);
                if (mCmt.matches()) {
	                int grps = mCmt.groupCount();
                	if(grps > 2) {
                		urlname[1] = mCmt.group(3);//mCmt.group("CMTVAL");
                	}
            	}
        	}
        }
                */
        return urlname;
	}	
	
    public Map<String, Object> getJSONMap() {
		Map<String, Object> jobj = new LinkedHashMap<>();
		jobj.put(PROP_sharedText, sharedText);
		jobj.put(PROP_sharedSubj, sharedSubj);
		jobj.put(PROP_sharedName, sharedName);
		jobj.put(PROP_fileName, fileName);
		return jobj;
	}
	
	@Override
	public String toString() {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> jobj = getJSONMap();
        sb.append("{");
        for (String k : jobj.keySet()) {
            Object o = jobj.get(k);
            if (o != null) {
                sb.append("\"").append(k).append("\"").append(":").append("\"").append(o.toString()).append("\"").append(",");
            }
        }
        sb.append("}");
		return sb.toString();
	}
    
}