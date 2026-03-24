package com.group5.ems.dto.request.validation;

import com.group5.ems.dto.request.PeriodCreateReq;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that the start date is strictly before the end date
 * on a PeriodCreateReq record.
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, PeriodCreateReq> {

    @Override
    public boolean isValid(PeriodCreateReq req, ConstraintValidatorContext context) {
        // If either date is null, let @NotNull handle it
        if (req == null || req.startDate() == null || req.endDate() == null) {
            return true;
        }
        return req.startDate().isBefore(req.endDate());
    }
}
