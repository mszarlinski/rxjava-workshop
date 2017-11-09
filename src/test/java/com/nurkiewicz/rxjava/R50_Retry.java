package com.nurkiewicz.rxjava;

import com.nurkiewicz.rxjava.util.CloudClient;
import io.reactivex.Flowable;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.concurrent.atomic.LongAdder;

import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@Ignore
public class R50_Retry {
	
	private static final Logger log = LoggerFactory.getLogger(R50_Retry.class);
	
	private CloudClient cloudClient = mock(CloudClient.class);
	
	/**
	 * Hint: retry(int)
	 * Hint: doOnError(), doOnSubscribe() for logging
	 */
	@Test
	public void shouldRetryThreeTimes() throws Exception {
		//given
		LongAdder subscriptionCounter = new LongAdder();
		given(cloudClient.pricing()).willReturn(
				failure()
						.doOnSubscribe(disposable -> subscriptionCounter.increment())
		);
				
		//when
		cloudClient
				.pricing()
				.doOnSubscribe(__ -> log.info("Ponowna subskrypcja!"))
				.doOnError(e -> log.error("Before" + e))
				.retry(3)
				.doOnError(e -> log.error("After" + e))
				.test();
		
		//then
		await().until(() -> subscriptionCounter.sum() == 4);
	}
	
	private Flowable<BigDecimal> failure() {
		return Flowable.error(new RuntimeException("Simulated"));
	}

	// https://gist.github.com/hzsweers/7902e3a0286774630f4f
// retries up to 3 times while exponentially backing off with each retry
//                .retryWhen(errors ->
//			errors
//					.zipWith(
//			Observable.range(1, MAX_RETRIES), (n, i) -> i
//                                )
//										.flatMap(
//			retryCount -> Observable.timer((long) Math.pow(4, retryCount), TimeUnit.SECONDS)
//			)
//			)
}
