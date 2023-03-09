package com.vladyslavholik;

import com.vladyslavholik.file.FileUtil;
import com.vladyslavholik.model.Matrix;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;


public class StackConfinementComputation {
    private final static Logger log = Logger.getLogger(StackConfinementComputation.class);

    public static void main(String[] args) throws InterruptedException {
        BasicConfigurator.configure();

        var B = FileUtil.readMatrixFromFile("input", "B");
        var MC = FileUtil.readMatrixFromFile("input", "MC");
        var D = FileUtil.readMatrixFromFile("input", "D");
        var MM = FileUtil.readMatrixFromFile("input", "MM");
        var MZ = FileUtil.readMatrixFromFile("input", "MZ");
        var a = FileUtil.readDoubleFromFile("input", "a");

        log.info("Starting calculation of C and MF");

        var stopWatch = new StopWatch();
        stopWatch.start();

        var CThread = new Thread(() -> calculateC(B, MC, D, MM));
        var MFThread = new Thread(() -> calculateMF(B, D, MC, MZ, MM, a));

        CThread.start();
        MFThread.start();

        CThread.join();
        MFThread.join();

        stopWatch.stop();
        log.info(String.format("Calculation of matrix C and MF completed, time taken: %s ms", stopWatch.getTime()));
    }

    // C = В * МС - D * MM
    public static void calculateC(Matrix externalB, Matrix externalMC, Matrix externalD, Matrix externalMM) {
        try {
            log.info("Starting calculation of C");

            var stopWatch = new StopWatch();
            stopWatch.start();
            var B = externalB.copy();
            var MC = externalMC.copy();
            var D = externalD.copy();
            var MM = externalMM.copy();

            Supplier<Matrix> B_MCSupplier = () -> {
                var localB = B.copy();
                var localMC = MC.copy();

                return localB.multiplyRight("B_MC", localMC);
            };

            Supplier<Matrix> D_MMSupplier = () -> {
                var localD = D.copy();
                var localMM = MM.copy();

                return localD.multiplyRight("D_MM", localMM);
            };

            CompletableFuture<Matrix> CFuture = CompletableFuture.supplyAsync(B_MCSupplier)
                    .thenCombine(CompletableFuture.supplyAsync(D_MMSupplier), (B_MC, D_MM) -> B_MC.subtract("C", D_MM));

            var C = CFuture.get();
            stopWatch.stop();
            log.info(String.format("Time taken to calculate matrix %s is %s ms", C.getName(), stopWatch.getTime()));
            FileUtil.storeMatrix("output", C);

        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }

    // MF = min(B + D) * MC * MZ + MM * (MC + MM) * a
    public static void calculateMF(Matrix externalB, Matrix externalD, Matrix externalMC, Matrix externalMZ, Matrix externalMM, Double a) {
        try {
            log.info("Starting calculation of MF");

            var stopWatch = new StopWatch();
            stopWatch.start();
            var B = externalB.copy();
            var D = externalD.copy();
            var MC = externalMC.copy();
            var MZ = externalMZ.copy();
            var MM = externalMM.copy();

            Supplier<Matrix> MC_MZSupplier = () -> {
                var localMC = MC.copy();
                var localMZ = MZ.copy();

                return localMC.multiplyRight("MC_MZ", localMZ);
            };

            Supplier<Matrix> B_DSupplier = () -> {
                var localB = B.copy();
                var localD = D.copy();

                return localB.add("B_D", localD);
            };

            var B_D_MC_MZFuture = CompletableFuture
                    .supplyAsync(MC_MZSupplier)
                    .thenCombine(CompletableFuture.supplyAsync(B_DSupplier), (MC_MZ, B_D) -> MC_MZ.multiply("B_D_MC_MZ", "min_BD", B_D.min()));

            Supplier<Matrix> MM_MC_MMSupplier = () -> {
                var localMC = MC.copy();
                var localMM = MM.copy();

                return localMM.multiplyRight("MM_MC_MM", localMC.add("MC_MM", localMM));
            };

            var MM_MC_MM_aFuture = CompletableFuture
                    .supplyAsync(MM_MC_MMSupplier)
                    .thenCombine(CompletableFuture.supplyAsync(() -> a), (MM_MC_MM, localA) -> MM_MC_MM.multiply("MM_MC_MM_a", "a", localA));

            CompletableFuture<Matrix> MFFuture = B_D_MC_MZFuture
                    .thenCombine(MM_MC_MM_aFuture, (B_D_MC_MZ, MM_MC_MM_a) -> B_D_MC_MZ.add("MF", MM_MC_MM_a));
            var MF = MFFuture.get();
            stopWatch.stop();

            log.info(String.format("Time taken to calculate matrix %s is %s ms", MF.getName(), stopWatch.getTime()));
            FileUtil.storeMatrix("output", MF);
        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }
}
