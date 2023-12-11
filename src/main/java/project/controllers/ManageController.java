package project.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.models.Person;
import project.models.Users;
import project.repositories.PersonRepository;
import project.repositories.UserRepository;

import java.util.List;

@Controller
public class ManageController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PersonRepository personRepository;
    @GetMapping("/workers")
    public String showBriefs(Authentication authentication, Model model){
        User user1 = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users user = userRepository.findByLogin(user1.getUsername());
        List<Person> people = personRepository.findAll();
        people.remove(personRepository.findAllByUser(user).get(0));
        model.addAttribute("people", people);
        return "users";
    }

    @PostMapping("/userCard")
    public String userCard(Model model, @RequestParam("personId") long id){
        System.out.println(id);
        model.addAttribute("person", personRepository.getReferenceById(id));
        return "person";
    }
    @PostMapping("/changeActive")
    public String changeActive(Model model, @RequestParam("personId") long id){
        Person person = personRepository.getReferenceById(id);
        Users user = person.getUser();
        user.setActive(!user.getActive());
        userRepository.save(user);
        model.addAttribute("person", person);
        return "person";
    }
}
