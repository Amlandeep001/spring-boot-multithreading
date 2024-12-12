package com.poc.multithreading.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.poc.multithreading.service.ProductService;
import com.poc.multithreading.service.ProductServiceV2;

@RestController
@RequestMapping("/api/products")
public class ProductController
{
	private final ProductService productService;
	private final ProductServiceV2 productServiceV2;

	public ProductController(ProductService productService, ProductServiceV2 productServiceV2)
	{
		this.productService = productService;
		this.productServiceV2 = productServiceV2;
	}

	// this endpoint is for testing
	@GetMapping("/ids")
	public ResponseEntity<List<Long>> getIds()
	{
		return ResponseEntity.ok(productService.getProductIds());
	}

	// this endpoint is for data reset
	@PutMapping("/reset")
	public ResponseEntity<String> resetProductRecords()
	{
		String response = productService.resetRecords();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/process")
	public ResponseEntity<String> processProductIds(@RequestBody List<Long> productIds)
	{
		productService.processProductIds(productIds);
		return ResponseEntity.ok("Products processed and events published.");
	}

	@PostMapping("/process/v2")
	public ResponseEntity<String> processProductIdsV2(@RequestBody List<Long> productIds)
	{
		productServiceV2.executeProductIds(productIds);
		return ResponseEntity.ok("Products processed and events published.");
	}

}
