package com.nexo.backend.service;

import com.nexo.backend.dto.CustomerDto;
import com.nexo.backend.model.Customer;
import com.nexo.backend.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerDto toDto(Customer customer) {
        return new CustomerDto(customer.getId(), customer.getName(), customer.getContact());
    }

    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll()
                .stream().map(this::toDto).toList();
    }

    public CustomerDto createCustomer(CustomerDto customerDto) {
        Customer customer = new Customer(customerDto.name(), customerDto.contact());
        Customer saved = customerRepository.save(customer);
        return toDto(saved);
    }
}
