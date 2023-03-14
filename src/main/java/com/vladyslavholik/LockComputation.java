package com.vladyslavholik;

import com.vladyslavholik.file.FileUtil;
import com.vladyslavholik.model.Matrix;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;


public class LockComputation {
    private final static Logger log = Logger.getLogger(LockComputation.class);
    private static Matrix B;
    private static Matrix MC;
    private static Matrix D;
    private static Matrix MM;
    private static Matrix MZ;
    private static Double a;

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        BasicConfigurator.configure();

        B = FileUtil.readMatrixFromFile("input", "B");
        MC = FileUtil.readMatrixFromFile("input", "MC");
        D = FileUtil.readMatrixFromFile("input", "D");
        MM = FileUtil.readMatrixFromFile("input", "MM");
        MZ = FileUtil.readMatrixFromFile("input", "MZ");
        a = FileUtil.readDoubleFromFile("input", "a");

        var BLock = new ReentrantReadWriteLock();
        var BReadLock = BLock.readLock();
        var MCLock = new ReentrantReadWriteLock();
        var MCReadLock = MCLock.readLock();
        var DLock = new ReentrantReadWriteLock();
        var DReadLock = DLock.readLock();
        var MMLock = new ReentrantReadWriteLock();
        var MMReadLock = MMLock.readLock();
        var MZLock = new ReentrantReadWriteLock();
        var MZReadLock = MZLock.readLock();
        var aLock = new ReentrantReadWriteLock();
        var aReadLock = aLock.readLock();

        log.info("Starting calculation of C and MF");

        var stopWatch = new StopWatch();
        stopWatch.start();

        var executorService = Executors.newFixedThreadPool(2);

        var CFuture = executorService.submit(() -> calculateC(BReadLock, MCReadLock, DReadLock, MMReadLock));
        var MFFuture = executorService.submit(() -> calculateMF(BReadLock, DReadLock, MCReadLock, MZReadLock, MMReadLock, aReadLock));

        CFuture.get();
        MFFuture.get();

        stopWatch.stop();
        log.info(String.format("Calculation of matrix C and MF completed, time taken: %s ms", stopWatch.getTime()));
        executorService.shutdown();
    }

    // C = В * МС - D * MM
    public static Matrix calculateC(ReentrantReadWriteLock.ReadLock BReadLock,
                                  ReentrantReadWriteLock.ReadLock MCReadLock,
                                  ReentrantReadWriteLock.ReadLock DReadLock,
                                  ReentrantReadWriteLock.ReadLock MMReadLock) {
        try {
            log.info("Starting calculation of C");
            var stopWatch = new StopWatch();
            stopWatch.start();

            Supplier<Matrix> B_MCSupplier = () -> {
                BReadLock.lock();
                MCReadLock.lock();

                var B_MC = B.multiplyRight("B_MC", MC);

                BReadLock.unlock();
                MCReadLock.unlock();

                return B_MC;
            };

            Supplier<Matrix> D_MMSupplier = () -> {
                DReadLock.lock();
                MMReadLock.lock();

                var D_MM = D.multiplyRight("D_MM", MM);

                DReadLock.unlock();
                MMReadLock.unlock();

                return D_MM;
            };

            CompletableFuture<Matrix> CFuture = CompletableFuture.supplyAsync(B_MCSupplier)
                    .thenCombine(CompletableFuture.supplyAsync(D_MMSupplier), (B_MC, D_MM) -> B_MC.subtract("C", D_MM));

            var C = CFuture.get();

            stopWatch.stop();

            log.info(String.format("Time taken to calculate matrix %s is %s ms", C.getName(), stopWatch.getTime()));
            FileUtil.storeMatrix("output", C);
            return C;
        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }

    // MF = min(B + D) * MC * MZ + MM * (MC + MM) * a
    public static Matrix calculateMF(ReentrantReadWriteLock.ReadLock BReadLock,
                                   ReentrantReadWriteLock.ReadLock DReadLock,
                                   ReentrantReadWriteLock.ReadLock MCReadLock,
                                   ReentrantReadWriteLock.ReadLock MZReadLock,
                                   ReentrantReadWriteLock.ReadLock MMReadLock,
                                   ReentrantReadWriteLock.ReadLock aReadLock) {
        try {
            log.info("Starting calculation of MF");

            var stopWatch = new StopWatch();
            stopWatch.start();

            Supplier<Matrix> MC_MZSupplier = () -> {
                MCReadLock.lock();
                MZReadLock.lock();

                var MC_MZ = MC.multiplyRight("MC_MZ", MZ);

                MCReadLock.unlock();
                MZReadLock.unlock();

                return MC_MZ;
            };

            Supplier<Matrix> B_DSupplier = () -> {
                BReadLock.lock();
                DReadLock.lock();

                var B_D = B.add("B_D", D);

                BReadLock.unlock();
                DReadLock.unlock();

                return B_D;
            };

            var B_D_MC_MZFuture = CompletableFuture
                    .supplyAsync(MC_MZSupplier)
                    .thenCombine(CompletableFuture.supplyAsync(B_DSupplier), (MC_MZ, B_D) -> MC_MZ.multiply("B_D_MC_MZ", "min_BD", B_D.min()));

            Supplier<Matrix> MM_MC_MM_aSupplier = () -> {
                MCReadLock.lock();
                MMReadLock.lock();

                var MC_MM = MC.add("MC_MM", MM);

                MCReadLock.unlock();

                var MM_MC_MM = MM.multiplyRight("MM_MC_MM", MC_MM);

                MMReadLock.unlock();
                aReadLock.lock();

                var MM_MC_MM_a = MM_MC_MM.multiply("MM_MC_MM_a", "a", a);

                aReadLock.unlock();

                return MM_MC_MM_a;
            };

            CompletableFuture<Matrix> MFFuture = B_D_MC_MZFuture
                    .thenCombine(CompletableFuture.supplyAsync(MM_MC_MM_aSupplier), (B_D_MC_MZ, MM_MC_MM_a) -> B_D_MC_MZ.add("MF", MM_MC_MM_a));

            var MF = MFFuture.get();
            stopWatch.stop();

            log.info(String.format("Time taken to calculate matrix %s is %s ms", MF.getName(), stopWatch.getTime()));
            FileUtil.storeMatrix("output", MF);
            return MF;
        } catch (InterruptedException | ExecutionException exception) {
            throw new RuntimeException(exception);
        }
    }
}
