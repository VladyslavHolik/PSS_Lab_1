package com.vladyslavholik.model;

import com.vladyslavholik.kahan.KahanUtil;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Matrix {
    private final static Logger log = Logger.getLogger(Matrix.class);

    private String name;
    private List<List<Double>> matrix;

    public Matrix(String name, Integer rowCount, Integer columnCount, Double initialValue) {
        this.name = name;
        this.matrix = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) {
            List<Double> matrixRow = new ArrayList<>(columnCount);

            for (int column = 0; column < columnCount; column++) {
                matrixRow.add(initialValue);
            }

            matrix.add(matrixRow);
        }
    }

    public Matrix(String name, List<List<Double>> matrix) {
        int columnCount = matrix.get(0).size();
        for (List<Double> row : matrix) {
            if (row.size() != columnCount) {
                var message = String.format("Invalid matrix: expected column count %s, but was %s", columnCount, row.size());
                throw new RuntimeException(message);
            }
        }
        this.matrix = matrix;
        this.name = name;
    }

    public List<List<Double>> getRows() {
        return matrix;
    }

    public Double get(Integer row, Integer column) {
        return matrix.get(row - 1).get(column - 1);
    }

    public void set(Double value, Integer row, Integer column) {
        matrix.get(row - 1).set(column - 1, value);
    }

    public int getRowCount() {
        return matrix.size();
    }

    public int getColumnCount() {
        return matrix.get(0).size();
    }

    public Matrix copy() {
        return new Matrix(name, matrix.stream().map(ArrayList::new).collect(Collectors.toList()));
    }

    public Matrix multiplyRight(String name, Matrix rightMatrix) {
        if (getColumnCount() != rightMatrix.getRowCount()) {
            throw new RuntimeException("Matrix could not be multiplied because of invalid size");
        }

        log.info(String.format("Starting multiplication: %s * %s", name, rightMatrix.name));
        var resultMatrix = copy();
        resultMatrix.name = name;

        for (int i = 1; i <= getRowCount(); i++) {
            for (int j = 1; j <= rightMatrix.getColumnCount(); j++) {
                List<Double> additions = new ArrayList<>();
                for (int k = 1; k <= rightMatrix.getRowCount(); k++) {
                    var a = get(i, k);
                    var b = rightMatrix.get(k, j);
                    additions.add(a * b);
                }

                var c = KahanUtil.sum(additions);
                resultMatrix.set(c, i, j);
            }
        }

        log.info(String.format("Multiplication %s * %s completed", name, rightMatrix.name));
        return resultMatrix;
    }

    public Matrix add(String name, Matrix rightMatrix) {
        if (getColumnCount() != rightMatrix.getColumnCount() || getRowCount() != rightMatrix.getRowCount()) {
            throw new RuntimeException("Matrix could not be added because of invalid size");
        }

        log.info(String.format("Starting addition: %s + %s", name, rightMatrix.name));
        var resultMatrix = copy();
        resultMatrix.name = name;

        for (int i = 1; i <= getRowCount(); i++) {
            for (int j = 1; j <= getColumnCount(); j++) {
                var a = get(i, j);
                var b = rightMatrix.get(i, j);

                var c = KahanUtil.sum(List.of(a, b));
                resultMatrix.set(c, i, j);
            }
        }

        log.info(String.format("Addition %s + %s completed", name, rightMatrix.name));
        return resultMatrix;
    }

    public Matrix subtract(String name, Matrix rightMatrix) {
        if (getColumnCount() != rightMatrix.getColumnCount() || getRowCount() != rightMatrix.getRowCount()) {
            throw new RuntimeException("Matrix could not be subtracted because of invalid size");
        }

        log.info(String.format("Starting subtraction: %s - %s", name, rightMatrix.name));
        var resultMatrix = copy();
        resultMatrix.name = name;
        for (int i = 1; i <= getRowCount(); i++) {
            for (int j = 1; j <= getColumnCount(); j++) {
                var a = get(i, j);
                var b = rightMatrix.get(i, j);

                var c = KahanUtil.sum(List.of(a, -b));
                resultMatrix.set(c, i, j);
            }
        }

        log.info(String.format("Subtraction %s - %s completed", name, rightMatrix.name));
        return resultMatrix;
    }

    public String getName() {
        return name;
    }

    public Double min() {
        log.info(String.format("Starting finding min of matrix %s ", name));
        var result = matrix.stream()
                .flatMap(Collection::stream)
                .min(Double::compareTo)
                .orElseThrow();

        log.info(String.format("Finding min of matrix %s completed", name));
        return result;
    }

    public Matrix multiply(String name, String valueName, Double value) {
        log.info(String.format("Starting multiplication of matrix %s on value %s", name, valueName));
        var resultMatrix = copy();
        resultMatrix.name = name;

        for (int i = 1; i <= getRowCount(); i++) {
            for (int j = 1; j <= getColumnCount(); j++) {
                resultMatrix.set(get(i, j) * value, i, j);
            }
        }

        log.info(String.format("Multiplication of matrix %s on value %s completed", name, valueName));
        return resultMatrix;
    }
}
