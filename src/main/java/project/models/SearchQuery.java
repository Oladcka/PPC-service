package project.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity(name = "search_query")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SearchQuery {
    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "cleaning", nullable = false, foreignKey = @ForeignKey(name = "fk_search_query_clean"))
    private Clean clean;

    @OneToMany(mappedBy= "searchQuery")
    List<NegPhrase> negPhrases;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private String campaign;

    @Column(nullable = false)
    private String addGroup;

    @Column(nullable = false)
    private String keyword;
}
