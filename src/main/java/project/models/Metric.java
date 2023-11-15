package project.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "metric")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Metric {
    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne()
    @JoinColumn(name = "idquery", nullable = false, foreignKey = @ForeignKey(name = "fk_metric_query"))
    private SearchQuery searchQuery;


    @Column(nullable = false)
    private Integer shows;

    @Column(nullable = false)
    private Integer clicks;

    @Column(nullable = false)
    private Integer conversions;

    @Column(nullable = false)
    private Float consumption;

    @Column(nullable = false)
    private String currencyCode;
}

