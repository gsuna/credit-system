package com.loan.mappers;

import com.loan.dto.LoanDto;
import com.loan.entity.Loan;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanMapper extends BaseMapper<Loan, LoanDto> {

}
