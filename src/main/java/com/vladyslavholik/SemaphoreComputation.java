package com.vladyslavholik;

import com.vladyslavholik.file.FileUtil;
import com.vladyslavholik.model.Matrix;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;


public class SemaphoreComputation {
    private final static Logger log = Logger.getLogger(SemaphoreComputation.class);
    private static volatile Matrix B;
    private static volatile Matrix MC;
    private static volatile Matrix D;
    private static volatile Matrix MM;
    private static volatile Matrix MZ;
    private static volatile Double a;

    public static void main(String[] args) throws InterruptedException {
        BasicConfigurator.configure();

        B = FileUtil.readMatrixFromFile("input", "B");
        MC = FileUtil.readMatrixFromFile("input", "MC");
        D = FileUtil.readMatrixFromFile("input", "D");
        MM = FileUtil.readMatrixFromFile("input", "MM");
        MZ = FileUtil.readMatrixFromFile("input", "MZ");
        a = FileUtil.readDoubleFromFile("input", "a");

        var BSemaphore = new Semaphore(1);
        var MCSemaphore = new Semaphore(1);
        var DSemaphore = new Semaphore(1);
        var MMSemaphore = new Semaphore(1);
        var MZSemaphore = new Semaphore(1);
        var aSemaphore = new Semaphore(1);

        log.info("Starting calculation of C and MF");

        var stopWatch = new StopWatch();
        stopWatch.start();

        var CThread = new Thread(() -> calculateC(BSemaphore, MCSemaphore, DSemaphore, MMSemaphore));
        var MFThread = new Thread(() -> calculateMF(BSemaphore, DSemaphore, MCSemaphore, MZSemaphore, MMSemaphore, aSemaphore));

        CThread.start();
        MFThread.start();

        CThread.join();
        MFThread.join();

        stopWatch.stop();
        log.info(String.format("Calculation of matrix C and MF completed, time taken: %s ms", stopWatch.getTime()));
    }

    // C = В * МС - D * MM
    public static void calculateC(Semaphore BSemaphore, Semaphore MCSemaphore, Semaphore DSemaphore, Semaphore MMSemaphore) {
        try {
            log.info("Starting calculation of C");
            var stopWatch = new StopWatch();
            stopWatch.start();

            Supplier<Matrix> B_MCSupplier = () -> {
                try {
                    BSemaphore.acquire();
                    MCSemaphore.acquire();

                    var B_MC = B.multiplyRight("B_MC", MC);

                    BSemaphore.release();
                    MCSemaphore.release();

                    return B_MC;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };

            Supplier<Matrix> D_MMSupplier = () -> {
                try {
                    DSemaphore.acquire();
                    MMSemaphore.acquire();

                    var D_MM = D.multiplyRight("D_MM", MM);

                    DSemaphore.release();
                    MMSemaphore.release();

                    return D_MM;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
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
    public static void calculateMF(Semaphore BSemaphore, Semaphore DSemaphore, Semaphore MCSemaphore, Semaphore MZSemaphore, Semaphore MMSemaphore, Semaphore aSemaphore) {
        try {
            log.info("Starting calculation of MF");

            var stopWatch = new StopWatch();
            stopWatch.start();

            Supplier<Matrix> MC_MZSupplier = () -> {
                try {
                    MCSemaphore.acquire();
                    MZSemaphore.acquire();

                    var MC_MZ = MC.multiplyRight("MC_MZ", MZ);

                    MCSemaphore.release();
                    MZSemaphore.release();

                    return MC_MZ;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };

            Supplier<Matrix> B_DSupplier = () -> {
                try {
                    BSemaphore.acquire();
                    DSemaphore.acquire();

                    var B_D = B.add("B_D", D);

                    BSemaphore.release();
                    DSemaphore.release();

                    return B_D;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };

            var B_D_MC_MZFuture = CompletableFuture
                    .supplyAsync(MC_MZSupplier)
                    .thenCombine(CompletableFuture.supplyAsync(B_DSupplier), (MC_MZ, B_D) -> MC_MZ.multiply("B_D_MC_MZ", "min_BD", B_D.min()));

            Supplier<Matrix> MM_MC_MM_aSupplier = () -> {
                try {
                    MCSemaphore.acquire();
                    MMSemaphore.acquire();

                    var MC_MM = MC.add("MC_MM", MM);

                    MCSemaphore.release();

                    var MM_MC_MM = MM.multiplyRight("MM_MC_MM", MC_MM);

                    MMSemaphore.release();
                    aSemaphore.acquire();

                    var MM_MC_MM_a = MM_MC_MM.multiply("MM_MC_MM_a", "a", a);

                    aSemaphore.release();

                    return MM_MC_MM_a;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };

            CompletableFuture<Matrix> MFFuture = B_D_MC_MZFuture
                    .thenCombine(CompletableFuture.supplyAsync(MM_MC_MM_aSupplier), (B_D_MC_MZ, MM_MC_MM_a) -> B_D_MC_MZ.add("MF", MM_MC_MM_a));

            var MF = MFFuture.get();
            stopWatch.stop();

            log.info(String.format("Time taken to calculate matrix %s is %s ms", MF.getName(), stopWatch.getTime()));
            FileUtil.storeMatrix("output", MF);
        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }
}
