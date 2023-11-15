package project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.models.SearchQuery;

@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, Long> {

}