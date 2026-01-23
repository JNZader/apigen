package com.example.myapi.customers.application.service;

import com.jnzader.apigen.core.application.service.BaseServiceImpl;
import com.jnzader.apigen.core.application.service.CacheEvictionService;
import com.example.myapi.customers.domain.entity.Customer;
import com.example.myapi.customers.infrastructure.repository.CustomerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional
public class CustomerServiceImpl
        extends BaseServiceImpl<Customer, Long>
        implements CustomerService {

    public CustomerServiceImpl(
            CustomerRepository repository,
            CacheEvictionService cacheEvictionService,
            ApplicationEventPublisher eventPublisher,
            AuditorAware<String> auditorAware) {
        super(repository, cacheEvictionService, eventPublisher, auditorAware);
    }

    @Override
    protected Class<Customer> getEntityClass() {
        return Customer.class;
    }
}
