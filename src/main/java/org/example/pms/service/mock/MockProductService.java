package org.example.pms.service.mock;

import org.example.pms.service.ProductService;
import org.h2.jdbcx.JdbcDataSource;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.function.Function;

public class MockProductService implements ProductService {
    private final Connection connection;
    private final String EQ_TBL = "prd_equity";
    private final String OPTION_TBL = "prd_option";

    public MockProductService() {
        try {
            this.connection = initH2("schema.sql", "data.sql");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Product> lookup(String ticker) {
        try {
            Optional<Product> product = query(ticker, EQ_TBL, this::buildEquity);
            if (product.isPresent()) {
                return product;
            }

            return query(ticker, OPTION_TBL, this::buildOption);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return Optional.empty();
    }

    private Optional<Product> query(String ticker, String tableName, Function<ResultSet, Product> builder) throws Exception {
        Statement statement = connection.createStatement();
        String queryStock = buildQuerySql(tableName, ticker);
        ResultSet resultSet = statement.executeQuery(queryStock);

        Product product = null;
        if (resultSet.first()) {
            product = builder.apply(resultSet);
        }

        return Optional.ofNullable(product);
    }

    private Equity buildEquity(ResultSet rs) {
        try {
            return new Equity(rs.getString("symbol"));
        } catch (Exception ex) {
            return null;
        }
    }

    private Option buildOption(ResultSet rs) {
        try {
            Option result = new Option(rs.getString("symbol"));

            result.setPutCall(PutCall.valueOf(rs.getString("put_call").trim().toUpperCase()));
            result.setStrikePrice(rs.getDouble("strike_price"));
            result.setYear(rs.getInt("maturity_year"));
            result.setMonth(rs.getInt("maturity_month"));
            result.setUnderlyingSymbol(rs.getString("underlying_symbol"));
            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String buildQuerySql(String tbl, String symbol) {
        //TODO: escape sql
        return "select * from %s where symbol = '%s'".formatted(tbl, symbol);
    }

    private Connection initH2(String schemaFile, String dataFile) throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test_db");
        ds.setUser("sa");
        ds.setPassword("sa");

        Connection conn = ds.getConnection();

        Statement stmt = conn.createStatement();

        executeUpdate(schemaFile, stmt);
        executeUpdate(dataFile, stmt);
        return conn;
    }

    private void executeUpdate(String sqlFile, Statement stmt) throws Exception {
        String file = MockProductService.class.getClassLoader().getResource(sqlFile).getFile();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;

        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        stmt.executeUpdate(builder.toString());
    }
}
