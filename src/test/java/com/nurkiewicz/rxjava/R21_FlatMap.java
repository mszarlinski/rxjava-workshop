package com.nurkiewicz.rxjava;

import com.nurkiewicz.rxjava.util.UrlDownloader;
import com.nurkiewicz.rxjava.util.Urls;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class R21_FlatMap {

    /**
     * Hint: UrlDownloader.download()
     * Hint: flatMap(), maybe concatMap()?
     * Hint: toList()
     * Hint: blockingGet()
     */
    @Test
    public void shouldDownloadAllUrlsInArbitraryOrder() throws Exception {
        Flowable<URL> urls = Urls.all();

        //when
        Flowable<String> map = urls
                .flatMap(url -> UrlDownloader.download(url) // flatMap synchronizuje wyniki z wielu watkow
                        .subscribeOn(Schedulers.io()));
        // .subscribeOn(Schedulers.io()) - powoduje sekwencyjne sciaganie

        List<String> bodies = map.toList().blockingGet();

        //then
        assertThat(bodies).hasSize(996);
        assertThat(bodies).contains("<html>www.twitter.com</html>", "<html>www.aol.com</html>", "<html>www.mozilla.org</html>");
    }

    /**
     * Hint: Pair.of(...)
     * Hint: Flowable.toMap()
     */
    @Test
    public void shouldDownloadAllUrls() throws Exception {
        //given
        Flowable<URL> urls = Urls.all();

        //when
        //WARNING: URL key in HashMap is a bad idea here - equals & hashCode over Internet

        Flowable<Pair<URI, String>> pairs = urls.
                flatMap(url -> UrlDownloader.downloadAsync(url, Schedulers.io())
                        .map(html -> Pair.of(URI.create(url.toString()), html)));

        Map<URI, String> bodies = pairs.toMap(Pair::getKey, Pair::getValue).blockingGet();

        //then
        assertThat(bodies).hasSize(996);
        assertThat(bodies).containsEntry(new URI("http://www.twitter.com"), "<html>www.twitter.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.aol.com"), "<html>www.aol.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.mozilla.org"), "<html>www.mozilla.org</html>");
    }

    // flatMap        - subskrybuje sie do wszystkich 1000 podstrumieni i robi strumien wynikowy
    // concatMap      - tez sie subskrybuje, ale sekwencyjnie, co zapewnie kolejnosc, ale brak wspolbieznosci
    // concatMapEager - od razu sie subskrybuje, ale buforuje zwrotki, zeby zwrocic w odpowiedniej kolejnosci (slabe)

    /**
     * Hint: flatMap with int parameter
     */
    @Test
    public void downloadThrottled() throws Exception {
        //given
        Flowable<URL> urls = Urls.all().take(20);

        //when
        //Use UrlDownloader.downloadThrottled()

        Flowable<Pair<URI, String>> pairs = urls.
                flatMap(url -> UrlDownloader.downloadThrottled(url).subscribeOn(Schedulers.io())
                        .map(html -> Pair.of(URI.create(url.toString()), html)), 10);
        // Lepsze rozwiazanie niz wlasna pula watkow, bo flatMap
        // i tak zasubskrybuje sie do 1000 watkow i spuchnie kolejka
        // Defaultowo flatMap odpala 128 watkow

        Map<URI, String> bodies = pairs.toMap(Pair::getKey, Pair::getValue).blockingGet();

        //then
        assertThat(bodies).containsEntry(new URI("http://www.twitter.com"), "<html>www.twitter.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.adobe.com"), "<html>www.adobe.com</html>");
        assertThat(bodies).containsEntry(new URI("http://www.bing.com"), "<html>www.bing.com</html>");
    }

    private URI toUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
