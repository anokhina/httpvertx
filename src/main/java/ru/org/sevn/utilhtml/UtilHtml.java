/*******************************************************************************
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
 *******************************************************************************/
package ru.org.sevn.utilhtml;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.org.sevn.common.mime.Mime;

public class UtilHtml {
	public static String getCleanHtmlBodyContent(String html) {
		if (html == null) return null;
		return getHtmlBodyContent(Jsoup.clean(html, Whitelist.basic()));
	}
	public static String getHtmlBodyContent(String html) {
		if (html == null) return null;
		
		Document doc = Jsoup.parseBodyFragment(html);
//		Document doc = Jsoup.parse(html);
		if (doc != null) {
			return doc.body().html();
		}
		return null;
	}
    public static void walkFilesByLinks(File dir, String doccontent, BiConsumer<File, String> handler) {
        walkFilesByLinks(dir, Jsoup.parseBodyFragment(doccontent), handler);
    }
    public static void walkFilesByLinks(File dir, Document doc, BiConsumer<File, String> handler) {
        //System.out.println("+++++++++++++"+dir.getAbsolutePath()+":"+doc.html());
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String abshref = link.attr("abs:href");
            if (abshref != null && abshref.length() > 0) continue;
            
            String href = link.attr("href");
            
            File fl = new File(dir.getParentFile(), href);
            //System.out.println("+++++++++++++"+href+":"+fl.getAbsolutePath());
            if (fl.exists()) {
                File fldir = fl;
                File flcontent = fl;
                if (!fl.isDirectory()) {
                    fldir = fl.getParentFile();
                } else {
                    flcontent = new File(fldir, "index.html");
                }
                if (flcontent.exists()) {
                    handler.accept(flcontent, link.text());
                    String contentType = Mime.getMimeTypeFile(flcontent.getName());
                    if ("text/html".equals(contentType)) {
                        try {
                            Document fdoc = Jsoup.parse(flcontent, "UTF-8"); //TODO detect charset
                            walkFilesByLinks(flcontent, fdoc, handler);
                        } catch (IOException ex) {
                            Logger.getLogger(UtilHtml.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }
    }
}
