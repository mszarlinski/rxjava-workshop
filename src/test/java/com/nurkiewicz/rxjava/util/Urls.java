package com.nurkiewicz.rxjava.util;

import io.reactivex.Flowable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Urls {

    public static Flowable<URL> all() {
        return all("urls.txt");
    }

    public static Flowable<URL> all(String fileName) {
        return Flowable.defer(() -> load(fileName));
    }

    // http://www.nurkiewicz.com/2017/08/loading-files-with-backpressure-rxjava.html
    private static Flowable<URL> load(String fileName) {
        try (Stream<String> lines = classpathReaderOf(fileName).lines()) {
            return Flowable.fromIterable(lines
                    .map(line -> {
                        try {
                            return new URL(line);
                        } catch (MalformedURLException e) {
                            return null;
                        }
                    })
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            return Flowable.error(e);
        }
    }

    private static BufferedReader classpathReaderOf(String fileName) throws IOException {
        URL input = Urls.class.getResource(fileName);
        if (input == null) {
            throw new FileNotFoundException(fileName);
        }
        InputStream is = input.openStream();
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

}
