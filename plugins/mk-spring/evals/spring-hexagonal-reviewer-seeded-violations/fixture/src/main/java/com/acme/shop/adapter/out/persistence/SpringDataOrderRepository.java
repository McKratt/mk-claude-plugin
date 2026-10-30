package com.acme.shop.adapter.out.persistence;

import com.acme.shop.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface SpringDataOrderRepository extends JpaRepository<Order, UUID> {
}
