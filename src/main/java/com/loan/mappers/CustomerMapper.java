package com.loan.mappers;

import com.loan.dto.CustomerDto;
import com.loan.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper extends BaseMapper<Customer, CustomerDto> {

}
