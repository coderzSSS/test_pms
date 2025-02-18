package org.example.pms.service.mock;

import org.example.pms.service.ProductService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class MockProductServiceTest {
    private static final MockProductService target = new MockProductService();

    @Test
    void test_get_by_valid_ticker_returns_success() {
        // arrange
        String ticker = "AAPL";
        ProductService.Equity expected = new ProductService.Equity(ticker);

        // action
        Optional<ProductService.Product> result = target.lookup(ticker);

        // assertion
        Assertions.assertEquals(expected, result.orElseThrow());
    }

    @Test
    void test_get_by_invalid_ticker_returns_empty() {
        // arrange
        String ticker = "not exits";

        // action
        Optional<ProductService.Product> result = target.lookup(ticker);

        // assertion
        Assertions.assertTrue(result.isEmpty());
    }
}