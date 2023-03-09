package com.vladyslavholik;

import com.vladyslavholik.file.FileUtil;
import com.vladyslavholik.model.Matrix;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import java.util.concurrent.Semaphore;


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
        log.info(String.format("Calculation of matrix C and MF completed, time taken: %s ns", stopWatch.getNanoTime()));
    }

    // C = В * МС - D * MM
    public static void calculateC(Semaphore BSemaphore, Semaphore MCSemaphore, Semaphore DSemaphore, Semaphore MMSemaphore) {
        try {
            log.info("Starting calculation of C");
            var stopWatch = new StopWatch();
            stopWatch.start();

            log.info("Waiting to acquire B");
            BSemaphore.acquire();
            log.info("Acquired B");

            log.info("Waiting to acquire MC");
            MCSemaphore.acquire();
            log.info("Acquired MC");

            var B_MC = B.multiplyRight("B_MC", MC);

            BSemaphore.release();
            log.info("Released B");
            MCSemaphore.release();
            log.info("Released MC");

            log.info("Waiting to acquire D");
            DSemaphore.acquire();
            log.info("Acquired D");

            log.info("Waiting to acquire MM");
            MMSemaphore.acquire();
            log.info("Acquired MM");

            var D_MM = D.multiplyRight("D_MM", MM);

            DSemaphore.release();
            log.info("Released D");
            MMSemaphore.release();
            log.info("Released MM");

            var C = B_MC.subtract("C", D_MM);
            stopWatch.stop();

            log.info(String.format("Time taken to calculate matrix %s is %s ns", C.getName(), stopWatch.getNanoTime()));
            FileUtil.storeMatrix("output", C);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }

    // MF = min(B + D) * MC * MZ + MM * (MC + MM) * a
    public static void calculateMF(Semaphore BSemaphore, Semaphore DSemaphore, Semaphore MCSemaphore, Semaphore MZSemaphore, Semaphore MMSemaphore, Semaphore aSemaphore) {
        try {
            log.info("Starting calculation of MF");

            var stopWatch = new StopWatch();
            stopWatch.start();

            log.info("Waiting to acquire MC");
            MCSemaphore.acquire();
            log.info("Acquired MC");

            log.info("Waiting to acquire MZ");
            MZSemaphore.acquire();
            log.info("Acquired MZ");

            var MC_MZ = MC.multiplyRight("MC_MZ", MZ);

            MCSemaphore.release();
            log.info("Released MC");
            MZSemaphore.release();
            log.info("Released MZ");

            log.info("Waiting to acquire B");
            BSemaphore.acquire();
            log.info("Acquired B");

            log.info("Waiting to acquire D");
            DSemaphore.acquire();
            log.info("Acquired D");

            var B_D = B.add("B_D", D);

            BSemaphore.release();
            log.info("Released B");
            DSemaphore.release();
            log.info("Released D");

            var B_D_MC_MZ = MC_MZ.multiply("B_D_MC_MZ", "min_BD", B_D.min());

            log.info("Waiting to acquire MC");
            MCSemaphore.acquire();
            log.info("Acquired MC");

            log.info("Waiting to acquire MM");
            MMSemaphore.acquire();
            log.info("Acquired MM");

            var MC_MM = MC.add("MC_MM", MM);

            MCSemaphore.release();
            log.info("Released MC");

            var MM_MC_MM = MM.multiplyRight("MM_MC_MM", MC_MM);

            MMSemaphore.release();
            log.info("Released MM");

            log.info("Waiting to acquire a");
            aSemaphore.acquire();
            log.info("Acquired a");

            var MM_MC_MM_a = MM_MC_MM.multiply("MM_MC_MM_a", "a", a);

            aSemaphore.release();
            log.info("Released a");

            var MF = B_D_MC_MZ.add("MF", MM_MC_MM_a);
            stopWatch.stop();

            log.info(String.format("Time taken to calculate matrix %s is %s ns", MF.getName(), stopWatch.getNanoTime()));
            FileUtil.storeMatrix("output", MF);
        } catch (InterruptedException exception) {
            throw new RuntimeException(exception);
        }
    }
}
