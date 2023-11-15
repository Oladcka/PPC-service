package project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.models.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Users findByLogin(String login);
}