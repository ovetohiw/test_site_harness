package com.perfumeshop.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageRedirectController {

    @GetMapping("/catalog.html")
    public String redirectCatalogHtmlToHome() {
        return "redirect:/";
    }
}
