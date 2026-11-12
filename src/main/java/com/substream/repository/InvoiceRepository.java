package com.substream.repository;

// PROVIDED — DO NOT MODIFY (the pipeline rejects changes to this file).

import com.substream.domain.Invoice;

import java.time.LocalDate;
import java.util.Optional;

public interface InvoiceRepository extends Repository<Invoice, String> {

    /** Looks up an invoice by its business key (BR-303). */
    Optional<Invoice> findByKey(String subscriptionId, LocalDate periodStart);
}
