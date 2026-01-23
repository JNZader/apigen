package com.example.myapi.orders.application.service;

import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.jnzader.apigen.core.application.util.Result;
import com.example.myapi.orders.domain.entity.Order;
import com.example.myapi.orders.infrastructure.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.persistence.EntityManager;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private CacheEvictionService cacheEvictionService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditorAware<String> auditorAware;

    @Mock
    private EntityManager entityManager;

    private OrderServiceImpl service;

    private Order order;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(repository, cacheEvictionService, eventPublisher, auditorAware);
        // Inject mock EntityManager for batch operations (saveAll uses flush/clear)
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
        order = new Order();
        order.setId(1L);
        order.setEstado(true);
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find Order by ID successfully")
        void shouldFindOrderById() {
            when(repository.findById(1L)).thenReturn(Optional.of(order));

            Result<Order, Exception> result = service.findById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(order);
            verify(repository).findById(1L);
        }

        @Test
        @DisplayName("Should return failure when Order not found by ID")
        void shouldReturnFailureWhenNotFound() {
            when(repository.findById(anyLong())).thenReturn(Optional.empty());

            Result<Order, Exception> result = service.findById(999L);

            assertThat(result.isFailure()).isTrue();
            verify(repository).findById(999L);
        }

        @Test
        @DisplayName("Should find all Order with pagination")
        void shouldFindAllWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(order));
            when(repository.findAll(pageable)).thenReturn(page);

            Result<Page<Order>, Exception> result = service.findAll(pageable);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow().getContent()).hasSize(1);
            verify(repository).findAll(pageable);
        }

        @Test
        @DisplayName("Should find all active Order with pagination")
        void shouldFindAllActiveWithPagination() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Order> page = new PageImpl<>(List.of(order));
            when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            Result<Page<Order>, Exception> result = service.findAllActive(pageable);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        @DisplayName("Should check if Order exists by ID")
        void shouldCheckExistsById() {
            when(repository.existsById(1L)).thenReturn(true);

            Result<Boolean, Exception> result = service.existsById(1L);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isTrue();
            verify(repository).existsById(1L);
        }

        @Test
        @DisplayName("Should count all Order")
        void shouldCountAll() {
            when(repository.count()).thenReturn(5L);

            Result<Long, Exception> result = service.count();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(5L);
            verify(repository).count();
        }

        @Test
        @DisplayName("Should count active Order")
        void shouldCountActive() {
            when(repository.count(any(Specification.class))).thenReturn(3L);

            Result<Long, Exception> result = service.countActive();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("Should save new Order successfully")
        void shouldSaveNewOrder() {
            Order newOrder = new Order();
            when(repository.save(any(Order.class))).thenReturn(order);

            Result<Order, Exception> result = service.save(newOrder);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should update existing Order")
        void shouldUpdateExistingOrder() {
            when(repository.findById(1L)).thenReturn(Optional.of(order));
            when(repository.save(any(Order.class))).thenReturn(order);

            Result<Order, Exception> result = service.update(1L, order);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Order.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should partial update Order")
        void shouldPartialUpdateOrder() {
            when(repository.findById(1L)).thenReturn(Optional.of(order));
            when(repository.save(any(Order.class))).thenReturn(order);

            Result<Order, Exception> result = service.partialUpdate(1L, order);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Order.class));
        }

        @Test
        @DisplayName("Should save all Order in batch")
        void shouldSaveAllInBatch() {
            List<Order> entities = List.of(order, new Order());
            when(repository.saveAll(anyList())).thenReturn(entities);

            Result<List<Order>, Exception> result = service.saveAll(entities);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should soft delete Order")
        void shouldSoftDeleteOrder() {
            when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testUser"));
            when(repository.findById(1L)).thenReturn(Optional.of(order));
            when(repository.save(any(Order.class))).thenReturn(order);

            Result<Void, Exception> result = service.softDelete(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).findById(1L);
            verify(repository).save(any(Order.class));
            verify(cacheEvictionService).evictListsByEntityName(anyString());
        }

        @Test
        @DisplayName("Should hard delete Order")
        void shouldHardDeleteOrder() {
            // Use lenient() to avoid strict stubbing issues with Mockito 5
            when(repository.hardDeleteById(1L)).thenReturn(1);
            lenient().doNothing().when(eventPublisher).publishEvent(any());
            lenient().doNothing().when(cacheEvictionService).evictListsByEntityName(anyString());
            lenient().doNothing().when(cacheEvictionService).evictCounts(anyString());

            Result<Void, Exception> result = service.hardDelete(1L);

            assertThat(result.isSuccess()).isTrue();
            verify(repository).hardDeleteById(1L);
        }

        @Test
        @DisplayName("Should soft delete all Order in batch")
        void shouldSoftDeleteAllInBatch() {
            when(auditorAware.getCurrentAuditor()).thenReturn(Optional.of("testUser"));
            when(repository.softDeleteAllByIds(anyList(), any(), anyString())).thenReturn(2);

            Result<Integer, Exception> result = service.softDeleteAll(List.of(1L, 2L));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Restore Operations")
    class RestoreOperations {

        @Test
        @DisplayName("Should restore all Order in batch")
        void shouldRestoreAllInBatch() {
            when(repository.restoreAllByIds(anyList())).thenReturn(2);

            Result<Integer, Exception> result = service.restoreAll(List.of(1L, 2L));

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.orElseThrow()).isEqualTo(2);
        }
    }
}
