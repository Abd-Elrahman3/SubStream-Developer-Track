package com.substream.repository;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

import com.substream.domain.Customer;

public class InMemoryCustomerRepository extends InMemoryRepository<Customer, String>
        implements CustomerRepository {
}
