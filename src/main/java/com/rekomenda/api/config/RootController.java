package com.rekomenda.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class RootController {

    private final String frontendUrl;

    public RootController(@Value("${app.frontend-url}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/")
    public RedirectView root() {
        var redirect = new RedirectView(frontendUrl);
        redirect.setStatusCode(HttpStatus.TEMPORARY_REDIRECT);
        return redirect;
    }
}
