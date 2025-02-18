package org.example.pms.service;

import lombok.Value;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public interface MarketDataService {
    void start();

    void stop();

    Optional<Double> getLastPrice(String symbol);

    void subscribe(Collection<String> symbols, Consumer<Collection<PriceTick>> priceListener);

    @Value
    class PriceTick {
        String symbol;
        double price;
    }
}
