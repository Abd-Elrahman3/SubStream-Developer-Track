package com.substream.repository;

import com.substream.domain.Customer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRepositoryTest {

    @Test
    void replacingAnIdPreservesInsertionOrderAndCount() {
        InMemoryCustomerRepository repository = new InMemoryCustomerRepository();
        Customer first = customer("first", "Original");
        Customer second = customer("second", "Second");
        Customer replacement = customer("first", "Replacement");
        repository.save(first);
        repository.save(second);
        assertEquals(replacement, repository.save(replacement));

        assertEquals(2, repository.count());
        assertEquals(List.of("first", "second"), repository.findAll().stream().map(Customer::getId).toList());
        assertEquals("Replacement", repository.findById("first").orElseThrow().getName());
        assertTrue(repository.findById("missing").isEmpty());
    }

    @Test
    void resultListIsUnmodifiableAndRepositoryInstancesAreIndependent() {
        InMemoryCustomerRepository first = new InMemoryCustomerRepository();
        InMemoryCustomerRepository second = new InMemoryCustomerRepository();
        first.save(customer("id", "Acme"));

        assertThrows(UnsupportedOperationException.class, () -> first.findAll().clear());
        assertEquals(1, first.count());
        assertEquals(0, second.count());
    }

    private Customer customer(String id, String name) {
        return new Customer(id, name, "billing@example.com", LocalDate.of(2024, 1, 1));
    }
}
