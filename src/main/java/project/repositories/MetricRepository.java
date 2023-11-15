package project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import project.models.Metric;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

}
