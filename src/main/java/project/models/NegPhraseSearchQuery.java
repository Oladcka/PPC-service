//package project.models;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity(name = "neg_phrase_search_query")
//@AllArgsConstructor
//@NoArgsConstructor
//@Getter
//@Setter
//public class NegPhraseSearchQuery {
//    @Column(name = "id")
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Integer id;
//
//    @ManyToOne()
//    @JoinColumn(name = "query", nullable = false, foreignKey = @ForeignKey(name = "fk_neg_phrase_search_query_query"))
//    private SearchQuery searchQuery;
//
//    @ManyToOne()
//    @JoinColumn(name = "negphrase", nullable = false, foreignKey = @ForeignKey(name = "fk_neg_phrase_search_query_phrase"))
//    private NegPhrase negPhrase;
//}
