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

import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GroupUserAuthorizer implements UserAuthorizer {
    final Set<String> groups = new HashSet<>();

    public GroupUserAuthorizer(JsonArray groups) {
        for (Object o : groups) {
            this.groups.add(o.toString());
        }
    }
    public GroupUserAuthorizer(Collection<String> groups) {
        for (String o : groups) {
            this.groups.add(o.toString());
        }
    }

    @Override
    public boolean isAllowed(User u, RoutingContext rc) {
        if (u instanceof ExtraUser) {
            ExtraUser user = (ExtraUser) u;
            if (groups.size() > 0) {
                for (String grp : this.groups) {
                    Set<String> groups = user.getGroups();
                    if (groups.contains("__forbidden__")) {
                        return false;
                    }
                    if (groups.contains(grp)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }
    
}
