package com.techwitz.accessibility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Parser for HTML content, supporting both files and URLs
 */
public class HtmlParser {
    private static final Logger LOGGER = Logger.getLogger(HtmlParser.class.getName());

    /**
     * Parse HTML from a file or URL
     * @param source File path or URL to parse
     * @return Parsed Document object
     */
    public Document parse(String source) throws IOException {
        if (source.startsWith("http://") || source.startsWith("https://")) {
            return parseUrl(source);
        } else {
            return parseFile(source);
        }
    }

    private Document parseUrl(String url) throws IOException {
        LOGGER.info("Parsing URL: " + url);
        return Jsoup.connect(url)
                .userAgent("WCAG Compliance Tool/1.0")
                .timeout(10000)
                .get();
    }

    private Document parseFile(String filePath) throws IOException {
        LOGGER.info("Parsing file: " + filePath);
        File input = new File(filePath);
        return Jsoup.parse(input, "UTF-8");
    }
}

