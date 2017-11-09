package com.nurkiewicz.rxjava;

import io.reactivex.Flowable;
import io.reactivex.Single;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Ignore
public class R80_Single {

    // Single - 1
    // Maybe - 0..1
    // Completable 0
    // Flowable 0..N

    @Test
    public void simpleSingle() throws Exception {
        Single<Instant> time = Single.create(sub -> {
            sub.onSuccess(Instant.now());
        });

        Single<List<Integer>> single = Flowable.just(1).toList();

        // from CompletableFuture to Single
        Single<String> fromCompletableFuture = Single.create(sub -> {
            CompletableFuture<String> fut = CompletableFuture.completedFuture("abc"); // call webservice
            fut.whenComplete((result, ex) -> {
                if (ex != null) {
                    sub.onError(ex);
                } else {
                    sub.onSuccess(result);
                }
            });
        });
    }


}
