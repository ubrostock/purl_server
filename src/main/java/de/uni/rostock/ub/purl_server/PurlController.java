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
package de.uni.rostock.ub.purl_server;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;

@Controller
public class PurlController {

	@Autowired
	PurlDAO purlDAO;

	@Autowired
	private MessageSource messages;

	private static Logger LOGGER = LoggerFactory.getLogger(PurlController.class);

	/**
	 * Resolve the Purl
	 * 
	 * @param request the HttpServletRequest
	 * @param resp    the HttpServletResponse
	 * @param domain  the PathVariable
	 * @return redirect to the target URL
	 */
	@RequestMapping(path = "/{domain:(?!admin)(?!api)(?!info)(?!webjars).*}/**", method = RequestMethod.GET)
	public String resolvePurl(HttpServletRequest request, HttpServletResponse resp, @PathVariable String domain) {
		String path = request.getServletPath();
		Optional<Purl> op = purlDAO.retrievePurl(path);
		if (op.isPresent()) {
			Purl p = op.get();
			switch (p.getType()) {
			case PARTIAL_302:
				String restPath = path.substring(p.getPath().length());
				return "redirect:" + calcRedirectWithParams(request, p.getTarget(), restPath);
			case FOUND_302:
				return "redirect:" + calcRedirectWithParams(request, p.getTarget(), "");
			case GONE_410:
				try {
					resp.sendError(HttpServletResponse.SC_GONE,
							messages.getMessage(
									"purl_server.error.purl_deleted", new Object[] { ServletUriComponentsBuilder
											.fromCurrentContextPath().path("/info/purl" + path).build().toString() },
									Locale.getDefault()));
				} catch (NoSuchMessageException | IOException e) {
					LOGGER.error(messages.getMessage("purl_server.error.sending.error", null, Locale.getDefault()), e);
				}
				break;
			}
		} else {
			try {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND,
						messages.getMessage("purl_server.error.purl.found", null, Locale.getDefault()));
			} catch (IOException e) {
				LOGGER.error(messages.getMessage("purl_server.error.sending.error", null, Locale.getDefault()), e);
			}
		}
		return null;
	}

	private String calcRedirectWithParams(HttpServletRequest request, String path, String restPath) {
		String redirect = path + restPath;
		String queryString = request.getQueryString();
		if (StringUtils.hasText(queryString)) {
			if (redirect.contains("?")) {
				redirect = redirect + "&" + queryString;
			} else {
				redirect = redirect + "?" + queryString;
			}
		}
		return redirect;
	}
}