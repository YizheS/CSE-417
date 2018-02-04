package cse417;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Program that finds the longest "sideways" trend in price data. The option
 * {@code --max-pct-change} controls how far apart the high and low (closing)
 * prices can be, in percentage terms, during a period for it to be consider a
 * sideways trend. This defaults to 5%.
 */
public class SidewaysTrend {

  /** Format for the dates used in the data files. */
  private static final DateFormat DATE_FORMAT =
      new SimpleDateFormat("dd-MMM-yy");

  /** Entry point for a program to build a model of NFL teams. */
  public static void main(String[] args) throws Exception {
    ArgParser argParser = new ArgParser("SidewaysTrend");
    argParser.addOption("max-pct-change", Double.class);
    argParser.addOption("naive", Boolean.class);
    args = argParser.parseArgs(args, 1, 1);

    double maxPctChange = argParser.hasOption("max-pct-change") ?
        argParser.getDoubleOption("max-pct-change") : 5.0;

    List<Date> dates = new ArrayList<Date>();
    List<Integer> prices = loadPrices(args[0], dates);

    Range longest;
    if (argParser.hasOption("naive")) {
      longest = findLongestSidewaysTrendNaive(maxPctChange, prices);
    } else {
      longest = findLongestSidewaysTrend(
          maxPctChange, prices, 0, prices.size()-1);
    }

    System.out.printf(
        "Longest sideways trend is from %s to %s (%d trading days)\n",
        DATE_FORMAT.format(dates.get(longest.firstIndex)),
        DATE_FORMAT.format(dates.get(longest.lastIndex)),
        longest.length());
    System.out.printf("Price range is %.2f to %.2f, a %.1f%% change\n",
        longest.lowPrice/100., longest.highPrice/100.,
        100. * (longest.highPrice - longest.lowPrice) / longest.lowPrice);
  }

  /**
   * Returns the prices in the file. Prices are returned in units of cents
   * ($0.01) to avoid roundoff issues elsewhere in the code.
   * @param fileName Name of the CSV file containing price data
   * @param dates If non-null, dates will be stored in this list. In this case,
   *     we will also check that the prices are in order of increasing date.
   */
  private static List<Integer> loadPrices(String fileName, List<Date> dates)
      throws IOException, ParseException {
    assert (dates == null) || (dates.size() == 0);

    // Stores the relevant information from one row of data.
    class Row {
      public final Date date;
      public final int price;
      public Row(Date date, int price) { this.date = date; this.price = price; }
    }
    List<Row> rows = new ArrayList<Row>();

    CsvParser parser = new CsvParser(fileName, true, new Object[] {
          DATE_FORMAT, Float.class, Float.class, Float.class, Float.class,
          String.class, String.class
        });
    while (parser.hasNext()) {
      String[] parts = parser.next();
      double close = Double.parseDouble(parts[1]);
      rows.add(new Row(DATE_FORMAT.parse(parts[0]), (int)(100 * close)));
    }

    // Put the rows in increasing order of date.
    Collections.sort(rows, (r1, r2) -> r1.date.compareTo(r2.date));

    // If requested, otput the dates from the file.
    if (dates != null) {
      for (Row row : rows)
        dates.add(row.date);
    }

    // Return the prices from the file.
    List<Integer> prices = new ArrayList<Integer>();
    for (Row row : rows)
      prices.add(row.price);
    return prices;
  }

  /** Returns the range with the longest sideways trend in the price data. */
  private static Range findLongestSidewaysTrendNaive(
		  double maxPctChange, List<Integer> prices) {

    // TODO: implement this properly...
    int pLength = prices.size();
    Range longest = Range.fromOneIndex(0, prices);
    for (int i = 0; i < pLength; i++) {
      Range begin = Range.fromOneIndex(i, prices);
      for (int j = i + 1; j < pLength; j++) {
        Range runner = Range.fromOneIndex(j, prices);
        begin = begin.concat(runner);
        // If the percentage change is higher and is more than the previous, replace
        if (begin.percentChangeAtMost(maxPctChange) &&
            longest.length() <= begin.length()) {
           longest = begin;
        }
      }
    }
      return longest;
  }

  /**
   * Returns the range with the longest sideways trend in the price data from
   * {@code firstIndex} to {@code lastIndex} (inclusive).
   */
  private static Range findLongestSidewaysTrend(double maxPctChange,
      List<Integer> prices, int firstIndex, int lastIndex) {
    assert firstIndex <= lastIndex;

    // TODO: implement this properly...

    // Base case: with only one element, return the index itself
    if (firstIndex == lastIndex) {
      return Range.fromOneIndex(firstIndex, prices);
    }

    int midpoint = (firstIndex + lastIndex)/2;

    // Splits range in half
    Range firstHalf = findLongestSidewaysTrend(maxPctChange, prices,
                        firstIndex, midpoint);
    Range secondHalf = findLongestSidewaysTrend(maxPctChange, prices,
                        midpoint + 1, lastIndex);
    Range combined = findLongestSidewaysTrendCrossingMidpoint(maxPctChange, prices,
                        firstIndex, midpoint + 1, lastIndex);

    Range longestRange;

    if (firstHalf.length() >= secondHalf.length()) longestRange = firstHalf;
    else longestRange = secondHalf;

    if (combined != null && combined.length() >= longestRange.length()) {
      longestRange = combined;
    }
    return longestRange;
  }

  /**
   * Returns the range with the longest sideways trend in the price data from
   * {@code firstIndex} to {@code lastIndex} (inclusive) that either starts
   * at or before {@code midIndex} or ends at or after {@code midIndex+1}. (If
   * no such range defines a sideways trend, then it returns null.)
   */
  private static Range findLongestSidewaysTrendCrossingMidpoint(
      double maxPctChange, List<Integer> prices, int firstIndex, int midIndex,
      int lastIndex) {

      // Goes through range 0 to (n/2 - 1)
      List<Range> lowerHalf = new ArrayList<Range>();
      Range longestLower = Range.fromOneIndex(midIndex - 1, prices);
      Range end = Range.fromOneIndex(midIndex - 1, prices); // n/2 - 1
      for (int i = midIndex - 2; i >= firstIndex; --i) {
         Range runner = Range.fromOneIndex(i, prices);
         end = runner.concat(end);
         if (end.percentChangeAtMost(maxPctChange)) {
            lowerHalf.add(end);
            if (end.length() >= longestLower.length()) {
               longestLower = end;
            }
         }
      }

      // Goes through range n/2 to n
      Range begin = Range.fromOneIndex(midIndex, prices);
      List<Range> upperHalf = new ArrayList<Range>();
      for (int i = midIndex + 1; i <= lastIndex; ++i) {
         Range runner = Range.fromOneIndex(i, prices);
         begin = begin.concat(runner);
         if (begin.percentChangeAtMost(maxPctChange)) {
            upperHalf.add(begin);
         }
      }

      Range longestCombined = null;
      for (Range upper : upperHalf) {
         Range crossing = longestLower.concat(upper);
         if (!crossing.percentChangeAtMost(maxPctChange)) {
            return longestCombined;
         }
         longestCombined = crossing;
      }
    return null;
  }
}
