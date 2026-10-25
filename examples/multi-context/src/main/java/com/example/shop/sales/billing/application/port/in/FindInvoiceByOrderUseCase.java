package com.example.shop.sales.billing.application.port.in;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import java.util.Optional;

public interface FindInvoiceByOrderUseCase {

    Optional<Invoice> findByOrder(FindInvoiceByOrderQuery query);
}
