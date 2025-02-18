package org.example.pms.service.mock;

import com.google.common.collect.Sets;
import org.example.pms.service.MarketDataService;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Consumer;

public class MockMarketDataService extends TimerTask implements MarketDataService {
    private Set<String> subscribedSymbols;
    private Consumer<Collection<PriceTick>> priceTickConsumer;

    private Map<String, Double> data = new HashMap<>();
    private Timer timer = null;

    public static final MathContext PRICE_MC = new MathContext(2, RoundingMode.HALF_UP);

    @Override
    public void start() {
        doNotifyPriceTicks(initialPriceTicks());

        if (timer != null) {
            throw new IllegalStateException("Timer has already been started");
        }

        timer = new Timer();
        timer.schedule(this, 100, 1000);
    }

    private List<PriceTick> initialPriceTicks() {
        return List.of(
                new PriceTick("AAPL", 110.0),
                new PriceTick("TELSA", 400.0)
        );
    }

    @Override
    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    public void run() {
        long sleepInMillis = (long) (2000 * Math.random());
        try {
            Thread.sleep(sleepInMillis);
        } catch (InterruptedException e) {
            // noop
        }

        if (subscribedSymbols != null) {
            List<String> tickList = subscribedSymbols.stream().toList();

            Random random = new Random();
            int size = subscribedSymbols.size();
            int randomTickSize = random.nextInt(1, size + 1);
            Set<Integer> indexes = Sets.newHashSet();
            for (int i = 0; i < randomTickSize; i++) {
                indexes.add(random.nextInt(size));
            }

            List<PriceTick> priceTicks = indexes.stream().map(index -> {
                BigDecimal changePercent = BigDecimal.valueOf(random.nextInt(30));
                String symbol = tickList.get(index);
                BigDecimal initPrice = BigDecimal.valueOf(data.getOrDefault(symbol, 10.0)).round(PRICE_MC);
                boolean upOrDown = random.nextBoolean();
                BigDecimal sign = upOrDown ? BigDecimal.ONE : BigDecimal.ONE.negate();
                BigDecimal newPrice = initPrice.multiply(
                        BigDecimal.ONE.add(
                                sign.multiply(changePercent).divide(BigDecimal.valueOf(100), PRICE_MC)
                        )
                );
                if (newPrice.compareTo(initPrice) != 0) {
                    return new PriceTick(symbol, newPrice.doubleValue());
                }
                return null;
            }).filter(Objects::nonNull).toList();

            doNotifyPriceTicks(priceTicks);
        }
    }

    private void doNotifyPriceTicks(Collection<PriceTick> priceTicks) {
        if (priceTickConsumer != null && !priceTicks.isEmpty()) {
            priceTicks.forEach(priceTick -> {
                data.put(priceTick.getSymbol(), priceTick.getPrice());
            });

            priceTickConsumer.accept(priceTicks);
        }
    }

    @Override
    public Optional<Double> getLastPrice(String symbol) {
        return Optional.ofNullable(data.get(symbol));
    }

    @Override
    public void subscribe(Collection<String> symbols, Consumer<Collection<PriceTick>> priceListener) {
        subscribedSymbols = Sets.newHashSet(symbols);
        priceTickConsumer = priceListener;
    }
}
