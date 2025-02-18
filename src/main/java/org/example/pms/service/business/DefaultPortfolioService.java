package org.example.pms.service.business;

import com.google.common.base.Preconditions;
import org.example.pms.service.MarketDataService;
import org.example.pms.service.PortfolioService;
import org.example.pms.service.ProductService;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

import static org.example.pms.service.mock.MockMarketDataService.PRICE_MC;

public class DefaultPortfolioService implements PortfolioService {
    private final ProductService productService;
    private final Map<ProductService.Product, PositionInfo> portfolio = new HashMap<>();
    private final List<Consumer<Collection<MarketDataService.PriceTick>>> listeners = new ArrayList<>();

    public DefaultPortfolioService(ProductService productService) {
        this.productService = productService;
    }

    @Override
    public void addPortfolio(String symbol, long quantity) {
        PositionInfo positionInfo = new PositionInfo();
        positionInfo.setSymbol(symbol);
        positionInfo.setQuantity(quantity);

        ProductService.Product product = productService.lookup(symbol).orElseThrow(() -> new IllegalArgumentException("Unknown product: " + symbol));

        PositionInfo result = portfolio.putIfAbsent(product, positionInfo);
        if (result != null) {
            throw new IllegalArgumentException("product " + symbol + " already added");
        }
    }

    public void onPriceTick(Collection<MarketDataService.PriceTick> priceTicks) {
        List<PositionInfo> updatedPos = priceTicks.stream().distinct().flatMap(priceTick -> {
            return portfolio.entrySet()
                    .stream()
                    .filter(e -> {
                        return e.getKey().getSymbol().contains(priceTick.getSymbol());
                    }).map(entry -> {
                        ProductService.Product product = entry.getKey();
                        PositionInfo positionInfo = entry.getValue();
                        calc(priceTick, product, positionInfo);
                        return positionInfo;
                    });
        }).toList();

        if (!updatedPos.isEmpty()) {
            listeners.forEach(listener -> listener.accept(priceTicks));
        }
    }

    private void calc(MarketDataService.PriceTick priceTick, ProductService.Product product, PositionInfo positionInfo) {
        Preconditions.checkArgument(positionInfo.getQuantity() != 0, "invalid position: " + positionInfo);
        Preconditions.checkArgument(priceTick.getPrice() > 0);

        double price = priceTick.getPrice();
        BigDecimal value = null;

        if (product.getProductType() == ProductService.ProductType.STOCK) {
            Preconditions.checkArgument(priceTick.getSymbol().equals(positionInfo.getSymbol()));

        } else {
            ProductService.Option option = (ProductService.Option) product;
            Preconditions.checkArgument(priceTick.getSymbol().equals(option.getUnderlyingSymbol()));
            boolean callOrPut = option.getPutCall() == ProductService.PutCall.CALL;
            double spotPrice = priceTick.getPrice();
            double strikePrice = option.getStrikePrice();
            double riskFreeRate = 0.1;
            double timeInYears = option.getYear() - Calendar.getInstance().get(Calendar.YEAR);
            double volatility = 0.1;

            price = calcOptionPrice(callOrPut, spotPrice, strikePrice, riskFreeRate, timeInYears, volatility);
        }

        BigDecimal usedPrice = BigDecimal.valueOf(price).round(PRICE_MC);
        value = usedPrice.multiply(BigDecimal.valueOf(positionInfo.getQuantity()));

        positionInfo.setPrice(usedPrice.doubleValue());
        positionInfo.setValue(value);
    }

    // copied from https://github.com/bret-blackford/black-scholes/blob/master/OptionValuation/src/mBret/options/BlackScholesFormula.java
    private double calcOptionPrice(boolean callOrPut, double spotPrice, double strikePrice, double riskFreeRate,
                                   double timeInYears, double volatility) {
        double blackScholesOptionPrice = 0.0;

        if (callOrPut) {
            double cd1 = cumulativeDistribution(d1(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility));
            double cd2 = cumulativeDistribution(d2(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility));

            blackScholesOptionPrice = spotPrice * cd1 - strikePrice * Math.exp(-riskFreeRate * timeInYears) * cd2;
        } else {
            double cd1 = cumulativeDistribution(-d1(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility));
            double cd2 = cumulativeDistribution(-d2(spotPrice, strikePrice, riskFreeRate, timeInYears, volatility));

            blackScholesOptionPrice = strikePrice * Math.exp(-riskFreeRate * timeInYears) * cd2 - spotPrice * cd1;
        }

        return blackScholesOptionPrice;
    }

    private double d1(double s, double k, double r, double t, double v) {
        double top = Math.log(s / k) + (r + Math.pow(v, 2) / 2) * t;
        double bottom = v * Math.sqrt(t);

        return top / bottom;
    }

    private double d2(double s, double k, double r, double t, double v) {
        return d1(s, k, r, t, v) - v * Math.sqrt(t);
    }

    private static double cumulativeDistribution(double x) {
        double t = 1 / (1 + P * Math.abs(x));
        double t1 = B1 * Math.pow(t, 1);
        double t2 = B2 * Math.pow(t, 2);
        double t3 = B3 * Math.pow(t, 3);
        double t4 = B4 * Math.pow(t, 4);
        double t5 = B5 * Math.pow(t, 5);
        double b = t1 + t2 + t3 + t4 + t5;

        double snd = standardNormalDistribution(x); //for testing
        double cd = 1 - (snd * b);

        double resp = 0.0;
        if (x < 0) {
            resp = 1 - cd;
        } else {
            resp = cd;
        }

        return resp;
    }

    private static double standardNormalDistribution(double x) {
        double top = Math.exp(-0.5 * Math.pow(x, 2));
        double bottom = Math.sqrt(2 * Math.PI);
        double resp = top / bottom;

        return resp;
    }

    @Override
    public List<PositionInfo> getPortfolioView() {
        return portfolio.values().stream().toList();
    }

    @Override
    public void onPortfolioUpdated(Consumer<Collection<MarketDataService.PriceTick>> action) {
        listeners.add(action);
    }

    private static final double P = 0.2316419;
    private static final double B1 = 0.319381530;
    private static final double B2 = -0.356563782;
    private static final double B3 = 1.781477937;
    private static final double B4 = -1.821255978;
    private static final double B5 = 1.330274429;
}
