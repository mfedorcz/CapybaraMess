package com.example.capybaramess;

public class AppConfig {
    private static String phoneNumber;

    public static String getPhoneNumber() {
        return phoneNumber;
    }

    public static void setPhoneNumber(String number) {
        phoneNumber = number;
    }

    public static String getConversationId(String recipientPhoneNumber) {
        // Ensure that the conversation ID is always in the format "smallerNumber_largerNumber"
        return phoneNumber.compareTo(recipientPhoneNumber) < 0
                ? phoneNumber + "_" + recipientPhoneNumber
                : recipientPhoneNumber + "_" + phoneNumber;
    }

    public static String checkAndAddCCToNumber(String numberToFormat){
        return  numberToFormat.length() == 9 && containsOnlyDigits(numberToFormat) ? "+48" + numberToFormat : numberToFormat;
    }
    private static boolean containsOnlyDigits(String str) {
        return str.matches("\\d+");
    }
}
