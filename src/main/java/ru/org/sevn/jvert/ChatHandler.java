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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringEscapeUtils;
import ru.org.sevn.common.data.DBProperty;
import ru.org.sevn.common.data.DBTableProperty;
import ru.org.sevn.common.data.SimpleSqliteObjectStore;
import static ru.org.sevn.jvert.HttpVerticle.secureSet;

public class ChatHandler implements io.vertx.core.Handler<RoutingContext> {

    private MessageStore messageStore;
    
    public ChatHandler(SimpleSqliteObjectStore os) {
        messageStore = new DBMessageStore(os);
    }
    
    public static class MessageMapper implements SimpleSqliteObjectStore.ObjectMapper<Message> {

        @Override
        public Class getObjectType() {
            return Message.class;
        }

        @Override
        public void mapValues(Message o, String colName, ResultSet rs) throws SQLException {
                        switch(colName) {
                case Message.FIELD_ID:
                    o.setId(rs.getLong(colName));
                    break;
                case Message.FIELD_READ:
                    o.setRead(rs.getInt(colName) != 0);
                    break;
                case Message.FIELD_DATE:
                    o.setDate(rs.getDate(colName));
                    break;
                case Message.FIELD_FROM:
                    o.setFrom(rs.getString(colName));
                    break;
                case Message.FIELD_TO:
                    o.setTo(rs.getString(colName));
                    break;
                case Message.FIELD_MSG:
                    o.setMsg(rs.getString(colName));
                    break;
            }
        }

        @Override
        public void setStatement(Message o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException {
            switch(colName) {
                case Message.FIELD_READ:
                    pstmt.setInt(parameterIndex, o.isRead()? 1 : 0);
                    break;
                case Message.FIELD_DATE:
                    pstmt.setDate(parameterIndex, new java.sql.Date(o.getDate().getTime()));
                    break;
                case Message.FIELD_FROM:
                    pstmt.setString(parameterIndex, o.getFrom());
                    break;
                case Message.FIELD_TO:
                    pstmt.setString(parameterIndex, o.getTo());
                    break;
                case Message.FIELD_MSG:
                    pstmt.setString(parameterIndex, o.getMsg());
                    break;
            }
        }
    }
    
    @DBTableProperty(name = Message.TABLE_NAME)
    public static class Message {
        //TODO
        public static final String TABLE_NAME = "CHAT_MSG";
        public static final String FIELD_ID = "ID";
        @DBProperty(name = FIELD_ID, dtype = "INTEGER PRIMARY KEY  AUTOINCREMENT   NOT NULL")
        private long id;
        public static final String FIELD_READ = "IS_READ";
        @DBProperty(name = FIELD_READ, dtype = "INTEGER")
        private boolean read;
        public static final String FIELD_DATE = "CREATE_DATE";
        @DBProperty(name = FIELD_DATE, dtype = "DATE NOT NULL")
        private Date date = new Date();
        public static final String FIELD_FROM = "UFROM";
        @DBProperty(name = FIELD_FROM, dtype = "TEXT NOT NULL")
        private String from;
        public static final String FIELD_TO = "UTO";
        @DBProperty(name = FIELD_TO, dtype = "TEXT NOT NULL")
        private String to;
        public static final String FIELD_MSG = "MSG";
        @DBProperty(name = FIELD_MSG, dtype = "TEXT NOT NULL")
        private String msg;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public boolean isRead() {
            return read;
        }

        public void setRead(boolean read) {
            this.read = read;
        }

        public Message() {}
        
        public Message(String uidfrom, String uidto, String msgStr) {
            setFrom(uidfrom);
            setTo(uidto);
            setMsg(msgStr);
        }
        public Message(String uidfrom, String uidto, String msgStr, Date d) {
            setFrom(uidfrom);
            setTo(uidto);
            setMsg(msgStr);
            setDate(d);
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
        
    }
    
    interface MessageStore {
        Collection<Message> getMessages(String uidfrom, String uidto, Date ... days);
        int addMessage(String uidfrom, String uidto, String msgStr);
        Collection<String> getChats(String uidfrom);
    }
    
    public static class DBMessageStore implements MessageStore {

        private final SimpleSqliteObjectStore ostore;
        
        public DBMessageStore(SimpleSqliteObjectStore os) {
            this.ostore = os;
        }

        public SimpleSqliteObjectStore getOstore() {
            return ostore;
        }
        
        @Override
        public Collection<Message> getMessages(String uidfrom, String uidto, Date... days) {
            Calendar cfrom = Calendar.getInstance();
            Calendar cto = Calendar.getInstance();
            if (days.length > 0 && days[0] != null) {
                cfrom.setTime(days[0]);
            } else {
                if (days.length > 1 && days[1] != null) {
                    cfrom.setTime(days[1]);
                    cfrom.add(Calendar.DAY_OF_YEAR, -1);
                } else {
                    cfrom.setTime(new Date());
                    cfrom.set(Calendar.HOUR_OF_DAY, 0);
                    cfrom.set(Calendar.MINUTE, 0);
                    cfrom.set(Calendar.SECOND, 0);
                }
            }
            if (days.length > 1 && days[1] != null) {
                cto.setTime(days[1]);
            } else {
                cto.setTime(cfrom.getTime());
                cto.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            try {
                return (Collection<Message>)ostore.getObjects(Message.class,
                            "SELECT * FROM ("+
                                    " SELECT * FROM "+Message.TABLE_NAME+
                                    " WHERE "+Message.FIELD_FROM+" = ? AND "+Message.FIELD_TO+" = ? AND " + Message.FIELD_DATE + " BETWEEN ? AND ? " +
                                    " UNION " +
                                    " SELECT * FROM "+Message.TABLE_NAME+
                                    " WHERE "+Message.FIELD_FROM+" = ? AND "+Message.FIELD_TO+" = ? AND " + Message.FIELD_DATE + " BETWEEN ? AND ? " +
                                    " ) ORDER BY " + Message.FIELD_DATE + " DESC "
                        ,
                        new String[] { Message.FIELD_FROM, Message.FIELD_TO, Message.FIELD_DATE,  Message.FIELD_FROM, Message.FIELD_TO, Message.FIELD_DATE},
                        new Object[] { uidfrom, uidto, new Object[] {cfrom.getTime(), cto.getTime()} , 
                            uidto, uidfrom, new Object[] {cfrom.getTime(), cto.getTime()} },
                        new String[] {Message.FIELD_DATE}, new String[] {"DESC"});
            } catch (Exception ex) {
                Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }

        @Override
        public int addMessage(String uidfrom, String uidto, String msgStr) {
            Message msg = new Message(uidfrom, uidto, msgStr, new Date());
            return ostore.addObject(msg);
        }

        @Override
        public Collection<String> getChats(String uidfrom) {
            HashSet<String> ret = new HashSet<>();
            try {
                //TODO
                Connection c = DriverManager.getConnection(ostore.getConnectionString());
                try {
                    PreparedStatement pstmt = c.prepareStatement(
                            "SELECT DISTINCT "+Message.FIELD_FROM+", "+Message.FIELD_TO+
                                    " FROM "+Message.TABLE_NAME+
                                    " WHERE "+Message.FIELD_FROM+" = ? OR "+Message.FIELD_TO+" = ?");
                    pstmt.setString(1, uidfrom);
                    pstmt.setString(2, uidfrom);

                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        String from = rs.getString(Message.FIELD_FROM);
                        String to = rs.getString(Message.FIELD_TO);
                        ret.add(from);
                        ret.add(to);
                    }
                    pstmt.close();
                } finally {
                    c.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            return ret;
        }
        
    }
    
    private String getHtmlDateStr(Date d) {
        if (d != null) {
            return dateFormatHHmm.format(d);
        }
        return "";
    }
    private Date parseDate(String s) {
        ParseException ex = null;
        try {
            return dateFormat.parse(s);
        } catch (ParseException e) {
            ex = e;
        }
        try {
            return dateFormatHHmm.parse(s);
        } catch (ParseException e) {
            ex = e;
        }
        try {
            return dateFormatShort.parse(s);
        } catch (ParseException e) {
            ex = e;
        }
        Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
        return null;
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        // store in day
        // page by day
        // search
        // https://docs.oracle.com/javase/tutorial/essential/io/find.html
        // send email
        if (ctx.user() instanceof ExtraUser) {
            ExtraUser user = (ExtraUser)ctx.user();
            MultiMap params = ctx.request().params();
            Date chatDate1 = null;
            Date chatDate2 = null;
            if (params.contains("chatDate1")) {
                chatDate1 = parseDate(params.get("chatDate1"));
            }
            if (params.contains("chatDate2")) {
                chatDate2 = parseDate(params.get("chatDate2"));
            }
            if (params.contains("chatTo")) {
                if (params.contains("chatMsg")) {
                    messageStore.addMessage(user.getId(), params.get("chatTo"), params.get("chatMsg"));
                }
                
                StringBuilder sb = new StringBuilder();
                //url=http://webdesign.about.com/
                HttpServerRequest r = ctx.request();
                String url = r.absoluteURI().substring(0, r.absoluteURI().length() - r.uri().length()) + r.path();

                sb.append("Chat\n");
                sb.append("from:").append(user.getId()).append("\n");
                sb.append("to  :").append(params.get("chatTo")).append("\n");
                
                //<input type="datetime-local" name="bdaytime">
                /*
<p><form action="">
  From (date and time):
  <input type="datetime-local" name="chatDate1" value="">
  To (date and time):
  <input type="datetime-local" name="chatDate2" value="">                
  <input type="submit" value="Refresh">
  <input type="hidden" name="chatTo" value="user"/>
</form>
                */
                String refresh = "<p><form action=\"\">\n" +
"  From (date and time):\n" +
"  <input type=\"datetime-local\" name=\"chatDate1\" value=\"\">\n" +
"  To (date and time):\n" +
"  <input type=\"datetime-local\" name=\"chatDate2\" value=\"\">                \n" +
"  <input type=\"submit\" value=\"Refresh\">\n" +
"  <input type=\"hidden\" name=\"chatTo\" value=\"user\"/>\n" +
"</form>";
//                sb.append("<a href=\"").append(r.absoluteURI()).append("\">");
//                sb.append("refresh").append("</a>").append("\n");
                sb.append(refresh
                        .replace("value=\"user\"", "value=\""+params.get("chatTo")+"\"")
                        .replace("action=\"\"", "action=\""+url+"\"")
                        .replace("name=\"chatDate1\" value=\"\"", "name=\"chatDate1\" value=\""+getHtmlDateStr(chatDate1)+"\"")
                        .replace("name=\"chatDate2\" value=\"\"", "name=\"chatDate2\" value=\""+getHtmlDateStr(chatDate2)+"\"")
                );
                
                for(Message m : messageStore.getMessages(user.getId(), params.get("chatTo"), chatDate1, chatDate2)) {
                    sb.append(dateFormat.format(m.getDate())).append(">>>");
                    sb.append(StringEscapeUtils.escapeHtml(m.getFrom())).append(":");
                    sb.append(addUrls(StringEscapeUtils.escapeHtml(m.getMsg()))).append("\n");
                }
                
                secureSet(ctx.response());
                ctx.response().putHeader("content-type", "text/html").end(
                        STR_CHAT
                                .replace("action=\"actionName\"", "action=\"\"")
                                .replace("input type=\"hidden\" name=\"chatTo\" value=\"user\"", "input type=\"hidden\" name=\"chatTo\" value=\""+params.get("chatTo")+"\"")
                                .replace("<messages/>", sb.toString())                        
                );
                
            } else {
                
                StringBuilder sb = new StringBuilder();
                for(String toid : messageStore.getChats(user.getId())) {
                    sb.append("<a href=\"?chatTo=").append(toid).append("\">");
                    sb.append(toid).append("</a>").append("\n");
                }
                
                secureSet(ctx.response());
                ctx.response().putHeader("content-type", "text/html").end(
                        STR_CHATS
                                .replace("action=\"actionName\"", "action=\"\"")
                                .replace("<messages/>", sb.toString())
                );
            }
        } else {
            ctx.fail(HttpResponseStatus.FORBIDDEN.code());
        }

    }
    
    private String addUrls(String str) {
        String ustr = StringEscapeUtils.unescapeHtml(str);
        Matcher urlMatcher = urlPattern.matcher(ustr);
        String ret = str;
        HashSet<String> uset = new HashSet<>();
        while (urlMatcher.find()) {
            String url = ustr.substring(urlMatcher.start(0), urlMatcher.end(0));
            uset.add(url);
        }
        for(String url : uset) {
            String eurl = StringEscapeUtils.escapeHtml(url);
            ret = ret.replace(eurl, "<a href=\""+url+"\">"+url+"</a>");
        }
        return ret;
    }

    private Pattern urlPattern = Pattern.compile(
            "\\b((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;\\*]*[-a-zA-Z0-9+&@#/%=~_|\\*])");
    private static final String STR_CHAT = "<!DOCTYPE HTML>\n" +
"<html>\n" +
"<head>\n" +
"    <meta charset=\"utf-8\">\n" +
"    <link rel=\"stylesheet\" href=\"/www/css.css\">\n" +
"    <title>title</title>\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"<form action=\"actionName\" method=\"POST\" name=\"chatform\">\n" +
"    <textarea            name=\"chatMsg\" rows=\"14\" style=\"width: 100%;\" wrap=\"soft\"></textarea>\n" +
"    <input type=\"hidden\" name=\"chatTo\" value=\"user\"/>\n" +
"    <input type=\"submit\"/>\n" +
"</form>\n" +
"<pre>\n" +
"<messages/>\n" +
"</pre>\n" +
"\n" +
"</body>\n" +
"</html>";
    private static final String STR_CHATS = "<!DOCTYPE HTML>\n" +
"<html>\n" +
"<head>\n" +
"    <meta charset=\"utf-8\">\n" +
"    <link rel=\"stylesheet\" href=\"/www/css.css\">\n" +
"    <title>Chats</title>\n" +
"</head>\n" +
"<body>\n" +
"\n" +
"<form action=\"actionName\" method=\"POST\" name=\"chatform\">\n" +
"    <input type=\"text\" name=\"chatTo\" style=\"width: 100%;\"/>\n" +
"    <textarea          name=\"chatMsg\" rows=\"14\" style=\"width: 100%;\" wrap=\"soft\"></textarea>\n" +
"    <input type=\"submit\"/>\n" +
"</form>\n" +
"<pre>\n" +
"<messages/>\n" +
"</pre>\n" +
"\n" +
"</body>\n" +
"</html>";
    
    private SimpleDateFormat dateFormatShort = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat dateFormatHHmm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
}
/*
<!DOCTYPE HTML>
<html>
<head>
    <meta charset="utf-8">
    <link rel="stylesheet" href="/www/css.css">
    <title>Chats</title>
</head>
<body>

<form action="actionName" method="POST" name="chatform">
    <input type="text" name="chatTo" style="width: 100%;"/>
    <textarea          name="chatMsg" rows="14" style="width: 100%;" wrap="soft"></textarea>
    <input type="submit"/>
</form>
<pre>
<messages/>
</pre>

</body>
</html>

<!DOCTYPE HTML>
<html>
<head>
    <meta charset="utf-8">
    <link rel="stylesheet" href="/www/css.css">
    <title>title</title>
</head>
<body>

<form action="actionName" method="POST" name="chatform">
    <textarea            name="chatMsg" rows="14" style="width: 100%;" wrap="soft"></textarea>
    <input type="hidden" name="chatTo" value="user"/>
    <input type="submit"/>
</form>
<pre>
<messages/>
</pre>

</body>
</html>
*/