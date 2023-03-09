package com.vladyslavholik.file;

import com.vladyslavholik.model.Matrix;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class FileUtil {

    public static void writeToFile(String directory, String filename, String content) {
        try {
            var directoryFile = new File(directory);
            var myWriter = new FileWriter(new File(directoryFile, filename));
            myWriter.write(content);
            myWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void storeDouble(String directory, String valueName, double value) {
        writeToFile(directory, valueName, Double.toString(value));
    }

    public static void storeMatrix(String directory, Matrix matrix) {
        var rowsAsStrings = matrix.getRows()
                .stream()
                .map(row -> row.stream().map(Object::toString).collect(Collectors.toList()))
                .map(row -> String.join(";", row))
                .collect(Collectors.toList());

        var stringRepresentation = String.join("\n", rowsAsStrings);
        writeToFile(directory, matrix.getName(), stringRepresentation);
    }

    public static Matrix readMatrixFromFile(String directory, String filename) {
        var directoryFile = new File(directory);
        try {
            var matrixAsString = FileUtils.readFileToString(new File(directoryFile, filename), "UTF-8");
            var matrix = Arrays.stream(matrixAsString.split("\n"))
                    .map(row -> Arrays.stream(row.split(";")).map(Double::valueOf).collect(Collectors.toList()))
                    .collect(Collectors.toList());
            return new Matrix(filename, matrix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Double readDoubleFromFile(String directory, String filename) {
        var directoryFile = new File(directory);
        try {
            var doubleAsString = FileUtils.readFileToString(new File(directoryFile, filename), "UTF-8");
            return Double.valueOf(doubleAsString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
