package com.nexo.backend.controller;

import com.nexo.backend.dto.CustomerRequestDto;
import com.nexo.backend.dto.CustomerDto;
import com.nexo.backend.service.CustomerService;
import jakarta.validation.Valid;
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

    @GetMapping("/{id}")
    public CustomerDto getCustomer(@PathVariable Long id) {
        return customerService.getCustomer(id);
    }

    @PostMapping
    public CustomerDto createCustomer(@Valid @RequestBody CustomerRequestDto customerDto) {
        return customerService.createCustomer(customerDto);
    }

    @PutMapping("/{id}")
    public CustomerDto updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerRequestDto customerDto) {
        return customerService.updateCustomer(id, customerDto);
    }
}
