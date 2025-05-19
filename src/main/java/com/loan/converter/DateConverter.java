package com.loan.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.text.SimpleDateFormat;
import java.util.Date;

@Converter(autoApply = true)
public class DateConverter implements AttributeConverter<Date, String> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

    @Override
    public String convertToDatabaseColumn(Date date) {
        return date != null ? DATE_FORMAT.format(date) : null;
    }

    @Override
    public Date convertToEntityAttribute(String dbData) {
        try {
            return dbData != null ? DATE_FORMAT.parse(dbData) : null;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy.MM.dd", e);
        }
    }
}
