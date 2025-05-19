package com.loan.service;

import com.loan.dto.CustomerDto;

import java.util.List;

public interface CustomerService {
    List<CustomerDto> getAllCustomers();
    CustomerDto findById(Long id);
}
