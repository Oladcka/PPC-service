package project.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "person")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Person {
    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne()
    @JoinColumn(name = "iduser", nullable = false, foreignKey = @ForeignKey(name = "fk_person_user"))
    private Users user;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = false)
    private String name;

    @Column (nullable = true)
    private String photo;
}



