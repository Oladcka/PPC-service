package project.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity(name = "neg_phrase")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class NegPhrase {
    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "negPhrase", nullable = false, foreignKey = @ForeignKey(name = "fk_search_query_neg"))
    private SearchQuery searchQuery;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private String status;
}


