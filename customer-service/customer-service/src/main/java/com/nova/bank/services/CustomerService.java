package com.nova.bank.services;

import com.nova.bank.dto.CreateCustomerRequest;
import com.nova.bank.dto.CustomerResponse;
import com.nova.bank.dto.PageResponse;
import com.nova.bank.dto.UpdateCustomerRequest;
import com.nova.bank.entities.Customer;
import com.nova.bank.entities.CustomerStatus;
import com.nova.bank.exception.CustomerAlreadyExistsException;
import com.nova.bank.exception.CustomerNotFoundException;
import com.nova.bank.repositories.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;


@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {

        // Check duplicate email
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new CustomerAlreadyExistsException("Customer with email " + request.getEmail() + " already exists");
        }

        // Check duplicate mobile number
        if (customerRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new CustomerAlreadyExistsException("Customer with mobile number " + request.getMobileNumber() + " already exists");
        }

        Customer customer = new Customer();

        customer.setCustomerId(generateCustomerId());
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setMobileNumber(request.getMobileNumber());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setStatus(CustomerStatus.ACTIVE);

        Customer savedCustomer = customerRepository.save(customer);

        return mapToResponse(savedCustomer);
    }

    private String generateCustomerId() {

        return "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private CustomerResponse mapToResponse(Customer customer) {

        CustomerResponse response = new CustomerResponse();

        response.setCustomerId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setEmail(customer.getEmail());
        response.setMobileNumber(customer.getMobileNumber());
        response.setDateOfBirth(customer.getDateOfBirth());
        response.setStatus(customer.getStatus());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());
        return response;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCustomerId(String customerId) {

        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(int page, int size,String sortBy,String sortDir) {
        Sort sort=  sortDir.trim().toLowerCase().equals("asc")? Sort.by(sortBy).ascending():Sort.by(sortBy).descending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Customer> all = customerRepository.findAll(pageRequest);
        Page<CustomerResponse> customerResponses = all.map(customer -> mapToResponse(customer));

        return PageResponse.fromPage(customerResponses);
    }

    @Transactional
    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request) {

        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        if (!customer.getEmail().equalsIgnoreCase(request.getEmail()) && customerRepository.existsByEmail(request.getEmail())) {

            throw new CustomerAlreadyExistsException("Customer with email " + request.getEmail() + " already exists");
        }

        if (!customer.getMobileNumber().equals(request.getMobileNumber()) && customerRepository.existsByMobileNumber(request.getMobileNumber())) {

            throw new CustomerAlreadyExistsException("Customer with mobile number " + request.getMobileNumber() + " already exists");
        }

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setMobileNumber(request.getMobileNumber());
        customer.setDateOfBirth(request.getDateOfBirth());

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomerStatus(String customerId, CustomerStatus newStatus) {

        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        CustomerStatus currentStatus = customer.getStatus();

        if (currentStatus == CustomerStatus.CLOSED) {
            throw new IllegalStateException("Closed customer cannot change status");
        }

        if (currentStatus == newStatus) {
            return mapToResponse(customer);
        }

        customer.setStatus(newStatus);

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }

    @Transactional
    public CustomerResponse closeCustomer(String customerId) {

        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        if (customer.getStatus() == CustomerStatus.CLOSED) {
            throw new IllegalStateException("Customer is already closed");
        }

        if (customer.getStatus() == CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Blocked customer cannot be closed");
        }

        customer.setStatus(CustomerStatus.CLOSED);

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {

        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));

        return mapToResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByMobile(String mobileNumber) {

        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> new CustomerNotFoundException("Customer not found with mobile number: " + mobileNumber));

        return mapToResponse(customer);
    }
}
