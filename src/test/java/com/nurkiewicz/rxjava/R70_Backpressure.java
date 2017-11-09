package com.nurkiewicz.rxjava;

import com.nurkiewicz.rxjava.util.InfiniteReader;
import com.nurkiewicz.rxjava.util.NumberSupplier;
import com.nurkiewicz.rxjava.util.Sleeper;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.annotations.BackpressureKind;
import io.reactivex.schedulers.Schedulers;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Ignore
public class R70_Backpressure {
	
	private static final Logger log = LoggerFactory.getLogger(R70_Backpressure.class);

	//.subscribe(record
	// 		-> storeInMongo(record)) // najgorsze rozwiazanie - "implicit synchronized"

	// .flatMap(record -> storeInMongoAsync().subscribeOn(io()))

	// .parallel().runOn(io()).map(record -> storeInMongo(record)).sequential()

	// .buffer(100).map(batch -> storeInMongoBatch(batch))

	// Backpressure - caly czas  protokol push & pull

	// io.reactivex.exceptions.OnErrorNotImplementedException: Can't deliver value 128 due to lack of requests
	// Scheduler ma domyslnie kolejke na 128 elementow, wyjatek rzuca publikujacy (subskrybent nie poprosil o eventy)

	// .doOnRequest(n) - o ile elementow poprosil subskrybent

	/** {@link BackpressureKind}
	*/

	// Flowable.create() - ma wymagany parametr BackpressureStrategy

	/** {@link BackpressureStrategy}
	 */

	// MISSING - YOLO, ktorys z operatorow subskrybenta zglosi blad
	// ERROR - blad przy drugim evencie, spropagowany do subskrybenta
	// BUFFER - nieskonczony bufor
	// DROP - dropowanie eventow
	// LATEST - trzymanie ostatniej wersji

	// io.reactivex.exceptions.OnErrorNotImplementedException - brak obslugi errora onError()

	@Test
	public void missingBackpressure() throws Exception {
		Flowable
				.interval(5, TimeUnit.MILLISECONDS)
				.doOnNext(x -> log.trace("Emitted: {}", x))
//				.onBackpressureBuffer()
//				.onBackpressureDrop(x -> log.error("Dropped: {}", x)) // Dropowanie przed observeOn()
				.doOnRequest(n -> log.info("B: Asking for {}", n))
				.observeOn(Schedulers.computation()) 				  // Jesli nie bedzie zmiany watku, to w naturalny
																	  // sposob mamy backpressure (ten sam watek, co nadawca)
				.doOnNext(x -> log.trace("Handling: {}", x))
				.doOnRequest(n -> log.info("A: Asking for {}", n))
				.subscribe(x -> Sleeper.sleep(Duration.ofMillis(6)));
		
		TimeUnit.SECONDS.sleep(30);
	}
	
	@Test
	public void loadingDataFromInfiniteReader() throws Exception {
		//given
		Flowable<String> numbers = Flowable.create(sub -> pushNumbersToSubscriber(sub), BackpressureStrategy.ERROR);

		//then
		numbers
				.take(4)
				.test()
				.assertValues("0", "1", "2", "3");
	}
	
	@Test
	public void backpressureIsNotAproblemIfTheSameThread() throws Exception {
		Flowable<String> numbers = Flowable.create(sub -> pushNumbersToSubscriber(sub), BackpressureStrategy.ERROR);
		
		numbers
				.doOnNext(x -> log.info("Emitted: {}", x))
				.subscribe(x -> Sleeper.sleep(Duration.ofMillis(6)));
	}
	
	/**
	 * TODO Reimplement `numbers` so that lines are pulled by subscriber, not pushed to subscriber
	 */
	@Test
	public void missingBackpressureIfCrossingThreads() throws Exception {
		Flowable<String> numbers = Flowable.create(sub -> pushNumbersToSubscriber(sub), BackpressureStrategy.ERROR);

		numbers
				.observeOn(Schedulers.io())
				.blockingSubscribe(x -> Sleeper.sleep(Duration.ofMillis(6)));
	}

	private void pushNumbersToSubscriber(FlowableEmitter<? super String> sub) {
		try (Reader reader = new InfiniteReader(NumberSupplier.lines())) {
			BufferedReader lines = new BufferedReader(reader);
			while (!sub.isCancelled()) {
				sub.onNext(lines.readLine());
			}
		} catch (IOException e) {
			sub.onError(e);
		}
	}

}
