package de.uni.rostock.ub.purl_server.info.controller;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import de.uni.rostock.ub.purl_server.dao.PurlDAO;
import de.uni.rostock.ub.purl_server.model.Purl;

@Controller
public class PurlInfoController {

	@Autowired
	PurlDAO purlDAO;

	@RequestMapping(path = "/info/purl/**", method = RequestMethod.GET, produces = "!"
			+ MediaType.APPLICATION_JSON_VALUE)
	public Object retrieveInfoPurl(HttpServletRequest request, @RequestParam(defaultValue = "") String format) {
		if ("json".equals(format)) {
			return retrieveJSONPurl(request);
		} else {
			ModelAndView mav = new ModelAndView("purlinfo");
			String purlPath = "/"
					+ new AntPathMatcher().extractPathWithinPattern("info/purl/**", request.getRequestURI());
			Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
			if (op.isPresent() && op != null) {
				mav.addObject("purl", op.get());
				mav.addObject("purl_url", ServletUriComponentsBuilder.fromCurrentContextPath().path(purlPath).build().toString());
			} else {
				mav.addObject("errorList", "");
			}
			return mav;
		}
	}

	@RequestMapping(path = "/info/purl/**", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Purl> retrieveJSONPurl(HttpServletRequest request) {
		String purlPath = "/" + new AntPathMatcher().extractPathWithinPattern("/info/purl/**", request.getRequestURI());
		Optional<Purl> op = purlDAO.retrievePurlWithHistory(purlPath);
		if (op.isEmpty()) {
			return new ResponseEntity<Purl>(HttpStatus.NOT_FOUND);
		}
		ResponseEntity<Purl> r = new ResponseEntity<Purl>(op.get(), HttpStatus.OK);
		return r;
	}

}
