package project.services;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project.models.Person;
import project.models.Users;
import project.repositories.PersonRepository;
import project.repositories.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService{
    private final PersonRepository personRepository;
    private final UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean createUser(Users user) {
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
        personRepository.save(person);
        return true;
    }
}
