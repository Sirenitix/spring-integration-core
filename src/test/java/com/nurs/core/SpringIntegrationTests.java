package com.nurs.core;

import com.nurs.core.dao.OrderRepository;
import com.nurs.core.dao.PaymentRepository;
import com.nurs.core.dto.OrderRequest;
import com.nurs.core.dto.PaymentRequest;
import com.nurs.core.dto.UpdateOrderRequest;
import com.nurs.core.entity.Order;
import com.nurs.core.entity.Payment;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpringIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	private static MockWebServer mockWebServer;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private PaymentRepository paymentRepository;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("exchange-rate-api.base-url", () -> mockWebServer.url("/").url().toString());
	}

	@BeforeAll
	static void setupMockWebServer() throws IOException {
		mockWebServer = new MockWebServer();
		mockWebServer.start();
	}


	@Test
	@org.junit.jupiter.api.Order(1)
	void createOrder() {
		BigDecimal amount = new BigDecimal("1000.00");
		String email = "n.suleev@yandex.ru";
		OrderRequest orderRequest = new OrderRequest(amount,email);
		webClient.post().uri("localhost:9090/order")
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(orderRequest), OrderRequest.class)
				.exchange()
				.expectStatus().isOk();
		Order actualOrder = orderRepository.findByEmail(email);
		assertThat(email).isEqualTo(actualOrder.getEmail());
		assertThat(amount).isEqualTo(actualOrder.getAmount());
		orderRepository.deleteAll();
	}



	@Test
	@org.junit.jupiter.api.Order(2)
	void updateOrder() {
		createOrder();

		Boolean testPaid = true;
		String testMail = "test@mail.com";
		String testDate = LocalDate.now().toString();
		BigDecimal testAmount = new BigDecimal("500.00");

		UpdateOrderRequest updateOrder = new UpdateOrderRequest();
		updateOrder.setDate(testDate);
		updateOrder.setAmount(testAmount);
		updateOrder.setPaid(testPaid);
		updateOrder.setEmail(testMail);

		int id = 1;
		webClient.put().uri("localhost:9090/order/" + id )
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(updateOrder), UpdateOrderRequest.class)
				.exchange()
				.expectStatus().isOk();

		Long orderId = 1L;
		Order order = orderRepository.findById(orderId).orElse(null);

		log.info(order + " - expected order");
		assert order != null;
		assertThat(order.isPaid()).isEqualTo(testPaid);
		assertThat(order.getAmount()).isEqualTo(testAmount);
		assertThat(order.getDate()).isEqualTo(testDate);
		assertThat(order.getEmail()).isEqualTo(testMail);
		orderRepository.deleteAll();
	}

	@Test
	@org.junit.jupiter.api.Order(3)
	void payOrder() {
		createOrder();
		Long testOrderId = 1L;
		String testCard = "4636790463393611";


		PaymentRequest paymentRequest = new PaymentRequest();
		paymentRequest.setOrderId(testOrderId);
		paymentRequest.setCreditCardNumber(testCard);

		int id = 1;
		webClient.post().uri("localhost:9090/order/" + id + "/payment")
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(paymentRequest), PaymentRequest.class)
				.exchange()
				.expectStatus().isOk();

		Order order = orderRepository.findById(testOrderId).orElse(null);
		Payment payment = paymentRepository.findByOrder(order);
		assertThat(payment.getOrder().getId()).isEqualTo(testOrderId);
		assertThat(payment.getCreditCardNumber()).isEqualTo(testCard);
		paymentRepository.deleteAll();
		orderRepository.deleteAll();
	}

	@Test
	@org.junit.jupiter.api.Order(4)
	void deleteOrder() {
		createOrder();
		int id = 1;
		webClient.delete().uri("localhost:9090/order/" + id )
				.exchange()
				.expectStatus().isOk();
		Long orderId = 1L;
		Order order = orderRepository.findById(orderId).orElse(null);
		assertThat(order).isEqualTo(null);
	}







}
