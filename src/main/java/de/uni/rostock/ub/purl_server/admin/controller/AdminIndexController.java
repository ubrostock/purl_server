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
package de.uni.rostock.ub.purl_server.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class AdminIndexController {

    /**
     * Show the home page
     * 
     * @return the index page
     */
    @RequestMapping(path = "/admin/manager", method = RequestMethod.GET)
    public String showIndex() {
        return "index";
    }

    @RequestMapping(path = "/", method = RequestMethod.GET)
    public String showStart() {
        return "index";
    }

    /**
     * Show the help page
     * 
     * @return the help page
     */
    @RequestMapping(path = "/admin/manager/help", method = RequestMethod.GET)
    public String showHelp() {
        return "help";
    }
}
