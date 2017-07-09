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
package ru.org.sevn.jvert.wwwgen;

import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import ru.org.sevn.common.solr.SolrIndexer;
import ru.org.sevn.jvert.HttpVerticle;

public class FindHandler implements io.vertx.core.Handler<RoutingContext> {

    private final SolrIndexer indexer;
    private final String webpath;
    private String msgSearch = "Search:";
    private String msgSearchRes = "Search result:";
    
    public FindHandler(SolrIndexer indexer, String webpath) {
        this.indexer = indexer;
        this.webpath = webpath;
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        String qs = ctx.request().getParam("qs");
        StringBuilder ret = new StringBuilder();
        if (qs == null || qs.length() == 0) {

            ret.append("<!DOCTYPE HTML>\n"
                    + "<html>\n"
                    + "<head>\n"
                    + "	<meta charset=\"utf-8\">\n"
                    + "    <title></title>\n"
                    + "</head>\n"
                    + "<body>\n"
                    + "\n"
                    + "<form action=\"\" method=\"POST\" name=\"searchform\">\n"
                    + msgSearch + "<input type=\"search\" name=\"qs\" style=\"width: 100%;\"/>\n"
                    + "    <input type=\"submit\"/>\n"
                    + "</form>\n"
                    + "\n"
                    + "</body>\n"
                    + "</html>");
        } else {
            try {
                ret.append("<!DOCTYPE HTML>\n"
                        + "<html>\n"
                        + "<head>\n"
                        + "	<meta charset=\"utf-8\">\n"
                        + "    <title></title>\n"
                        + "</head>\n"
                        + "<body>\n");
                ret.append("<p>").append(msgSearchRes);
                //TODO paging
                indexer.findHtml(webpath, qs, 500).forEach(doc -> {
                    String href = "/" + doc.getFieldValue(SolrIndexer.HTML_WPATH) + "/" + doc.getFieldValue(SolrIndexer.HTML_PATH);
                    Object otitle = doc.getFieldValue(SolrIndexer.HTML_TITLE);
                    String title;
                    if (otitle == null) {
                        title = href;
                    } else {
                        title = otitle.toString();
                    }
                    ret.append("<br>").append("<a href='").append(href).append("'>").append(title).append("</a>");
                });
                ret.append("\n"
                    + "</body>\n"
                    + "</html>");
            } catch (SolrServerException | IOException ex) {
                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                ctx.fail(ex);
                return;
            }
        }
        ctx.response().putHeader("content-type", "text/html").end(ret.toString());
    }

    public String getMsgSearch() {
        return msgSearch;
    }

    public void setMsgSearch(String msgSearch) {
        this.msgSearch = msgSearch;
    }

    public String getMsgSearchRes() {
        return msgSearchRes;
    }

    public void setMsgSearchRes(String msgSearchRes) {
        this.msgSearchRes = msgSearchRes;
    }

}
/*
<!DOCTYPE HTML>
<html>
<head>
	<meta charset="utf-8">
    <title></title>
</head>
<body>

<form action="" method="POST" name="searchform">
    Search:<input type="text" name="qs" style="width: 100%;"/>
    <input type="submit"/>
</form>

</body>
</html>
*/                