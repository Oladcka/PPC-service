package project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.models.Clean;
import project.models.Users;

import java.util.List;

@Repository
public interface CleanRepository extends JpaRepository<Clean, Long> {
    List<Clean> findAllByUser(Users user);

}

