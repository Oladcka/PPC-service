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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
        user.setActive(false);
        userRepository.save(user);
        Users savedUser = userRepository.save(user);
        Person person = new Person();
        person.setUser(savedUser);
        person.setName(name);
        person.setSurname(surname);
            String UPLOAD_DIRECTORY = "D:/Учёба/КП/CProject/src/main/resources/static/images/";
            Path fileNameAndPath = Paths.get(UPLOAD_DIRECTORY+images[0].getOriginalFilename());
        try {
            if (!Files.exists(fileNameAndPath)) {
                Files.createDirectories(fileNameAndPath.getParent()); // Создаем родительскую директорию, если она не существует
                Files.createFile(fileNameAndPath); // Создаем файл
            }

            BufferedOutputStream fio = new BufferedOutputStream(new FileOutputStream(fileNameAndPath.toFile()));
            byte[] buffer = images[0].getBytes();
            fio.write(buffer, 0, buffer.length);
            fio.flush();
            fio.close();
        }
        catch (Exception e) {
            person.setPhoto("../../images/acc.png");
            personRepository.save(person);
            return true;
        }
            person.setPhoto("../../images/" + images[0].getOriginalFilename());
            personRepository.save(person);
        return true;
    }
}
