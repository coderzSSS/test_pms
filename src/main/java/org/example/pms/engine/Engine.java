package org.example.pms.engine;

import org.example.pms.service.MarketDataService;
import org.example.pms.service.PortfolioService;
import org.example.pms.service.ProductService;
import org.example.pms.service.business.DefaultPortfolioService;
import org.example.pms.service.mock.MockMarketDataService;
import org.example.pms.service.mock.MockProductService;
import org.example.pms.util.PortfolioPrinter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class Engine {
    MarketDataService marketDataService = initMarketDataService();
    ProductService productService = null;
    PortfolioService portfolioService = null;

    public void start() throws Exception {
        productService = initProductService();

        portfolioService = initPortfolioService(productService, marketDataService);

        marketDataService.start();
    }

    public void stop() {
        marketDataService.stop();
    }

    private PortfolioService initPortfolioService(ProductService productService, MarketDataService marketDataService) throws Exception {
        DefaultPortfolioService result = new DefaultPortfolioService(productService);
        marketDataService.subscribe(List.of("AAPL", "TELSA"), result::onPriceTick);
        PortfolioPrinter printer = new PortfolioPrinter(System.out);
        result.onPortfolioUpdated(priceTicks -> {
            printer.print(priceTicks, result.getPortfolioView());
        });

        BufferedReader reader = new BufferedReader(new InputStreamReader(Engine.class.getClassLoader().getResourceAsStream("pos.csv")));

        String line;
        reader.readLine(); //ignore first line
        while((line = reader.readLine()) != null) {
            String[] strs = line.split(",");
            String symbol = strs[0];
            String pos = strs[1];

            result.addPortfolio(symbol, Long.parseLong(pos));
        }

        return result;
    }

    private MarketDataService initMarketDataService() {
        return new MockMarketDataService();
    }

    private ProductService initProductService() throws Exception {
        return new MockProductService();
    }
}
