package com.nexo.backend.service;

import com.nexo.backend.dto.CustomerRequestDto;
import com.nexo.backend.dto.CustomerDto;
import com.nexo.backend.exception.ResourceNotFoundException;
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
        return new CustomerDto(customer.getId(), customer.getName(), customer.getEmail(), customer.getPhone(), customer.getNotes());
    }

    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll()
                .stream().map(this::toDto).toList();
    }

    public CustomerDto getCustomer(Long id) {
        return customerRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + id + " not found"));
    }

    public CustomerDto createCustomer(CustomerRequestDto customerDto) {
        Customer customer = new Customer(
                customerDto.name().trim(),
                blankToNull(customerDto.email()),
                blankToNull(customerDto.phone()),
                blankToNull(customerDto.notes()));
        Customer saved = customerRepository.save(customer);
        return toDto(saved);
    }

    public CustomerDto updateCustomer(Long id, CustomerRequestDto customerDto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer " + id + " not found"));
        customer.setName(customerDto.name().trim());
        customer.setEmail(blankToNull(customerDto.email()));
        customer.setPhone(blankToNull(customerDto.phone()));
        customer.setNotes(blankToNull(customerDto.notes()));
        return toDto(customerRepository.save(customer));
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
