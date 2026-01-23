package com.example.myapi.customers.infrastructure.repository;

import com.jnzader.apigen.core.domain.repository.BaseRepository;
import com.example.myapi.customers.domain.entity.Customer;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends BaseRepository<Customer, Long> {

    // Custom query methods

    Optional<Customer> findByEmail(String email);
}
