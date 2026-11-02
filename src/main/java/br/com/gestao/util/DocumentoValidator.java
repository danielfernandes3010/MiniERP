package br.com.gestao.util;

public final class DocumentoValidator {
    private DocumentoValidator() {}

    public static String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    public static boolean valid(String value) {
        String digits = normalize(value);
        if (digits.length() == 11) return validCpf(digits);
        if (digits.length() == 14) return validCnpj(digits);
        return false;
    }

    private static boolean validCpf(String cpf) {
        if (cpf.matches("(\\d)\\1{10}")) return false;
        int first = digit(cpf.substring(0, 9), 10);
        int second = digit(cpf.substring(0, 9) + first, 11);
        return cpf.endsWith("" + first + second);
    }

    private static int digit(String source, int weight) {
        int sum = 0;
        for (char c : source.toCharArray()) sum += (c - '0') * weight--;
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean validCnpj(String cnpj) {
        if (cnpj.matches("(\\d)\\1{13}")) return false;
        int first = cnpjDigit(cnpj.substring(0, 12), new int[]{5,4,3,2,9,8,7,6,5,4,3,2});
        int second = cnpjDigit(cnpj.substring(0, 12) + first, new int[]{6,5,4,3,2,9,8,7,6,5,4,3,2});
        return cnpj.endsWith("" + first + second);
    }

    private static int cnpjDigit(String source, int[] weights) {
        int sum = 0;
        for (int i = 0; i < weights.length; i++) sum += (source.charAt(i) - '0') * weights[i];
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
