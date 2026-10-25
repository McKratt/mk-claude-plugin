package com.example.shop.sales.billing.application.domain.service;

import com.example.shop.sales.billing.application.domain.model.Invoice;
import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderQuery;
import com.example.shop.sales.billing.application.port.in.FindInvoiceByOrderUseCase;
import com.example.shop.sales.billing.application.port.out.LoadInvoicePort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
public class FindInvoiceByOrderService implements FindInvoiceByOrderUseCase {

    private final LoadInvoicePort loadInvoicePort;

    @Override
    public Optional<Invoice> findByOrder(FindInvoiceByOrderQuery query) {
        return loadInvoicePort.loadByOrderId(query.orderId());
    }
}
