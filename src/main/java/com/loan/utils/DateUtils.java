package com.loan.utils;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@UtilityClass
public class DateUtils {
    private static final SimpleDateFormat DATE_CONVERTER_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

    public Date getFirstDayOfNextMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return calendar.getTime();
    }

    public Date addMonthsToDate(Date date, int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, months);
        return calendar.getTime();
    }

    public String formatDate(Date date) {
        return DATE_CONVERTER_FORMAT.format(date);
    }

    public Date convertDate(String date) {
        try {
            return DATE_CONVERTER_FORMAT.parse(date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format. Expected yyyy.MM.dd", e);
        }
    }

    public long daysBetween(Date startDate, Date endDate) {
        long diffInMillis = endDate.getTime() - startDate.getTime();
        return diffInMillis / (1000 * 60 * 60 * 24);
    }
}
