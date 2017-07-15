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
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        public Class getType() {
            return Message.class;
        }

        @Override
        public void mapValues(Message o, String colName, ResultSet rs) throws SQLException {
                        switch(colName) {
                case "ID":
                    o.setId(rs.getLong(colName));
                    break;
                case "IS_READ":
                    o.setRead(rs.getInt(colName) != 0);
                    break;
                case "CREATE_DATE":
                    o.setDate(rs.getDate(colName));
                    break;
                case "UFROM":
                    o.setFrom(rs.getString(colName));
                    break;
                case "UTO":
                    o.setTo(rs.getString(colName));
                    break;
                case "MSG":
                    o.setMsg(rs.getString(colName));
                    break;
            }
        }

        @Override
        public void setStatement(Message o, String colName, int parameterIndex, PreparedStatement pstmt) throws SQLException {
            switch(colName) {
                case "IS_READ":
                    pstmt.setInt(parameterIndex, o.isRead()? 1 : 0);
                    break;
                case "CREATE_DATE":
                    pstmt.setDate(parameterIndex, new java.sql.Date(o.getDate().getTime()));
                    break;
                case "UFROM":
                    pstmt.setString(parameterIndex, o.getFrom());
                    break;
                case "UTO":
                    pstmt.setString(parameterIndex, o.getTo());
                    break;
                case "MSG":
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

        
        private Message(String uidfrom, String uidto, String msgStr) {
            setFrom(uidfrom);
            setTo(uidto);
            setMsg(msgStr);
        }
        private Message(String uidfrom, String uidto, String msgStr, Date d) {
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
            Date day = new Date();
            if (days.length > 0 && days[0] != null) {
                day = days[0];
            }
            Calendar cfrom = Calendar.getInstance();
            Calendar cto = Calendar.getInstance();
            cfrom.setTime(day);
            cfrom.set(Calendar.HOUR_OF_DAY, 0);
            cfrom.set(Calendar.MINUTE, 0);
            cfrom.set(Calendar.SECOND, 0);
            cto.setTime(cfrom.getTime());
            cto.add(Calendar.DAY_OF_YEAR, 1);
            
            try {
                return (Collection<Message>)ostore.getObjects(Message.class,
                        new String[] { Message.FIELD_TO, Message.FIELD_DATE },
                        new Object[] {uidto, new Object[] {cfrom.getTime(), cto.getTime()} } );
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
            System.out.println("++++++++++"+uidfrom);
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
//    static class SQLiteMessageStore implements MessageStore {
//        
//        private final boolean loaded;
//        private final String filePath;
//        
//        public SQLiteMessageStore(String filePath) {
//            this.filePath = filePath;
//            boolean l = false;
//            try {
//                Class.forName("org.sqlite.JDBC");
//                l = true;
//            } catch (ClassNotFoundException ex) {
//                Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            loaded = l;
//            File dbFile = new File(filePath);
//            if (!dbFile.exists()) {
//                initDB();
//            }
//        }
//        
//        protected void initDB() {
//            if (loaded) {
//                try {
//                    Connection c = DriverManager.getConnection(getConnectionString());
//                    try {
//                        Statement stmt = c.createStatement();
//                        String sql = "CREATE TABLE CHAT_MSG " +
//                                     "(ID INTEGER PRIMARY KEY  AUTOINCREMENT   NOT NULL," +
//                                     " FROM_ID        TEXT    NOT NULL, " + 
//                                     " TO_ID          TEXT    NOT NULL, " + 
//                                     " MSG            TEXT    NOT NULL, " + 
//                                     " MSG_DATE       DATE)"; 
//                        stmt.executeUpdate(sql);
//                        stmt.close();
//                    } finally {
//                        c.close();                    
//                    }
//                } catch (SQLException ex) {
//                    Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        }
//        
//        protected String getConnectionString() {
//            return "jdbc:sqlite:"+filePath;
//        }
//
//        @Override
//        public Collection<Message> getMessages(String uidfrom, String uidto, Date... days) {
//            ArrayList<Message> ret = new ArrayList<>();
//            Date day = new Date();
//            if (days.length > 0 && days[0] != null) {
//                day = days[0];
//            }
//            Calendar cfrom = Calendar.getInstance();
//            Calendar cto = Calendar.getInstance();
//            cfrom.setTime(day);
//            cfrom.set(Calendar.HOUR_OF_DAY, 0);
//            cfrom.set(Calendar.MINUTE, 0);
//            cfrom.set(Calendar.SECOND, 0);
//            cto.setTime(cfrom.getTime());
//            cto.add(Calendar.DAY_OF_YEAR, 1);
//            if (loaded) {
//                try {
//                    Connection c = DriverManager.getConnection(getConnectionString());
//                    try {
//                        PreparedStatement pstmt = c.prepareStatement("SELECT DISTINCT * FROM CHAT_MSG WHERE MSG_DATE BETWEEN ? AND ? AND FROM_ID = ? AND TO_ID = ? OR FROM_ID = ? AND TO_ID = ? ORDER BY MSG_DATE DESC");
//                        pstmt.setDate(1, new java.sql.Date(cfrom.getTimeInMillis()));
//                        pstmt.setDate(2, new java.sql.Date(cto.getTimeInMillis()));
//                        pstmt.setString(3, uidfrom);
//                        pstmt.setString(4, uidto);
//                        pstmt.setString(5, uidto);
//                        pstmt.setString(6, uidfrom);
//                        
//                        ResultSet rs = pstmt.executeQuery();
//                        while (rs.next()) {
//                            Message msg = new Message(rs.getString("FROM_ID"), rs.getString("TO_ID"), rs.getString("MSG"), rs.getDate("MSG_DATE"));
//                            ret.add(msg);
//                        }
//                        pstmt.close();
//                    } finally {
//                        c.close();
//                    }
//                } catch (SQLException ex) {
//                    Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//            return ret;
//        }
//
//        @Override
//        public int addMessage(String uidfrom, String uidto, String msgStr) {
//            int ret = 0;
//            if (loaded) {
//                try {
//                    Connection c = DriverManager.getConnection(getConnectionString());
//                    try {
//
//                        PreparedStatement pstmt = c.prepareStatement("INSERT INTO CHAT_MSG (FROM_ID, TO_ID, MSG, MSG_DATE) VALUES (?, ?, ?, ?)");
//                        pstmt.setString(1, uidfrom);
//                        pstmt.setString(2, uidto);
//                        pstmt.setString(3, msgStr);
//                        pstmt.setDate(4, new java.sql.Date(new Date().getTime()));
//                        ret = pstmt.executeUpdate();
//                        pstmt.close();
//                    } finally {
//                        c.close();
//                    }
//                } catch (SQLException ex) {
//                    Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                
//            }
//            return ret;
//        }
//
//        @Override
//        public Collection<String> getChats(String uidfrom) {
//            HashSet<String> ret = new HashSet<>();
//            if (loaded) {
//                try {
//                    Connection c = DriverManager.getConnection(getConnectionString());
//                    try {
//                        PreparedStatement pstmt = c.prepareStatement("SELECT DISTINCT FROM_ID, TO_ID FROM CHAT_MSG WHERE FROM_ID = ? OR TO_ID = ?");
//                        pstmt.setString(1, uidfrom);
//                        pstmt.setString(2, uidfrom);
//                        
//                        ResultSet rs = pstmt.executeQuery();
//                        while (rs.next()) {
//                            String from = rs.getString("FROM_ID");
//                            String to = rs.getString("TO_ID");
//                            ret.add(from);
//                            ret.add(to);
//                        }
//                        pstmt.close();
//                    } finally {
//                        c.close();
//                    }
//                } catch (SQLException ex) {
//                    Logger.getLogger(ChatHandler.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//            return ret;
//        }
//        
//    }
    
//    static class MemoryMessageStore implements MessageStore {
//        public HashMap<String, HashMap<String, List<Message>>> map = new HashMap();
//        
//        public Collection<Message> getMessages(String uidfrom, String uidto, Date ... days) {
//            List<Message> chat = getUserChat(uidfrom, uidto, false);
//            if (chat != null) {
//                return new ArrayList<>(chat);
//            } else {
//                return new ArrayList<>();
//            }
//        }
//        private List<Message> getUserChat(String uidfrom, String uidto, boolean create) {
//            HashMap<String, List<Message>> chats = map.get(uidfrom);
//            if (chats == null) {
//                if (!create) {
//                    return null;
//                }
//                chats = new HashMap<>();
//                map.put(uidfrom, chats);
//            }
//            List<Message> ret = chats.get(uidto);
//            if (ret == null) {
//                if (!create) {
//                    return null;
//                }
//                ret = new ArrayList<>();
//                chats.put(uidto, ret);
//            }
//            return ret;
//        }
//        public int addMessage(String uidfrom, String uidto, String msgStr) {
//            Message msg = new Message(uidfrom, uidto, msgStr);
//            
//            getUserChat(uidfrom, uidto, true).add(msg);
//            getUserChat(uidto, uidfrom, true).add(msg);
//            return 1;
//        }
//        
//        public Collection<String> getChats(String uidfrom) {
//            HashMap<String, List<Message>> chats = map.get(uidfrom);
//            ArrayList<String> ret;
//            if (chats != null) {
//                ret = new ArrayList(chats.keySet());
//            } else {
//                ret = new ArrayList<>();
//            }
//            Collections.sort(ret);
//            return ret;
//        }
//    }
    
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
            if (params.contains("chatTo")) {
                if (params.contains("chatMsg")) {
                    messageStore.addMessage(user.getId(), params.get("chatTo"), params.get("chatMsg"));
                }
                
                StringBuilder sb = new StringBuilder();
                //url=http://webdesign.about.com/
                HttpServerRequest r = ctx.request();
                String url = r.absoluteURI().substring(0, r.absoluteURI().length() - r.uri().length()) + r.path() + "?chatTo=" + params.get("chatTo");
                sb.append("<html><head>");
                sb.append("<meta http-equiv=\"refresh\" content=\"10;url='"+url+"'\">");
                sb.append("</head><body>");
                sb.append("<pre>");
                sb.append("Chats:\nchatTo=\nchatMsg=\n");
                sb.append("from:").append(user.getId()).append("\n");
                sb.append("to  :").append(params.get("chatTo")).append("\n");
                
                sb.append("<a href=\"?chatTo=").append(params.get("chatTo")).append("\">");
                sb.append("refresh").append("</a>").append("\n");
                
                for(Message m : messageStore.getMessages(user.getId(), params.get("chatTo"))) {
                    sb.append(dateFormat.format(m.getDate())).append(">>>");
                    sb.append(m.getFrom()).append(":");
                    sb.append(m.getMsg()).append("\n");
                }
                sb.append("</pre>");
                sb.append("</body></html>");
                
                secureSet(ctx.response());
                ctx.response().putHeader("content-type", "text/html").end(sb.toString());
                
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("<pre>");
                sb.append("Chats:\nchatTo=\nchatMsg=\n");
                for(String toid : messageStore.getChats(user.getId())) {
                    sb.append("<a href=\"?chatTo=").append(toid).append("\">");
                    sb.append(toid).append("</a>").append("\n");
                }
                sb.append("</pre>");
                
                secureSet(ctx.response());
                ctx.response().putHeader("content-type", "text/html").end(sb.toString());
            }
        } else {
            ctx.fail(HttpResponseStatus.FORBIDDEN.code());
        }

    }
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
}
