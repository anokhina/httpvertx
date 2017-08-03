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
package ru.org.sevn.jvert;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringEscapeUtils;
import ru.org.sevn.common.data.DBProperty;
import ru.org.sevn.common.data.DBTableProperty;
import ru.org.sevn.common.data.SimpleSqliteObjectStore;
import static ru.org.sevn.jvert.HttpVerticle.secureSet;

public class UrlHandler implements io.vertx.core.Handler<RoutingContext> {

    public static class UrlSimpleSqliteObjectStore extends SimpleSqliteObjectStore {
        
        public UrlSimpleSqliteObjectStore(String filePath, ObjectMapper... ompArr) {
            super(filePath, ompArr);
        }
        
        @Override
        protected int addObject(Object o, SimpleSqliteObjectStore.ObjectDescriptor objectDescriptor, Connection c) throws SQLException {
            c.setAutoCommit(false);
            int ret = super.addObject(o, objectDescriptor, c);
            if (ret > 0) {
                if (o instanceof UrlLink) {
                    UrlLink ulink = (UrlLink)o;
                    final ObjectDescriptor dscUrlLinkTag = getObjectDescriptor(UrlLinkTag.class);
                    for (UrlTag t : ulink.getTags()) {
                        ret += addObject(new UrlLinkTag().setUrlId(ulink.getId()).setTagId(t.getId()), dscUrlLinkTag, c);
                    }
                }
            }
            c.commit();
            return ret;
        }
        
        @Override
        protected int updateObject(Object o, ObjectDescriptor objectDescriptor, final Connection c) throws SQLException {
            c.setAutoCommit(false);
            int ret = 0;
            if (o instanceof UrlLink) {
                UrlLink ulink = (UrlLink)o;
                // TODO delete only removed
                //delete old
                final ObjectDescriptor dscUrlLinkTag = getObjectDescriptor(UrlLinkTag.class);
                Collection<UrlLinkTag> linksTagOld = dscUrlLinkTag.getObjects(c, null, new String[]{UrlLinkTag.FIELD_URL_ID}, new Object[] {ulink.getId()}, null, null);
                for (UrlLinkTag e : linksTagOld) {
                    deleteObject(e, dscUrlLinkTag, c);
                }
                //add new
                for (UrlTag t : ulink.getTags()) {
                    ret += addObject(new UrlLinkTag().setUrlId(ulink.getId()).setTagId(t.getId()), dscUrlLinkTag, c);
                }
            }
            ret += super.updateObject(o, objectDescriptor, c);
            c.commit();
            return ret;
        }
        
        @Override
        protected int deleteObject(Object o, ObjectDescriptor objectDescriptor, final Connection c) throws SQLException {
            c.setAutoCommit(false);
            int ret = 0;
            if (o instanceof UrlLink) {
                UrlLink ulink = (UrlLink)o;
                final ObjectDescriptor dscUrlLinkTag = getObjectDescriptor(UrlLinkTag.class);
                Collection<UrlLinkTag> linksTagOld = dscUrlLinkTag.getObjects(c, null, new String[]{UrlLinkTag.FIELD_URL_ID}, new Object[] {ulink.getId()}, null, null);
                for (UrlLinkTag e : linksTagOld) {
                    ret += deleteObject(e, dscUrlLinkTag, c);
                }
            }
            ret += super.deleteObject(o, objectDescriptor, c);
            c.commit();
            return ret;
        }
    }
    
    private final UrlSimpleSqliteObjectStore ostore;
    
    public UrlHandler(UrlSimpleSqliteObjectStore os) {
        this.ostore = os;
    }

    public static class UrlTagMapper implements SimpleSqliteObjectStore.ObjectMapper<UrlTag> {

        @Override
        public Class getObjectType() {
            return UrlTag.class;
        }

        @Override
        public void mapValues(UrlTag o, String colName, ResultSet rs) throws SQLException {
            switch(colName) {
                case UrlTag.FIELD_ID:
                    o.setId(rs.getLong(colName));
                    break;
                case UrlTag.FIELD_NAME:
                    o.setName(rs.getString(colName));
                    break;
            }
        }

        @Override
        public void setStatement(UrlTag o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException {
            switch(colName) {
                case UrlTag.FIELD_ID:
                    pstmt.setLong(parameterIndex, o.getId());
                    break;
                case UrlTag.FIELD_NAME:
                    pstmt.setString(parameterIndex, o.getName());
                    break;
            }        
        }
        
    }
    public static class UrlLinkMapper implements SimpleSqliteObjectStore.ObjectMapper<UrlLink> {

        @Override
        public Class getObjectType() {
            return UrlLink.class;
        }

        @Override
        public void mapValues(UrlLink o, String colName, ResultSet rs) throws SQLException {
            switch(colName) {
                case UrlLink.FIELD_ID:
                    o.setId(rs.getLong(colName));
                    break;
                case UrlLink.FIELD_TITLE:
                    o.setTitle(rs.getString(colName));
                    break;
                case UrlLink.FIELD_URL:
                    o.setUrl(rs.getString(colName));
                    break;
                case UrlLink.FIELD_PATH:
                    o.setPath(rs.getString(colName));
                    break;
            }
        }

        @Override
        public void setStatement(UrlLink o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException {
            switch(colName) {
                case UrlLink.FIELD_ID:
                    pstmt.setLong(parameterIndex, o.getId());
                    break;
                case UrlLink.FIELD_TITLE:
                    pstmt.setString(parameterIndex, o.getTitle());
                    break;
                case UrlLink.FIELD_URL:
                    pstmt.setString(parameterIndex, o.getUrl());
                    break;
                case UrlLink.FIELD_PATH:
                    pstmt.setString(parameterIndex, o.getPath());
                    break;
            }        
        }
        
    }
    public static class UrlLinkTagMapper implements SimpleSqliteObjectStore.ObjectMapper<UrlLinkTag> {

        @Override
        public Class getObjectType() {
            return UrlLinkTag.class;
        }

        @Override
        public void mapValues(UrlLinkTag o, String colName, ResultSet rs) throws SQLException {
            switch(colName) {
                case UrlLinkTag.FIELD_TAG_ID:
                    o.setTagId(rs.getLong(colName));
                    break;
                case UrlLinkTag.FIELD_URL_ID:
                    o.setUrlId(rs.getLong(colName));
                    break;
            }
        }

        @Override
        public void setStatement(UrlLinkTag o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException {
            switch(colName) {
                case UrlLinkTag.FIELD_TAG_ID:
                    pstmt.setLong(parameterIndex, o.getTagId());
                    break;
                case UrlLinkTag.FIELD_URL_ID:
                    pstmt.setLong(parameterIndex, o.getUrlId());
                    break;            
            }        
        }
        
    }
    
    @DBTableProperty(name = UrlTag.TABLE_NAME)
    public static class UrlTag {
        public static final String TABLE_NAME = "URL_TAG";
        public static final String FIELD_ID = "ID";
        @DBProperty(name = FIELD_ID, dtype = "INTEGER PRIMARY KEY  AUTOINCREMENT   NOT NULL")
        private long id;
        public static final String FIELD_NAME = "NAME";
        @DBProperty(name = FIELD_NAME, dtype = "TEXT NOT NULL")
        private String name;

        public long getId() {
            return id;
        }

        public UrlTag setId(long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public UrlTag setName(String name) {
            this.name = name;
            return this;
        }
    }
    
    @DBTableProperty(name = UrlLinkTag.TABLE_NAME)
    public static class UrlLinkTag {
        public static final String TABLE_NAME = "URL_LINK_TAG";
        public static final String FIELD_TAG_ID = "TAG_ID";
        @DBProperty(name = FIELD_TAG_ID, dtype = "INTEGER NOT NULL")
        private long tagId;
        public static final String FIELD_URL_ID = "URL_ID";
        @DBProperty(name = FIELD_URL_ID, dtype = "INTEGER NOT NULL")
        private long urlId;

        public long getTagId() {
            return tagId;
        }

        public UrlLinkTag setTagId(long tagId) {
            this.tagId = tagId;
            return this;
        }

        public long getUrlId() {
            return urlId;
        }

        public UrlLinkTag setUrlId(long urlId) {
            this.urlId = urlId;
            return this;
        }
    }
    
    @DBTableProperty(name = UrlLink.TABLE_NAME)
    public static class UrlLink {
        public static final String TABLE_NAME = "URL_LINK";
        public static final String FIELD_ID = "ID";
        @DBProperty(name = FIELD_ID, dtype = "INTEGER PRIMARY KEY  AUTOINCREMENT   NOT NULL")
        private long id;
        public static final String FIELD_URL = "URL";
        @DBProperty(name = FIELD_URL, dtype = "TEXT NOT NULL")        
        private String url;
        public static final String FIELD_TITLE = "TITLE";
        @DBProperty(name = FIELD_TITLE, dtype = "TEXT NOT NULL")        
        private String title;
        public static final String FIELD_PATH = "SPATH";
        @DBProperty(name = FIELD_PATH, dtype = "TEXT")        
        private String path;
        
        private Collection<UrlTag> tags = new ArrayList<UrlTag>();

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

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

        public Collection<UrlTag> getTags() {
            return tags;
        }

        public void setTags(Collection<UrlTag> tags) {
            this.tags = tags;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
        
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.user() instanceof ExtraUser) {
            MultiMap params = ctx.request().params();
            String msg = "";
            if (params.contains("addTag")) {
                if (addTag(ctx, params.get("addTag")) > 0) {
                    msg = "Tag added";
                } else {
                    msg = "Tag not added";
                }
            } else if (params.contains("addLink_url")) {
                if (addLinks(ctx, params) > 0) {
                    msg = "Link added";
                } else {
                    msg = "Link not added";
                }                
            }
            try {
                showTags(ctx, msg);
            } catch (Exception ex) {
                Logger.getLogger(UrlHandler.class.getName()).log(Level.SEVERE, null, ex);
                ctx.fail(ex);
            }
        } else {
            ctx.fail(HttpResponseStatus.FORBIDDEN.code());
        }
    }

    private Collection<UrlTag> getTagsByName(String n) throws Exception {
        return ostore.getObjects(UrlTag.class, new String[] {UrlTag.FIELD_NAME}, new String[] {n});
    }
    
    private Collection<UrlTag> getTags() throws Exception {
        return ostore.getObjects(UrlTag.class, null, null, null, new String[] {UrlTag.FIELD_NAME}, null);
    }
    
    private int addTag(RoutingContext ctx, String name) {
        int ret = ostore.addObject(new UrlTag().setName(name));
        
        return ret;
    }

    private int addLinks(RoutingContext ctx, MultiMap params) {
        UrlLink ulink = new UrlLink();
        //addLink_url
        if (params.contains("addLink_url")) {
            ulink.setUrl(params.get("addLink_url"));
        }
        //addLink_title
        if (params.contains("addLink_title")) {
            ulink.setTitle(params.get("addLink_title"));
        } else {
            ulink.setTitle(ulink.getUrl());
        }
        //tags
        if (params.contains("tags")) {
            for (String tagid : params.getAll("tags")) {
                try {
                    long l = Long.parseLong(tagid);
                    ulink.getTags().add(new UrlTag().setId(l));
                } catch (Exception e) {
                    return 0;
                }
            }
        }
        return ostore.addObject(ulink);
    }
    
    private void showTags(RoutingContext ctx, String msg) throws Exception {
        String msgs = "<!DOCTYPE HTML>\n" +
"<html>\n" +
"<head>\n" +
"    <meta charset=\"utf-8\">\n" +
"    <link rel=\"stylesheet\" href=\"/www/css.css\">\n" +
"    <title>Add url</title>\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"<p><message/>\n" +
"\n" +
"<p>Add url:\n" +
"<form action=\"actionName\" method=\"POST\" name=\"urllinkform\">\n" +
"    <input placeholder=\"Url: https://www.wikipedia.org\" type=\"text\" name=\"addLink_url\" style=\"width: 100%;\" required=\"true\"/>\n" +
"    <input placeholder=\"Title: Wikipedia\" type=\"text\" name=\"addLink_title\" style=\"width: 100%;\" required=\"false\"/>\n" +
"    <messages/>\n" +
"    <input type=\"submit\" value=\"Submit\"/>\n" +
"</form>\n" +
"\n" +
"<p>Add tag:\n" +
"<form action=\"actionName\" method=\"POST\" name=\"urltagform\">\n" +
"    <input type=\"text\" name=\"addTag\" style=\"width: 100%;\" required=\"true\"/>\n" +
"    <input type=\"submit\" value=\"Submit\"/>\n" +
"</form>\n" +
"\n" +
"</body>\n" +
"</html>";
        secureSet(ctx.response());
        ctx.response().putHeader("content-type", "text/html").end(
                msgs
                        .replace("action=\"actionName\"", "action=\"\"")
                        .replace("<message/>", msg)
                        .replace("<messages/>", getTagList())
        );
    }
    
    private String getTagList() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"tags\" multiple style=\"width: 100%;\"  required=\"true\">");
        for (UrlTag t : getTags()) {
            sb.append("<option value=\""+t.getId()+"\">"+StringEscapeUtils.escapeHtml(t.getName())+"</option>");
        }
        sb.append("</select>");
        return sb.toString();
    }
    
    private int updateTag(UrlTag tag) {
        return ostore.updateObject(tag);
    }
    
    private int addUrlLink(UrlLink obj) {
        return ostore.addObject(obj);
    }
    
    private int updateUrlLink(UrlLink obj) {
        return ostore.updateObject(obj);
    }
    
    private int delUrlLink(UrlLink obj) {
        return ostore.deleteObject(obj);
    }
    
//    private Collection<UrlLink> getUrlLink(String ...tags) {
//        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        String sqlQ = "";
//        for (int i = 0; i < tags.length; i++) {
//            if (i > 0) {
//                sqlQ += ", ";
//            }
//            sqlQ += "?";
//        }
//        String sqlSelectTags = "select "+UrlTag.FIELD_ID+" from " + UrlTag.TABLE_NAME + " where " + UrlTag.FIELD_NAME + " IN ("+sqlQ + ")";
//        String sqlSelectLinks = "select distinct ";
//    }
}
/*
<!DOCTYPE HTML>
<html>
<head>
    <meta charset="utf-8">
    <link rel="stylesheet" href="/www/css.css">
    <title>Add url</title>
</head>
<body>

<p><message/>
<p>Search url:
<form action="actionName" method="POST" name="urllinksearchform">
    <messages/>
    <input type="submit" value="Submit"/>
    <input type="hidden" name="urllinksearch" value="1"/>
</form>

<p>Add url:
<form action="actionName" method="POST" name="urllinkform">
    <input placeholder="Url: https://www.wikipedia.org" type="text" name="addLink_url" style="width: 100%;" required="true"/>
    <input placeholder="Title: Wikipedia" type="text" name="addLink_title" style="width: 100%;" required="false"/>
    <messages/>
    <input type="submit" value="Submit"/>
</form>

<p>Add tag:
<form action="actionName" method="POST" name="urltagform">
    <input type="text" name="addTag" style="width: 100%;" required="true"/>
    <input type="submit" value="Submit"/>
</form>

</body>
</html>


*/