package com.cushion.cushion_backend.model;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "clients")
@Data
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;
    private String role = "USER";

    private String phone;

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL)
    private Cart cart;
}

