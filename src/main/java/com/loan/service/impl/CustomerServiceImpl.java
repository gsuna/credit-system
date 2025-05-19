package com.loan.service.impl;

import com.loan.dto.CustomerDto;
import com.loan.exception.BusinessException;
import com.loan.mappers.CustomerMapper;
import com.loan.repository.CustomerRepository;
import com.loan.service.CustomerService;
import com.loan.service.cache.RedissonCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public List<CustomerDto> getAllCustomers() {
        return customerMapper.toDtoList(customerRepository.findAll());

    }

    @Override
    public CustomerDto findById(Long id) {
        return customerMapper.toDto(customerRepository.findById(id).
                orElseThrow(() -> new BusinessException("Customer not found")));
    }
}
