package de.tukl.softech.exclaim.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class AsyncUtils {


    public static <T> CompletableFuture<T> timeoutAfter(CompletableFuture<T> fut, T value, long timeout, TimeUnit unit) {
        // TODO if we switch to Java 9 this can be replaced by CompletableFuture.completeOnTimeout​
        CompletableFuture<T> res = new CompletableFuture<>();
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(TimeUnit.MILLISECONDS.convert(timeout, unit));
                res.complete(value);
                fut.cancel(true);
            } catch (InterruptedException e) {
                // cancel
            }
        });
        t.start();
        fut.thenAccept(val -> {
            t.interrupt();
            res.complete(val);
        });
        return res;
    }


}
