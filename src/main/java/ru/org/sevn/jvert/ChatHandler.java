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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import static ru.org.sevn.jvert.HttpVerticle.secureSet;

public class ChatHandler implements io.vertx.core.Handler<RoutingContext> {

    private MessageStore messageStore = new MessageStore();
    
    static class Message {
        private Date date = new Date();
        private String from;
        private String to;
        private String msg;

        private Message(String uidfrom, String uidto, String msgStr) {
            setFrom(uidfrom);
            setTo(uidto);
            setMsg(msgStr);
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
    static class MessageStore {
        public HashMap<String, HashMap<String, List<Message>>> map = new HashMap();
        
        public Collection<Message> getMessages(String uidfrom, String uidto, Date ... days) {
            List<Message> chat = getUserChat(uidfrom, uidto, false);
            if (chat != null) {
                return new ArrayList<>(chat);
            } else {
                return new ArrayList<>();
            }
        }
        private List<Message> getUserChat(String uidfrom, String uidto, boolean create) {
            HashMap<String, List<Message>> chats = map.get(uidfrom);
            if (chats == null) {
                if (!create) {
                    return null;
                }
                chats = new HashMap<>();
                map.put(uidfrom, chats);
            }
            List<Message> ret = chats.get(uidto);
            if (ret == null) {
                if (!create) {
                    return null;
                }
                ret = new ArrayList<>();
                chats.put(uidto, ret);
            }
            return ret;
        }
        public void addMessage(String uidfrom, String uidto, String msgStr) {
            Message msg = new Message(uidfrom, uidto, msgStr);
            
            getUserChat(uidfrom, uidto, true).add(msg);
            getUserChat(uidto, uidfrom, true).add(msg);
        }
        
        public Collection<String> getChats(String uidfrom) {
            HashMap<String, List<Message>> chats = map.get(uidfrom);
            ArrayList<String> ret;
            if (chats != null) {
                ret = new ArrayList(chats.keySet());
            } else {
                ret = new ArrayList<>();
            }
            Collections.sort(ret);
            return ret;
        }
    }
    @Override
    public void handle(RoutingContext ctx) {
        // store in day
        // page by day
        // search
        // https://docs.oracle.com/javase/tutorial/essential/io/find.html
        // send email
        if (ctx.user() instanceof HttpVerticle.ExtraUser) {
            HttpVerticle.ExtraUser user = (HttpVerticle.ExtraUser)ctx.user();
            MultiMap params = ctx.request().params();
            if (params.contains("chatTo")) {
                if (params.contains("chatMsg")) {
                    messageStore.addMessage(user.getId(), params.get("chatTo"), params.get("chatMsg"));
                }
                
                StringBuilder sb = new StringBuilder();
                sb.append("<pre>");
                sb.append("Chats:\nchatTo=\nchatMsg=\n");
                sb.append("from:").append(user.getId()).append("\n");
                sb.append("to  :").append(params.get("chatTo")).append("\n");
                
                sb.append("<a href=\"?chatTo=").append(params.get("chatTo")).append("\">");
                sb.append("refresh").append("</a>").append("\n");
                
                for(Message m : messageStore.getMessages(user.getId(), params.get("chatTo"))) {
                    sb.append(m.getFrom()).append(":");
                    sb.append(m.getMsg()).append("\n");
                }
                sb.append("</pre>");
                
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
}
