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
import org.springframework.web.multipart.MultipartFile;
import project.models.Clean;
import project.models.Person;
import project.models.Users;
import project.repositories.CleanRepository;
import project.repositories.PersonRepository;
import project.repositories.UserRepository;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ManageController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private CleanRepository cleanRepository;

    private Long personId;
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
    public String userCard(Model model, @RequestParam("personId") String id){
        System.out.println(id);
        personId = Long.valueOf(id);
        Person person = personRepository.getReferenceById(personId);
        List<Clean> cleans = cleanRepository.findAllByUser(person.getUser());
        model.addAttribute("cleans", cleans);
        model.addAttribute("person", person);
        return "person";
    }
    @PostMapping("/changeActive")
    public String changeActive(Model model){
        Person person = personRepository.getReferenceById(personId);
        Users user = person.getUser();
        user.setActive(!user.getActive());
        userRepository.save(user);
        List<Clean> cleans = cleanRepository.findAllByUser(person.getUser());
        model.addAttribute("cleans", cleans);
        model.addAttribute("person", person);
        return "person";
    }

    @PostMapping("/account")
    public String account(Model model) {
        User user1 = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users user = userRepository.findByLogin(user1.getUsername());
        model.addAttribute("person", personRepository.findAllByUser(user).get(0));
        return "account";
    }

    @PostMapping("/changeAccount")
    public String changeAccount(Model model, @RequestParam("name") String name, @RequestParam("surname") String surname, @RequestParam("login") String login, @RequestParam("image") MultipartFile[] images) {
        User user1 = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Users user = userRepository.findByLogin(user1.getUsername());
        Person person = personRepository.findAllByUser(user).get(0);
        try {
            user.setLogin(login);
            userRepository.save(user);
        }
        catch (Exception e) {
            model.addAttribute("person", person);
            model.addAttribute("error", "Пользователь с таким логином уже существует");
            return "account";
        }
        person.setName(name);
        try {
            String UPLOAD_DIRECTORY = "D:/Учёба/КП/CProject/src/main/resources/static/images/";
            Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY+images[0].getOriginalFilename());
            if (!Files.exists(fileNameAndPath)) {
                Files.createDirectories(fileNameAndPath.getParent()); // Создаем родительскую директорию, если она не существует
                Files.createFile(fileNameAndPath); // Создаем файл
            }

            BufferedOutputStream fio = new BufferedOutputStream(new FileOutputStream(fileNameAndPath.toFile()));
            byte[] buffer = images[0].getBytes();
            fio.write(buffer, 0, buffer.length);
            fio.flush();
            fio.close();
            person.setPhoto("../../images/" + images[0].getOriginalFilename());
        }
        catch (Exception e) {
        }
        personRepository.save(person);
        model.addAttribute("person", personRepository.findAllByUser(user).get(0));
        return "account";
    }
}
