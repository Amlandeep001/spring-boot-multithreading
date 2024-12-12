package com.poc.multithreading.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poc.multithreading.entity.Product;
import com.poc.multithreading.repository.ProductRepository;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ProductServiceV2
{
	private final ProductRepository repository;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final String topicName;

	private final ExecutorService executorService;

	public ProductServiceV2(ProductRepository repository, KafkaTemplate<String, String> kafkaTemplate,
			ObjectMapper objectMapper, @Value("${product.discount.update.topic}") String topicName)
	{
		this.repository = repository;
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
		this.topicName = topicName;

		executorService = Executors.newFixedThreadPool(6);
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

	public void executeProductIds(List<Long> productIds)
	{
		List<List<Long>> batches = splitIntoBatches(productIds, 50);

		List<CompletableFuture<Void>> futures = batches
				.stream()
				.map(
						batch -> CompletableFuture.runAsync(() -> processProductIds(batch), executorService))
				.collect(Collectors.toList());

		// wait for all future to complete
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
	}

	// batch List<Long> 1-50 , 2-100 ..

	private List<List<Long>> splitIntoBatches(List<Long> productIds, int batchSize)
	{
		int totalSize = productIds.size();
		// 300 - 100 -> 3
		int batchNums = (totalSize + batchSize - 1) / batchSize;
		// calculate number of batch
		List<List<Long>> batches = new ArrayList<>();

		for(int i = 0; i < batchNums; i++)
		{
			int start = i * batchSize;// 0 , 51 ,100
			int end = Math.min(totalSize, (i + 1) * batchSize);// 50 , 100
			batches.add(productIds.subList(start, end));
		}

		return batches;
	}

	private void processProductIds(List<Long> batch)
	{
		log.info("Processing batch {} by thread {} ", batch, Thread.currentThread().getName());
		batch.forEach(this::fetchUpdateAndPublish);
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
				.collect(Collectors.toList());
	}

}
