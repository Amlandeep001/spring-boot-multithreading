package com.poc.multithreading.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poc.multithreading.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long>
{
}
