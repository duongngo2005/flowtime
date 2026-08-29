package com.ndd.flowtime_be.google_account.repository;

import com.ndd.flowtime_be.google_account.entity.GoogleAccount;
import com.ndd.flowtime_be.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, Long> {

    Optional<GoogleAccount> findByUser(User user);

    boolean existsByUser(User user);
}
