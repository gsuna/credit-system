package com.loan.service.impl;

import com.loan.entity.User;
import com.loan.enums.RoleType;
import com.loan.exception.BusinessException;
import com.loan.service.SecurityAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityAccessServiceImpl implements SecurityAccessService {

    public boolean isAdminOrOwner(Long customerId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;

        // is role ADMIN
        if (authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(RoleType.ADMIN.name()))) {
            return true;
        }

        // is role CUSTOMER
        User principal = (User) authentication.getPrincipal();
        if (principal.getCustomer() != null &&
                !principal.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Access denied");
        }

        return true;
    }
}
