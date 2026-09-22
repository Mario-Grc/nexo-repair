package com.nexo.backend.service;

import com.nexo.backend.dto.CreateCustomerDto;
import com.nexo.backend.dto.CustomerDto;
import com.nexo.backend.exception.InvalidCustomerDataException;
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

    public CustomerDto createCustomer(CreateCustomerDto customerDto) {
        boolean hasEmail = customerDto.email() != null && !customerDto.email().isBlank();
        boolean hasPhone = customerDto.phone() != null && !customerDto.phone().isBlank();
        if (!hasEmail && !hasPhone) {
            throw new InvalidCustomerDataException("A customer needs at least an email or phone contact");
        }

        Customer customer = new Customer(customerDto.name(), customerDto.email(), customerDto.phone(), customerDto.notes());
        Customer saved = customerRepository.save(customer);
        return toDto(saved);
    }
}
