package com.vladyslavholik.generator;

import com.vladyslavholik.file.FileUtil;
import com.vladyslavholik.model.Matrix;

import java.util.Random;

public class Generator {
    private static final Random random = new Random();

    public double generateRandomDoubleValue() {
        return Integer.MAX_VALUE * random.nextDouble();
    }

    public Matrix generateRandomDoubleMatrix(String matrixName, Integer rowQuantity, Integer columnQuantity) {
        var matrix = new Matrix(matrixName, rowQuantity, columnQuantity, 0d);
        for (int row = 1; row <= rowQuantity; row++) {
            for (int column = 1; column <= columnQuantity; column++) {
                matrix.set(generateRandomDoubleValue(), row, column);
            }
        }
        return matrix;
    }

    public static void main(String[] args) {
        var generator = new Generator();

        var defaultSize = args.length == 0 ? 100 : Integer.parseInt(args[0]);

        var MC = generator.generateRandomDoubleMatrix("MC", defaultSize, defaultSize);
        FileUtil.storeMatrix("input", MC);

        var B = generator.generateRandomDoubleMatrix("B", 1, defaultSize);
        FileUtil.storeMatrix("input", B);

        var D = generator.generateRandomDoubleMatrix("D", 1, defaultSize);
        FileUtil.storeMatrix("input", D);

        var MM = generator.generateRandomDoubleMatrix("MM", defaultSize, defaultSize);
        FileUtil.storeMatrix("input", MM);

        var MZ = generator.generateRandomDoubleMatrix("MZ", defaultSize, defaultSize);
        FileUtil.storeMatrix("input", MZ);

        var a = generator.generateRandomDoubleValue();
        FileUtil.storeDouble("input", "a", a);
    }
}
