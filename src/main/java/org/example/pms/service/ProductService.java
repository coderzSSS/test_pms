package org.example.pms.service;

import lombok.Data;

import java.util.Optional;

public interface ProductService {
    Optional<Product> lookup(String ticker);

    enum ProductType {
        STOCK,
        OPTION
    }

    enum PutCall {
        PUT,
        CALL
    }

    interface Product {
        String getSymbol();

        ProductType getProductType();
    }

    @Data
    class Equity implements Product {
        private final String symbol;

        public Equity(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public ProductType getProductType() {
            return ProductType.STOCK;
        }
    }

    @Data
    class Option implements Product {
        private final String symbol;

        private PutCall putCall;

        private Double strikePrice;

        private int year;

        private int month;

        private String underlyingSymbol;

        public Option(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public ProductType getProductType() {
            return ProductType.OPTION;
        }
    }

}
