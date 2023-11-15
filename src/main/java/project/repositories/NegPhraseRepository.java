package project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.models.NegPhrase;

@Repository
public interface NegPhraseRepository extends JpaRepository<NegPhrase, Long> {

}
