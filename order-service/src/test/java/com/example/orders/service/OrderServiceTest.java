package com.example.orders.service;

import com.example.orders.model.Order;
import com.example.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setCustomerName("John Doe");
        testOrder.setProduct("Widget");
        testOrder.setAmount(new BigDecimal("29.99"));
        testOrder.setStatus("PENDING");
    }

    @Test
    void getAllOrders_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder));
        List<Order> orders = orderService.getAllOrders();
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void getOrderById_existingId_returnsOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        Optional<Order> result = orderService.getOrderById(1L);
        assertThat(result).isPresent();
        assertThat(result.get().getProduct()).isEqualTo("Widget");
    }

    @Test
    void getOrderById_nonExistingId_returnsEmpty() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        Optional<Order> result = orderService.getOrderById(99L);
        assertThat(result).isEmpty();
    }

    @Test
    void createOrder_setsDefaultStatus() {
        Order newOrder = new Order();
        newOrder.setCustomerName("Jane");
        newOrder.setProduct("Gadget");
        newOrder.setAmount(new BigDecimal("49.99"));

        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        Order created = orderService.createOrder(newOrder);
        assertThat(created.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void updateOrderStatus_validTransition_succeeds() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order updated = orderService.updateOrderStatus(1L, "CONFIRMED");
        assertThat(updated.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void updateOrderStatus_invalidTransition_throwsException() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "DELIVERED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from PENDING to DELIVERED");
    }

    @Test
    void updateOrderStatus_cancelFromAnyStatus_succeeds() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order updated = orderService.updateOrderStatus(1L, "CANCELLED");
        assertThat(updated.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void updateOrderStatus_fromTerminalStatus_throwsException() {
        testOrder.setStatus("DELIVERED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "SHIPPED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from terminal status");
    }

    @Test
    void deleteOrder_callsRepository() {
        orderService.deleteOrder(1L);
        verify(orderRepository).deleteById(1L);
    }

    // ── getAllOrders ──────────────────────────────────────────────────

    @Test
    void getAllOrders_emptyRepository_returnsEmptyList() {
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());
        List<Order> orders = orderService.getAllOrders();
        assertThat(orders).isEmpty();
    }

    // ── getOrdersByStatus ─────────────────────────────────────────────

    @Test
    void getOrdersByStatus_matchingOrders_returnsList() {
        when(orderRepository.findByStatus("PENDING")).thenReturn(Arrays.asList(testOrder));
        List<Order> orders = orderService.getOrdersByStatus("PENDING");
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    void getOrdersByStatus_noMatchingOrders_returnsEmptyList() {
        when(orderRepository.findByStatus("SHIPPED")).thenReturn(Collections.emptyList());
        List<Order> orders = orderService.getOrdersByStatus("SHIPPED");
        assertThat(orders).isEmpty();
    }

    // ── createOrder ───────────────────────────────────────────────────

    @Test
    void createOrder_statusAlreadySet_preservesExistingStatus() {
        Order newOrder = new Order();
        newOrder.setCustomerName("Jane");
        newOrder.setProduct("Gadget");
        newOrder.setAmount(new BigDecimal("49.99"));
        newOrder.setStatus("CONFIRMED");

        when(orderRepository.save(any(Order.class))).thenReturn(newOrder);

        Order created = orderService.createOrder(newOrder);
        assertThat(created.getStatus()).isEqualTo("CONFIRMED");
    }

    // ── updateOrderStatus: valid transitions ──────────────────────────

    @Test
    void updateOrderStatus_confirmedToShipped_succeeds() {
        testOrder.setStatus("CONFIRMED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order updated = orderService.updateOrderStatus(1L, "SHIPPED");
        assertThat(updated.getStatus()).isEqualTo("SHIPPED");
    }

    @Test
    void updateOrderStatus_shippedToDelivered_succeeds() {
        testOrder.setStatus("SHIPPED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        Order updated = orderService.updateOrderStatus(1L, "DELIVERED");
        assertThat(updated.getStatus()).isEqualTo("DELIVERED");
    }

    // ── updateOrderStatus: invalid transitions ────────────────────────

    @Test
    void updateOrderStatus_confirmedToInvalidStatus_throwsException() {
        testOrder.setStatus("CONFIRMED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "PENDING"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from CONFIRMED to PENDING");
    }

    @Test
    void updateOrderStatus_shippedToInvalidStatus_throwsException() {
        testOrder.setStatus("SHIPPED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "CONFIRMED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from SHIPPED to CONFIRMED");
    }

    @Test
    void updateOrderStatus_cancelledIsTerminal_throwsException() {
        testOrder.setStatus("CANCELLED");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "PENDING"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition from terminal status");
    }

    @Test
    void updateOrderStatus_unknownCurrentStatus_throwsException() {
        testOrder.setStatus("UNKNOWN");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "CONFIRMED"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown status: UNKNOWN");
    }

    // ── updateOrderStatus: order not found ────────────────────────────

    @Test
    void updateOrderStatus_orderNotFound_throwsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, "CONFIRMED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found: 99");
    }
}
