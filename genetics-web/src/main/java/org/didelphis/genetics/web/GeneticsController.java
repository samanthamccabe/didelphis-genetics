package org.didelphis.genetics.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeneticsController {

	@RequestMapping (
			value    = "/test",
			method   = RequestMethod.GET
//			consumes = MediaType.APPLICATION_JSON_UTF8_VALUE
	)
	public String testRequest() {
		return "test";
	}
}
