package project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.models.Clean;

@Repository
public interface CleanRepository extends JpaRepository<Clean, Long> {

}

