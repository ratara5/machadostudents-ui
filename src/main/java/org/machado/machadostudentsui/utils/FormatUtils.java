package org.machado.machadostudentsui.utils;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FormatUtils {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0");

    public static String formatNumber(int data) {
        return DECIMAL_FORMAT.format(data);
    }

    public static int parseNumber(String str) {
        try {
            return DECIMAL_FORMAT.parse(str).intValue();
        } catch (Exception e) {
            throw new NumberFormatException(String.format("Invalid number format %s", str));
        }
    }

    public static String getElementStringList(List<String> list, int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Error: IndexOutOfBoundsException.");
            return "";
        }
    }

    public static Integer getElementIntegerList(List<Integer> list, int index) {
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Error: IndexOutOfBoundsException.");
            return null;
        }
    }

    public static <T> List<T> joinLists(List<T> list1, List<T> list2) {
        Set<T> setJoin = new HashSet<>(list1);
        setJoin.addAll(list2);

        return new ArrayList<>(setJoin);
    }

    public static <T> List<T> intersectLists(List<T> list1, List<T> list2) {
        Set<T> set1 = new HashSet<>(list1);
        Set<T> set2 = new HashSet<>(list2);

        // Calcular la intersección
        set1.retainAll(set2);

        return new ArrayList<>(set1);
    }

    public static <T> List<T> differenceLists(List<T> list1, List<T> list2) {
        List<T> differenceList = new ArrayList<>(list1);
        differenceList.removeAll(list2);
        return differenceList;
    }

}