package com.loan.controller;

import com.loan.dto.CustomerDto;
import com.loan.dto.response.GenericResponse;
import com.loan.dto.response.PaymentResponse;
import com.loan.service.CustomerService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "api")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping(path = "/customer/list")
    public ResponseEntity<GenericResponse<List<CustomerDto>>> getAllCustomer() {
        List<CustomerDto> customerList = customerService.getAllCustomers();
        return ResponseEntity.ok(GenericResponse.<List<CustomerDto>>builder()
                .data(customerList)
                .build());
    }
}
