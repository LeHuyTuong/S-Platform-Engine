package com.example.platform.downloader.ui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final String downloaderUiUrl;

    public HomeController(@Value("${app.ui.downloader-url:}") String downloaderUiUrl) {
        this.downloaderUiUrl = downloaderUiUrl;
    }

    @GetMapping("/")
    public String home() {
        if (downloaderUiUrl != null && !downloaderUiUrl.isBlank()) {
            return "redirect:" + downloaderUiUrl;
        }
        return "redirect:/downloader";
    }
}
