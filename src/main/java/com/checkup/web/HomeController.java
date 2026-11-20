package com.checkup.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles the home page.
 *
 * In the console version, Main.java printed a menu and read a number with Scanner.
 * In a web app, each "menu option" becomes a URL. When a browser asks for "/",
 * Spring calls home(), which fills in the Model and names the template to show.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "checkUp");
        return "home"; // renders src/main/resources/templates/home.html
    }
}
