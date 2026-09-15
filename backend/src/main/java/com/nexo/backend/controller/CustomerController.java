package com.nexo.backend.controller;

import com.nexo.backend.dto.CustomerDto;
import com.nexo.backend.service.CustomerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerDto> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @PostMapping
    public CustomerDto createCustomer(@RequestBody CustomerDto customerDto) {
        return customerService.createCustomer(customerDto);
    }
}
