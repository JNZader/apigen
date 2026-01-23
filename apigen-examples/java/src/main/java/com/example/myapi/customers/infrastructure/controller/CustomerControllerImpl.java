package com.example.myapi.customers.infrastructure.controller;

import com.jnzader.apigen.core.infrastructure.controller.BaseControllerImpl;
import com.example.myapi.customers.application.dto.CustomerDTO;
import com.example.myapi.customers.application.mapper.CustomerMapper;
import com.example.myapi.customers.application.service.CustomerService;
import com.example.myapi.customers.domain.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class CustomerControllerImpl
        extends BaseControllerImpl<Customer, CustomerDTO, Long>
        implements CustomerController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerControllerImpl(CustomerService service, CustomerMapper mapper) {
        super(service, mapper);
        this.customerService = service;
        this.customerMapper = mapper;
    }
}
