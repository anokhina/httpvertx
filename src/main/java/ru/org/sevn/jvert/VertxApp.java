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
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class VertxApp {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions().setMaxEventLoopExecuteTime(2L * 1000 * 1000000));
        vertx.deployVerticle(new Main(), stringAsyncResult -> {
            System.out.println("BasicVerticle deployment complete");
        });
    }    
}
