package com.nurkiewicz.rxjava;

import com.nurkiewicz.rxjava.util.Sleeper;
import io.reactivex.Flowable;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.nurkiewicz.rxjava.R30_Zip.LOREM_IPSUM;

@Ignore
public class R31_WindowBuffer {

    /**
     * Hint: use buffer()
     */
    @Test
    public void everyThirdWordUsingBuffer() throws Exception {
        //given
        Flowable<String> everyThirdWord = LOREM_IPSUM
                .skip(2)
                .buffer(3)
                .map(l -> l.get(0));

        //then
        everyThirdWord
                .test()
                .assertValues("dolor", "consectetur")
                .assertNoErrors();
    }

//	Flowable<Long> numbers = Flowable.inverval(30, NANOSECONDS);
//
//	numbers.buffer(1, SECONDS)
//		.map(List::size)
//		.subscribe(System.out::println);

    /**
     * Hint: use window()
     * Hint: use elementAt()
     */
    @Test
    public void everyThirdWordUsingWindow() throws Exception {
        //given
        Flowable<Flowable<String>> window = LOREM_IPSUM
                .skip(2)
                .window(3); // Nie tworzy listy jak buffer(), tylko tworzy leniwe strumienie

        Flowable<String> everyThirdWord = window
                .flatMap(f -> f.firstElement().toFlowable()); // flatMapMaybe()

        //then
        everyThirdWord
                .test()
                .assertValues("dolor", "consectetur")
                .assertNoErrors();
    }


    @Test
    public void rps() {
        Flowable<Long> numbers = Flowable.interval(10, TimeUnit.MILLISECONDS);

        Flowable<Long> map = numbers
                .window(1, TimeUnit.SECONDS)
                .flatMapSingle(Flowable::count);

        map.subscribe(System.out::println);

        Sleeper.sleep(Duration.ofMinutes(1));
    }
}
