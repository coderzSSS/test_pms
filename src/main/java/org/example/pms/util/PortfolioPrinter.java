package org.example.pms.util;

import org.example.pms.service.MarketDataService;
import org.example.pms.service.PortfolioService;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class PortfolioPrinter {
    private final PrintStream printStream;

    private AtomicInteger counter = new AtomicInteger(0);

    public PortfolioPrinter(PrintStream printStream) {
        this.printStream = printStream;
    }

    public void print(Collection<MarketDataService.PriceTick> priceTickList, List<PortfolioService.PositionInfo> positionInfoList) {
        int count = counter.incrementAndGet();

        printStream.printf("## %d Market Data Update%n", count);
        priceTickList.forEach(priceTick -> {
            printStream.printf("%s change to %s %n", priceTick.getSymbol(), priceTick.getPrice());
        });

        printStream.println();
        printStream.printf("## Portfolio%n");
        String fmtStr = "%-25s %15s %15s %15s%n";
        printStream.printf(fmtStr, "symbol", "price", "qty", "value");

        positionInfoList.stream().sorted(Comparator.comparing(PortfolioService.PositionInfo::getSymbol))
                .forEach(positionInfo -> {
                    printStream.printf(fmtStr, positionInfo.getSymbol(), positionInfo.getPrice(), positionInfo.getQuantity(), positionInfo.getValue());
                });

        printStream.println();

        BigDecimal value = positionInfoList.stream()
                .map(PortfolioService.PositionInfo::getValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        printStream.printf("#Total portfolio %56s%n%n", value);
    }
}
