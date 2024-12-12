package com.poc.multithreading.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "product", schema = "test")
public class Product
{
	@Id
	Long id;
	String name;
	String category;
	double price;
	boolean isOfferApplied;
	double discountPercentage;
	double priceAfterDiscount;
}
