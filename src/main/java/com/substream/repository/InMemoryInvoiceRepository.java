package com.substream.repository;

import com.substream.domain.Invoice;

import java.time.LocalDate;
import java.util.Optional;

/**
 * PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).
 * Invoices, looked up by their business key (BR-303). It works as soon as your
 * generic {@link InMemoryRepository} works (M2), because it searches {@code findAll()}.
 */
public class InMemoryInvoiceRepository extends InMemoryRepository<Invoice, String>
        implements InvoiceRepository {

    @Override
    public Optional<Invoice> findByKey(String subscriptionId, LocalDate periodStart) {
        return findAll().stream()
                .filter(invoice -> invoice.getSubscriptionId().equals(subscriptionId))
                .filter(invoice -> invoice.getPeriodStart().equals(periodStart))
                .findFirst();
    }
}
