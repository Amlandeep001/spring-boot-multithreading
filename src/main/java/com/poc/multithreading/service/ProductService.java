package com.poc.multithreading.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.multithreading.entity.Product;
import com.poc.multithreading.repository.ProductRepository;

import jakarta.transaction.Transactional;

@Service
public class ProductService
{
	private final ProductRepository repository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topicName;

	public ProductService(ProductRepository repository, KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper, @Value("${product.discount.update.topic}") String topicName)
	{
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topicName = topicName;
	}

	public String resetRecords()
	{
		repository.findAll()
				.forEach(product ->
				{
					product.setOfferApplied(false);
					product.setPriceAfterDiscount(product.getPrice());
					product.setDiscountPercentage(0);
					repository.save(product);
				});
		return "Data Reset to DB";
	}

	@Transactional
	public void processProductIds(List<Long> productIds)
	{
		productIds.parallelStream()
				.forEach(this::fetchUpdateAndPublish);
	}

	private void fetchUpdateAndPublish(Long productId)
	{
		// fetch product by id
		Product product = repository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product ID does not exist in the system"));

		// update discount properties
		updateDiscountedPrice(product);

		// save to DB
		repository.save(product);

		// kafka events
		publishProductEvent(product);
	}

	private void updateDiscountedPrice(Product product)
	{
		final double price = product.getPrice();
		final int discountPercentage = (price >= 1000) ? 10 : (price > 500 ? 5 : 0);
		final double priceAfterDiscount = price - (price * discountPercentage / 100);

		if(discountPercentage > 0)
		{
			product.setOfferApplied(true);
		}
		product.setDiscountPercentage(discountPercentage);
		product.setPriceAfterDiscount(priceAfterDiscount);
	}

	private void publishProductEvent(Product product)
	{
		try
		{
			final String productJson = objectMapper.writeValueAsString(product);
			kafkaTemplate.send(topicName, productJson);
		}
		catch(JsonProcessingException e)
		{
			throw new RuntimeException("Failed to convert product to JSON", e);
		}
	}

	public List<Long> getProductIds()
	{
		return repository.findAll()
				.stream()
				.map(Product::getId)
				.sorted()
				.collect(Collectors.toList());
	}
}
