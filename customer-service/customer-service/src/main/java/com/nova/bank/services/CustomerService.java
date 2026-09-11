package com.nova.bank.services;
import com.nova.bank.clients.KycClient;
import com.nova.bank.clients.KycStatusResponse;
import com.nova.bank.dto.*;
import com.nova.bank.entities.Customer;
import com.nova.bank.entities.CustomerStatus;
import com.nova.bank.exception.CustomerAlreadyExistsException;
import com.nova.bank.exception.CustomerNotFoundException;
import com.nova.bank.exception.InvalidCustomerStatusTransitionException;
import com.nova.bank.exception.KycNotFoundException;
import com.nova.bank.repositories.CustomerRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KycClient kycClient;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        // 1. Check duplicate email
        if (request.email() != null && customerRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Customer with email already exists");
        }
        // 2. Check duplicate mobile number
        if (request.mobileNumber() != null && customerRepository.existsByMobileNumber(request.mobileNumber())) {
            throw new IllegalStateException("Customer with mobile number already exists");
        }
        // 3. Create Customer entity
        Customer customer = Customer.builder()
                .customerId(generateCustomerId())
                .title(request.title())
                .firstName(request.firstName())
                .middleName(request.middleName())
                .lastName(request.lastName())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .nationality(request.nationality())
                .maritalStatus(request.maritalStatus())
                .email(request.email())
                .mobileNumber(request.mobileNumber())
                .alternateMobileNumber(request.alternateMobileNumber())
                .permanentAddress(request.permanentAddress())
                .permanentCity(request.permanentCity())
                .permanentState(request.permanentState())
                .permanentCountry(request.permanentCountry())
                .permanentPinCode(request.permanentPinCode())
                .communicationAddress(request.communicationAddress())
                .communicationCity(request.communicationCity())
                .communicationState(request.communicationState())
                .communicationCountry(request.communicationCountry())
                .communicationPinCode(request.communicationPinCode())
                .employmentType(request.employmentType())
                .occupation(request.occupation())
                .employerName(request.employerName())
                .annualIncome(request.annualIncome())
                .sourceOfIncome(request.sourceOfIncome())
                .language(request.language())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return mapToResponse(savedCustomer);
    }

    private String generateCustomerId() {
        return "CUST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private CustomerResponse mapToResponse(Customer customer) {

        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getTitle(),
                customer.getFirstName(),
                customer.getMiddleName(),
                customer.getLastName(),
                customer.getDateOfBirth(),
                customer.getGender(),
                customer.getNationality(),
                customer.getMaritalStatus(),
                customer.getEmail(),
                customer.getMobileNumber(),
                customer.getAlternateMobileNumber(),
                customer.getPermanentAddress(),
                customer.getPermanentCity(),
                customer.getPermanentState(),
                customer.getPermanentCountry(),
                customer.getPermanentPinCode(),
                customer.getCommunicationAddress(),
                customer.getCommunicationCity(),
                customer.getCommunicationState(),
                customer.getCommunicationCountry(),
                customer.getCommunicationPinCode(),
                customer.getEmploymentType(),
                customer.getOccupation(),
                customer.getEmployerName(),
                customer.getAnnualIncome(),
                customer.getSourceOfIncome(),
                customer.getLanguage(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getAllCustomers(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }


        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {

            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'");
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

        PageRequest pageRequest = PageRequest.of(page, size, sort);

        Page<Customer> customerPage = customerRepository.findAll(pageRequest);

        Page<CustomerResponse> responsePage = customerPage.map(this::mapToResponse);

        return PageResponse.fromPage(responsePage);
    }

    // update customer
    @Transactional
    public CustomerResponse updateCustomer(String customerId, UpdateCustomerRequest request) {

        // Find existing customer
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        // Check duplicate email
        if (request.email() != null && !request.email().equalsIgnoreCase(customer.getEmail()) && customerRepository.existsByEmail(request.email())) {
            throw new CustomerAlreadyExistsException("Customer with email already exists");
        }
        // Check duplicate mobile number
        if (request.mobileNumber() != null && !request.mobileNumber().equals(customer.getMobileNumber()) && customerRepository.existsByMobileNumber(request.mobileNumber())) {

            throw new CustomerAlreadyExistsException("Customer with mobile number already exists");
        }
        // Update only fields supplied by the client
        if (request.title() != null) {
            customer.setTitle(request.title());
        }

        if (request.firstName() != null) {
            customer.setFirstName(request.firstName());
        }

        if (request.middleName() != null) {
            customer.setMiddleName(request.middleName());
        }

        if (request.lastName() != null) {
            customer.setLastName(request.lastName());
        }

        if (request.nationality() != null) {
            customer.setNationality(request.nationality());
        }

        if (request.maritalStatus() != null) {
            customer.setMaritalStatus(request.maritalStatus());
        }

        if (request.email() != null) {
            customer.setEmail(request.email());
        }

        if (request.mobileNumber() != null) {
            customer.setMobileNumber(request.mobileNumber());
        }

        if (request.alternateMobileNumber() != null) {
            customer.setAlternateMobileNumber(
                    request.alternateMobileNumber()
            );
        }

        if (request.permanentAddress() != null) {
            customer.setPermanentAddress(request.permanentAddress());
        }

        if (request.permanentCity() != null) {
            customer.setPermanentCity(request.permanentCity());
        }

        if (request.permanentState() != null) {
            customer.setPermanentState(request.permanentState());
        }

        if (request.permanentCountry() != null) {
            customer.setPermanentCountry(request.permanentCountry());
        }

        if (request.permanentPinCode() != null) {
            customer.setPermanentPinCode(request.permanentPinCode());
        }

        if (request.communicationAddress() != null) {
            customer.setCommunicationAddress(request.communicationAddress());
        }

        if (request.communicationCity() != null) {
            customer.setCommunicationCity(request.communicationCity());
        }

        if (request.communicationState() != null) {
            customer.setCommunicationState(request.communicationState());
        }

        if (request.communicationCountry() != null) {
            customer.setCommunicationCountry(
                    request.communicationCountry()
            );
        }

        if (request.communicationPinCode() != null) {
            customer.setCommunicationPinCode(
                    request.communicationPinCode()
            );
        }

        if (request.employmentType() != null) {
            customer.setEmploymentType(request.employmentType());
        }

        if (request.occupation() != null) {
            customer.setOccupation(request.occupation());
        }

        if (request.employerName() != null) {
            customer.setEmployerName(request.employerName());
        }

        if (request.annualIncome() != null) {
            customer.setAnnualIncome(request.annualIncome());
        }

        if (request.sourceOfIncome() != null) {
            customer.setSourceOfIncome(request.sourceOfIncome());
        }

        if (request.language() != null) {
            customer.setLanguage(request.language());
        }

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }

    private void validateStatusTransition(CustomerStatus currentStatus, CustomerStatus newStatus) {

        switch (currentStatus) {
            case PENDING -> {
                if (newStatus != CustomerStatus.ACTIVE) {
                    throw new IllegalStateException("PENDING customer can only become ACTIVE");
                }
            }

            case ACTIVE -> {
                if (newStatus != CustomerStatus.INACTIVE && newStatus != CustomerStatus.BLOCKED && newStatus != CustomerStatus.CLOSED) {

                    throw new IllegalStateException("ACTIVE customer can only become INACTIVE, BLOCKED or CLOSED");
                }
            }

            case INACTIVE -> {
                if (newStatus != CustomerStatus.ACTIVE && newStatus != CustomerStatus.CLOSED) {

                    throw new IllegalStateException("INACTIVE customer can only become ACTIVE or CLOSED");
                }
            }

            case BLOCKED -> {
                if (newStatus != CustomerStatus.ACTIVE && newStatus != CustomerStatus.CLOSED) {

                    throw new IllegalStateException("BLOCKED customer can only become ACTIVE or CLOSED");
                }
            }

            case CLOSED -> {
                throw new IllegalStateException("CLOSED customer cannot change status");
            }
        }
    }

    @Transactional
    public CustomerResponse updateCustomerStatus(String customerId, UpdateCustomerStatusRequest request) {

        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        CustomerStatus currentStatus = customer.getStatus();
        CustomerStatus newStatus = request.getStatus();

        if (newStatus == CustomerStatus.ACTIVE) {

            try {

                KycStatusResponse kycStatus = kycClient.getKycStatus(customerId);

                if (!"APPROVED".equalsIgnoreCase(kycStatus.verificationStatus())) {

                    throw new InvalidCustomerStatusTransitionException("Customer cannot become ACTIVE because KYC is not APPROVED");
                }

            } catch (FeignException.NotFound ex) {

                throw new KycNotFoundException("KYC record not found for customer: " + customerId);
            }
        }

        validateStatusTransition(currentStatus, newStatus);

        customer.setStatus(newStatus);

        Customer updatedCustomer = customerRepository.save(customer);

        return mapToResponse(updatedCustomer);
    }

    public CustomerResponse getCustomerByCustomerId(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("customer not found: " + customerId));
        return mapToResponse(customer);
    }

    public CustomerResponse closeCustomer(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("customer not found:" + customerId));
        customer.setStatus(CustomerStatus.CLOSED);
        Customer save = customerRepository.save(customer);
        return mapToResponse(save);
    }

    public @Nullable CustomerResponse getCustomerByEmail(String email) {
        Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new CustomerNotFoundException("Customer not found:" + email));
        return mapToResponse(customer);
    }

    public @Nullable CustomerResponse getCustomerByMobile(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(() -> new CustomerNotFoundException("Customer not found:" + mobileNumber));
        return mapToResponse(customer);
    }

    public void statusUpdate(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId).orElseThrow(() -> new CustomerNotFoundException("Customer not found :" + customerId));
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);
    }
}