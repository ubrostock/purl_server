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

import java.beans.Transient;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class Purl implements PurlServerResponse {
    private int id = -1;

    private String path;

    private Domain domain;

    private int domainId;

    private Type type;

    private String target;

    private LocalDateTime created;

    private LocalDateTime lastmodified;

    private Status status;

    private List<PurlHistory> purlHistory;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUri() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(getPath()).build().toString();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastmodified() {
        return lastmodified;
    }

    public void setLastmodified(LocalDateTime lastmodified) {
        this.lastmodified = lastmodified;
    }

    public List<PurlHistory> getPurlHistory() {
        return purlHistory;
    }

    public void setPurlHistory(List<PurlHistory> purlHistory) {
        this.purlHistory = purlHistory;
    }

    @Transient
    public int getDomainId() {
        return domainId;
    }

    public void setDomainId(int domainId) {
        this.domainId = domainId;
    }

    @Transient
    public String getDomainPath() {
        int end = getPath().indexOf("/", 1);
        return end == -1 ? getPath() : getPath().substring(0, end);
    }

}
