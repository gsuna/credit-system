package com.loan.mappers;

import com.loan.dto.LoanInstallmentDto;
import com.loan.entity.LoanInstallment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanInstallmentMapper extends BaseMapper<LoanInstallment, LoanInstallmentDto> {

}
