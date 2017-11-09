package com.nurkiewicz.rxjava;

import com.google.common.base.MoreObjects;
import com.nurkiewicz.rxjava.util.Sleeper;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class R60_GroupBy {

	private static final Logger log = LoggerFactory.getLogger(R60_GroupBy.class);

	/**
	 * Hint: buffer() and List.size()
	 */
	@Test
	public void countClicksPerSecondUsingBuffer() throws Exception {
		//given
		Observable<Click> clicks = clicks(Schedulers.io());

		//when
		clicks
				.buffer(1, SECONDS)
				.map(List::size)
				.take(5)
				.blockingSubscribe(x -> log.info("{} clicks/s", x));

		//then
	}

	/**
	 * Hint: Observable.count()
	 * Do you think window() or buffer() is better? Why?
	 */
	@Test
	public void countClicksPerSecondUsingWindow() throws Exception {
		//given
		Observable<Click> clicks = clicks(Schedulers.io());

		//when
		clicks
				.window(1, SECONDS)
				.flatMapSingle(Observable::count)
				.take(5)
				.blockingSubscribe(x -> log.info("{} clicks/s", x));

		//then
	}

	@Test
	public void shouldCountClicksPerSecondUsingTestScheduler() throws Exception {
		//given
		TestScheduler scheduler = new TestScheduler();
		Observable<Click> clicks = clicks(scheduler);

		//when
		final TestObserver<Long> subscriber = clicks
				.window(1, SECONDS, scheduler)
				.flatMapSingle(Observable::count)  //TODO Use window() to count here
				.test();

		//then
		scheduler.advanceTimeBy(1_000 - 1, MILLISECONDS);
		subscriber.assertNoValues();

		scheduler.advanceTimeBy(1, MILLISECONDS);
		subscriber.assertValueCount(1);

		scheduler.advanceTimeBy(1_000 - 1, MILLISECONDS);
		subscriber.assertValueCount(1);

		scheduler.advanceTimeBy(1, MILLISECONDS);
		subscriber.assertValueCount(2);

		scheduler.advanceTimeBy(1_000 - 1, MILLISECONDS);
		subscriber.assertValueCount(2);

		scheduler.advanceTimeBy(1, MILLISECONDS);
		subscriber.assertValueCount(3);
	}

	/**
	 * Total clicks from which country?
	 * Hint: grouped.getKeu
	 * Hint: Pair class will be useful
	 */
	@Test
	public void groupingClicksPerCountry() throws Exception {
		//given
		Observable<Click> clicks = clicks(Schedulers.io());

		//when
        Observable<Pair<Country, Long>> grouped = clicks
                .take(1000)
                .groupBy(Click::getCountry)
                .flatMapSingle(group -> group
                        .count()
                        .map(cnt -> Pair.of(group.getKey(), cnt)));

        grouped.blockingSubscribe(x -> log.info("Total {} clicks from {} country", x.getKey(), x.getValue()));

		//then
	}


	@Test
	public void groupingAndCountingClicksByCountry() throws Exception {
		//given
		Observable<Click> clicks = clicks(Schedulers.io());

		//when
        Observable<Pair<Country, Observable<Click>>> groups = clicks
                .groupBy(Click::getCountry)
                .flatMap(group -> group.window(1, SECONDS)
                        .map(win -> Pair.of(group.getKey(), win)));

        Observable<String> map = groups
                .flatMapSingle(p -> p.getValue().count()
                        .map(cnt -> String.format("%s-%d", p.getKey(), cnt)));

        map.subscribe(x -> System.out.println(x));

        Sleeper.sleep(Duration.ofMinutes(1));

        List<String> firstStats = map
				.take(3)
				.toList()
				.blockingGet()
				.stream()
				.sorted()
				.collect(toList());

		//then
		assertThat(firstStats.get(0)).matches("DE-\\d+");
		assertThat(firstStats.get(1)).matches("PL-\\d+");
		assertThat(firstStats.get(2)).matches("US-\\d+");
	}


	Observable<Click> clicks(Scheduler scheduler) {
		return Observable
				.interval(3, MILLISECONDS, scheduler)
				.map(x -> Click.random(scheduler));
	}

}

class Click {
	private final Instant when;
	private final Country country;

	private Click(Instant when, Country country) {
		this.when = when;
		this.country = country;
	}

	static Click random(Scheduler scheduler) {
		return new Click(
				Instant.ofEpochMilli(scheduler.now(MILLISECONDS)),
				Country.random()
		);
	}

	public Country getCountry() {
		return country;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("when", when)
				.add("country", country)
				.toString();
	}
}

enum Country {
	PL, DE, US;

	static Country random() {
		double rand = Math.random();
		if (rand < 0.33) {
			return PL;
		}
		if (rand < 0.67) {
			return DE;
		}
		return US;
	}
}