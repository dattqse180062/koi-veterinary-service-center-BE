package org.ftf.koifishveterinaryservicecenter.model.fish;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.baopen753.koifishveterinaryservice.model.appointment.Appointment;
import org.baopen753.koifishveterinaryservice.model.image.Image;
import org.baopen753.koifishveterinaryservice.model.user.User;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "fish")
public class Fish {

    @EmbeddedId
    private FishId fishId;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "species", nullable = false, length = 50)
    private String species;

    @Column(name = "size", nullable = false, precision = 6, scale = 2)
    private BigDecimal size;

    @Column(name = "weight", nullable = false, precision = 6, scale = 2)
    private BigDecimal weight;

    @Column(name = "color", nullable = false, length = 45)
    private String color;

    @Column(name = "origin", nullable = false, length = 45)
    private String origin;


    // Bidirectional, identifying relationship
    // Owning side: Fish
    // Inverse side: User
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)     // A Fish must belong to a User
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private User user;

    // Bidirectional, non-identifying relationship
    // Owning side: Appointment
    // Inverse side: Fish
    @OneToMany(mappedBy = "fish")
    private Set<Appointment> appointments = new LinkedHashSet<>();

    // Bidirectional, identifying relationship
    // Owning side: Image
    // Inverse side: Fish
    @OneToMany(mappedBy = "fish")
    private Set<Image> images = new LinkedHashSet<>();


}
