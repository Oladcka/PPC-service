package project.controllers;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import project.models.Users;
import project.models.enums.Role;
import project.repositories.PersonRepository;
import project.services.UserService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;



@Controller
@RequiredArgsConstructor
public class AuthorizationController {
    private final UserService userService;
    @Autowired
    private PersonRepository personRepository;

    @PostMapping("/registration/reg_new_user")
    public String createUser(@ModelAttribute Users user, Model model, @RequestParam Map<String, String> form,@RequestParam("name") String name, @RequestParam("surname") String surname, @RequestParam("image") MultipartFile[] images) throws IOException {
        Set<String> roles = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());

        for (String key : form.keySet()) {
            if (roles.contains(form.get(key))) {
                user.getRole().add(Role.valueOf(form.get(key)));
            }
        }
        if (!userService.createUser(user, name, surname, images)) {
            model.addAttribute("errorMessage", "Пользователь с login: " + user.getUsername() +
                    " уже существует");
            return "registration";
        }
        else
        return "redirect:/login";
    }

}
