package com.vladyslavholik.kahan;

import java.util.List;

public class KahanUtil {
    public static double sum(List<Double> args) {
        double sum = 0.0;
        double c = 0.0;
        for (double arg : args) {
            double y = arg - c;
            double t = sum + y;
            c = (t - sum) - y;
            sum = t;
        }

        return sum;
    }
}
