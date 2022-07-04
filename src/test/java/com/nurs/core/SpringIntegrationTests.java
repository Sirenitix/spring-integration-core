package com.nurs.core;

import com.nurs.core.dao.OrderRepository;
import com.nurs.core.dao.PaymentRepository;
import com.nurs.core.dto.OrderRequest;
import com.nurs.core.dto.TestPaymentRequest;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
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

	@AfterEach
	void deleteEntities() {
		paymentRepository.deleteAll();
		orderRepository.deleteAll();
	}


	@Test
	void deleteOrder() throws InterruptedException {
		createOrder();
		int id = 1;
		webClient.delete().uri("localhost:9090/order/" + id )
				.exchange()
				.expectStatus().isOk();
		TimeUnit.SECONDS.sleep(1);
		Long orderId = 1L;
		Order order = orderRepository.findById(orderId).orElse(null);
		assertThat(order).isEqualTo(null);
	}


	@Test
	void updateOrder() throws InterruptedException {
		createOrder();
		Boolean testPaid = true;
		String testMail = "test@mail.com";
		String testDate = LocalDate.now().toString();
		BigDecimal testAmount = new BigDecimal("500.00");

		UpdateOrderRequest updateOrder = new UpdateOrderRequest();
		updateOrder.setId(1L);
		updateOrder.setDate(testDate);
		updateOrder.setAmount(testAmount);
		updateOrder.setPaid(testPaid);
		updateOrder.setEmail(testMail);

		int id = 2;
		webClient.put().uri("localhost:9090/order/" + id )
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(updateOrder), UpdateOrderRequest.class)
				.exchange()
				.expectStatus().isOk();
		TimeUnit.SECONDS.sleep(1);

		Long orderId = 2L;
		Order order = orderRepository.findById(orderId).orElse(null);

		log.info(order + " - expected order");
		assert order != null;
		assertThat(order.isPaid()).isEqualTo(testPaid);
		assertThat(order.getAmount()).isEqualTo(testAmount);
		assertThat(order.getDate()).isEqualTo(testDate);
		assertThat(order.getEmail()).isEqualTo(testMail);
	}


	@Test
	void createOrder() throws InterruptedException {
		BigDecimal amount = new BigDecimal("1000.00");
		String email = "n.suleev@yandex.ru";
		OrderRequest orderRequest = new OrderRequest(amount,email);
		webClient.post().uri("localhost:9090/order")
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(orderRequest), OrderRequest.class)
				.exchange()
				.expectStatus().isOk();
		TimeUnit.SECONDS.sleep(1);
		Order actualOrder = orderRepository.findByEmail(email);
		assertThat(email).isEqualTo(actualOrder.getEmail());
		assertThat(amount).isEqualTo(actualOrder.getAmount());
	}


	@Test
	void payOrder() throws InterruptedException {
		createOrder();
		Long testOrderId = 4L;
		String testCard = "4146431922133966";


		TestPaymentRequest testPaymentRequest = new TestPaymentRequest();
		testPaymentRequest.setCreditCardNumber(testCard);


		webClient.post().uri("localhost:9090/order/"+ testOrderId.intValue() +"/payment")
				.accept(MediaType.APPLICATION_JSON)
				.body(Mono.just(testPaymentRequest), TestPaymentRequest.class)
				.exchange()
				.expectStatus().isOk();
		TimeUnit.SECONDS.sleep(1);
		Order order = orderRepository.findById(testOrderId).orElse(null);
		Payment payment = paymentRepository.findByOrder(order);
		assertThat(payment.getOrder().getId()).isEqualTo(testOrderId);
		assertThat(payment.getCreditCardNumber()).isEqualTo(testCard);
	}


	@Test
	void getOrder() throws InterruptedException {
		createOrder();
		Long testOrderId = 6L;
		AtomicReference<Order> requestOrder = new AtomicReference<>();
		webClient.get().uri("localhost:9090/order/"+ testOrderId.intValue())
				.exchange()
				.expectStatus().isOk()
				.expectBody(Order.class)
				.consumeWith(response -> {
					log.info(response + " - response");
					requestOrder.set(response.getResponseBody());
				});
		TimeUnit.SECONDS.sleep(1);
		Order order = orderRepository.findById(testOrderId).orElse(null);
		assert order != null;
		assertThat(order.getDate()).isEqualTo(requestOrder.get().getDate());
	}








}
