package io.github.michalmszczepanski.carwatcher.repository;

import io.github.michalmszczepanski.carwatcher.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationToken(String verificationToken);
}
