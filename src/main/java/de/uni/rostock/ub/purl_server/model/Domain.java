/*
 * Copyright 2021 University Library, 18051 Rostock, Germany
 *
 * This file is part of the application "PURL Server".
 * https://github.com/ubrostock/purl_server
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.uni.rostock.ub.purl_server.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Domain {

    public static final List<String> RESERVED_DOMAINS = List.of("admin", "api", "info", "static", "webjars");

    public static final String REGEX_VALID_DOMAINS = "(?!\\badmin\\b|\\bapi\\b|\\binfo\\b|\\bstatic\\b|\\bwebjars\\b).+";
    //dynamic creation did not work -- needs to be constant at compile time to be used in annotation:
    //public static final String REGEX_RESERVED_DOMAINS = RESERVED_DOMAINS.stream()
    //    .map(x -> "\\b" + x + "\\b")
    //    .collect(Collectors.joining("|", "(?!", ")"))
    //    + ".+";

    private int id = -1;

    private String path;

    private String name;

    private String comment;

    private Instant created;

    private Instant lastmodified;

    private Status status;

    private List<DomainUser> domainUserList = new ArrayList<DomainUser>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(Instant lastmodified) {
        this.lastmodified = lastmodified;
    }

    public List<DomainUser> getDomainUserList() {
        return domainUserList;
    }
}
