package org.example.pms.service;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public interface PortfolioService {
    void addPortfolio(String symbol, long quantity);

    List<PositionInfo> getPortfolioView();

    void onPortfolioUpdated(Consumer<Collection<MarketDataService.PriceTick>> action);

    @Data
    class PositionInfo {
        private String symbol;
        private Double price;
        private long quantity;
        private BigDecimal value;
    }
}
