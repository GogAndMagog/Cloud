package org.fizz_buzz.cloud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.fizz_buzz.cloud.entity.listeners.UserListener;
import org.fizz_buzz.cloud.model.db.DatabaseConstraints;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = DatabaseConstraints.USERNAME_UNIQUE, columnNames = "name"))
@RequiredArgsConstructor
@NoArgsConstructor
@EntityListeners(UserListener.class)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @Max(20)
    @NonNull
    @Column(name = "name", length = 20, nullable = false)
    private String name;

    @NonNull
    @Column(name = "password", nullable = false)
    private String password;
}