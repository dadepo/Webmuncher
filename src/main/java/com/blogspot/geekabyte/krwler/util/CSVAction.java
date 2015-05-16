package com.blogspot.geekabyte.krwler.util;

import com.blogspot.geekabyte.krwler.FetchedPage;
import com.blogspot.geekabyte.krwler.interfaces.KrwlerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link KrwlerAction} that dumps crawled pages
 * to a CSV file.
 * <p/>
 * Created by dadepo on 1/8/15.
 */
public class CSVAction implements KrwlerAction {

    Logger logger = LoggerFactory.getLogger(CSVAction.class);

    private boolean isFirstPage = true;
    private Path destination = Paths.get(LocalDateTime.now() + "_" + "./out.csv");
    private CsvPreference csvformat = CSVFORMAT.DEFAULT.getValue();
    private ICsvMapWriter mapWriter;
    private BufferedWriter fileWriter;
    private boolean convertToPlainText = false;

    // prevents direct instantiation
    private CSVAction() {
    }

    /**
     * Returns the location where the CSV would be written to
     *
     * @return the location where the CSV would be written to
     */
    public Path getDestination() {
        return destination;
    }

    private void setDestination(Path destination) {
        this.destination = Paths.get(destination.toString());
    }

    private void setCSVFormat(CSVFORMAT format) {
        this.csvformat = format.getValue();
    }

    private boolean isPlainText() {
        return convertToPlainText;
    }

    private void convertToPlainText(boolean convertToPlainText) {
        this.convertToPlainText = convertToPlainText;
    }

    // Convenient access to the builder constructor, alternative to having to do a
    // new CSVAction.Build() by the client
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating instances of {@link com.blogspot.geekabyte.krwler.util.CSVAction}
     */
    public static class Builder {

        private CSVAction instance = new CSVAction();

        /**
         * Sets the destination where the csv file would be written. If not used, the out put would be written
         * to {@code out.csv} in the running directory
         *
         * @param file the file to write csv
         * @return the builder
         */
        public Builder setDestination(Path file) {
            instance.setDestination(file);
            return this;
        }

        /**
         * Sets the format used for writing the CSV file
         *
         * @param format the format expressed as a {@link com.blogspot.geekabyte.krwler.util.CSVAction.CSVFORMAT}
         *               if none is configured the
         *               default is {@link com.blogspot.geekabyte.krwler.util.CSVAction.CSVFORMAT#DEFAULT}
         * @return the builder
         */
        public Builder setCSVFormat(CSVFORMAT format) {
            instance.setCSVFormat(format);
            return this;
        }

        /**
         * Use to set if plain text version or html version should be saved to CSV. True saves plain text; false
         * saves HTML version. Default is false
         *
         * @param toPlainText true or false
         * @return the builder
         */
        public Builder convertToPlainText(boolean toPlainText) {
            instance.convertToPlainText(toPlainText);
            return this;
        }

        /**
         * Returns a properly configured instance of {@link com.blogspot.geekabyte.krwler.util.CSVAction}
         *
         * @return an instance of {@link com.blogspot.geekabyte.krwler.util.CSVAction}
         */
        public CSVAction buildAction() {
            return instance;
        }
    }

    @Override
    public void execute(FetchedPage page) {
        try {
            if (isFirstPage) {
                // Create new file and add headers
                Files.createFile(destination);
                fileWriter = Files.newBufferedWriter(destination, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                mapWriter = new CsvMapWriter(fileWriter, csvformat);
                mapWriter.writeHeader(getHeaders(page).split(","));
                doWrite(page);
                isFirstPage = false;
            } else {
                fileWriter = Files.newBufferedWriter(destination, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                mapWriter = new CsvMapWriter(fileWriter, csvformat);
                doWrite(page);
            }
        } catch (Exception e) {
            logger.debug("Exception while writing CSV file", e);
        } finally {
            if (mapWriter != null) {
                try {
                    mapWriter.close();
                } catch (IOException e) {
                    logger.debug("Exception while closing resources used for csv", e);
                }
            }
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    logger.debug("Exception while closing resources used for csv", e);
                }
            }
        }
    }

    private void doWrite(FetchedPage page) throws IOException {
        String[] headers = getHeaders(page).split(",");
        final Map<String, Object> entry = new HashMap<>();
        entry.put(headers[0], callCorrespondingGetterMethod(page, headers[0]));
        entry.put(headers[1], callCorrespondingGetterMethod(page, headers[1]));
        entry.put(headers[2], callCorrespondingGetterMethod(page, headers[2]));
        entry.put(headers[3], callCorrespondingGetterMethod(page, headers[3]));
        entry.put(headers[4], callCorrespondingGetterMethod(page, headers[4]));
        entry.put(headers[5], callCorrespondingGetterMethod(page, headers[5]));
        mapWriter.write(entry, headers, getProcessors());
    }

    private Object callCorrespondingGetterMethod(FetchedPage page, String property) {
        Object invoke = null;
        try {
            property = "get" + property.substring(0, 1) + property.substring(1);
            Method method = page.getClass().getMethod(property);
            invoke = method.invoke(page);
        } catch (NoSuchMethodException e) {
            logger.debug("No getter method found for {}", property);
        } catch (InvocationTargetException e) {
            logger.debug("Target exception for {}", property);
        } catch (IllegalAccessException e) {
            logger.debug("Illegal access while trying to call {}", property);
        }
        return invoke;
    }

    private static CellProcessor[] getProcessors() {

        final CellProcessor[] processors = new CellProcessor[]{
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
                new Optional(),
        };
        return processors;
    }

    private String getHeaders(FetchedPage page) {
        StringBuilder stringBuilder = new StringBuilder();
        Method[] declaredMethods = page.getClass().getDeclaredMethods();
        for (Method method : declaredMethods) {
            String name = method.getName();
            if (name.startsWith("get")) {
                String columnName = name.replace("get", "");

                if (columnName.toLowerCase().equals("plainText".toLowerCase())) {
                    if (this.isPlainText()) {
                        stringBuilder.append(columnName + ",");
                    }
                    continue;
                }

                if (columnName.toLowerCase().equals("html".toLowerCase())) {
                    if (!this.isPlainText()) {
                        stringBuilder.append(columnName + ",");
                    }
                    continue;
                }
                stringBuilder.append(columnName + ",");
            }
        }

        String headerStrings = stringBuilder.toString();
        return headerStrings.substring(0, headerStrings.length() - 1);
    }


    /**
     * Representation of the format of CSV file to write
     */
    public enum CSVFORMAT {
        /**
         * Should be applicable for most use cases
         */
        DEFAULT,
        /**
         * For Windows Excel exported CSV files
         */
        EXCEL,
        /**
         * For tab-delimited files
         */
        TAB;

        private CsvPreference getValue() {
            switch (this) {
                case DEFAULT:
                    return CsvPreference.STANDARD_PREFERENCE;
                case EXCEL:
                    return CsvPreference.EXCEL_PREFERENCE;
                case TAB:
                    return CsvPreference.TAB_PREFERENCE;
                default:
                    return CsvPreference.STANDARD_PREFERENCE;
            }
        }
    }
}
