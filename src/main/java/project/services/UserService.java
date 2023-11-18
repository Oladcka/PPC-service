package project.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import project.models.Person;
import project.models.Users;
import project.repositories.PersonRepository;
import project.repositories.UserRepository;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean createUser(Users user, String name, String surname, MultipartFile[] images) {
        String login = user.getLogin();
        String password = user.getPassword();
        if (userRepository.findByLogin(login) != null) {
            return false;
        }
        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);
        userRepository.save(user);
        Users savedUser = userRepository.save(user);
        Person person = new Person();
        person.setUser(savedUser);
        person.setName(name);
        person.setSurname(surname);
        try {
            String UPLOAD_DIRECTORY = System.getProperty("user.dir") + "/src/main/resources/static/images/";
//            Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, images[0].getOriginalFilename());
//            StringBuilder fileNames = new StringBuilder();
//            File file = new File(UPLOAD_DIRECTORY + images[0].getOriginalFilename());
//            file.createNewFile();
//            Files.write(fileNameAndPath, images[0].getBytes());
            Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY, images[0].getOriginalFilename());
            Files.write(fileNameAndPath, images[0].getBytes());
            person.setPhoto("../../images/" + images[0].getOriginalFilename());
            personRepository.save(person);
//            for (MultipartFile image : images) {
//                fileNames.append(image.getOriginalFilename()).append(", ");
//                Files.write(fileNameAndPath, image.getBytes());
//                person.setPhoto("../../images/" + image.getOriginalFilename());
//                personRepository.save(person);
//            }

        } catch (IOException e) {

        }
        return true;
    }
}
