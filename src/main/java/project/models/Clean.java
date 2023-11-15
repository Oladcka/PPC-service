package project.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity(name = "clean")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Clean {
    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne()
    @JoinColumn(name = "specialyst", nullable = false, foreignKey = @ForeignKey(name = "fk_clean_user"))
    private Users user;

    @OneToMany(mappedBy= "clean")
    private List<SearchQuery> searchQueries;

    @Column(nullable = false)
    private Date date;

    @Column(name = "addSystem", nullable = false)
    private String advertisingSystem;

    @Column(nullable = false)
    private String project;
}
