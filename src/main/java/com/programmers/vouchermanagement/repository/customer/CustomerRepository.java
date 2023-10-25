package com.programmers.vouchermanagement.repository.customer;

import com.programmers.vouchermanagement.domain.customer.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    List<Customer> findAll();

    Optional<Customer> findById(UUID customerId);

    List<Customer> findByName(String name);

    List<Customer> findBannedCustomers();

    Customer save(Customer customer);

    Customer update(Customer customer);

    void delete(UUID id);
}
