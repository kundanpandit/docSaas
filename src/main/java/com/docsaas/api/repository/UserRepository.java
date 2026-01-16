package com.docsaas.api.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.docsaas.api.model.User;

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
