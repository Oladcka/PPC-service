package project.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import project.models.Users;
import project.repositories.PersonRepository;
import project.repositories.UserRepository;

import java.util.Optional;

@Controller
public class GreetingController {

    @Autowired
    private UserRepository userRepository;
    @GetMapping("/")
    public String home() {
        return "home";
    }

//    @PostMapping("/login")
//    public String login(Model model, @ModelAttribute("user") Users user){
//                Optional<Users> u = userRepository.findByLoginAndPassword(user.getLogin(), user.getPassword());
//        if(u.isEmpty()){
//            //model.addAttribute("errorMessage", "Данного пользователя не существует! Проверьте корректность введённых данных.");
//            return "redirect:/login";
//        }
//        return "redirect:/home";
//    }


}
