package com.substream.structure;

import com.substream.domain.Invoice;
import com.substream.domain.InvoiceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PROVIDED — DO NOT MODIFY.
 *
 * Checks that the class Invoice of M2 matches README section 4. InMemoryRepository needs no check here:
 * it implements the provided interface Repository, so the compiler already checks its methods.
 * Run it when you finish M2: right-click the file → Run.
 *
 * It checks the structure only: names, parameters, return types, private fields, no setters.
 * It does not check the logic. The rules of the milestone are checked by the NIX bot on every
 * Merge Request, and the final evaluation checks them with more cases.
 */
@Tag("structure")
@DisplayName("M2 structure")
class M2StructureTest {

    @Test
    @DisplayName("Invoice")
    void invoice() {
        StructureCheck check = new StructureCheck(Invoice.class);
        check.constructor(String.class, String.class, LocalDate.class, BigDecimal.class,
                LocalDate.class, LocalDate.class);
        check.getter(String.class, "getId");
        check.getter(String.class, "getSubscriptionId");
        check.getter(LocalDate.class, "getPeriodStart");
        check.getter(BigDecimal.class, "getAmount");
        check.getter(LocalDate.class, "getIssueDate");
        check.getter(LocalDate.class, "getDueDate");
        check.getter(InvoiceStatus.class, "getStatus");
        check.instanceMethod(void.class, "markPaid");
        check.instanceMethod(boolean.class, "isOverdue", LocalDate.class);
        check.fields("id", "subscriptionId", "periodStart", "amount", "issueDate", "dueDate", "status");
        check.verify();
    }
}
