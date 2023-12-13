package project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.models.NegPhrase;
import project.models.SearchQuery;

import java.util.List;

@Repository
public interface NegPhraseRepository extends JpaRepository<NegPhrase, Long> {
    List<NegPhrase> findAllBySearchQuery(SearchQuery searchQuery);
    List<NegPhrase> findAllByStatus(String status);
}
