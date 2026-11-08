package io.github.michalmszczepanski.carreport.repository;

import io.github.michalmszczepanski.carreport.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);
}
