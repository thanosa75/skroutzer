package org.tangelatos.feed.skroutzer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by thanos on 11/12/16.
 */

@Component
public class FeedGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(FeedGenerator.class);
    @Value("${feed.locale:el_GR}")
    private String locale;


    @Autowired
    ApplicationContext context;

    @Autowired
    JdbcTemplate template;

    @Autowired
    TemplateEngine engine;

    public List<Product> generateProducts(Generator sqlGenerator) {
        String SQL = sqlGenerator.prepareSql();
        String categorySQL = sqlGenerator.prepareCategorySql();

        Map<String,String> categoriesPath = new HashMap<>();

        List<Product> products = template.query(SQL, new RowMapper<Product>() {

            @Override
            public Product mapRow(ResultSet resultSet, int i) throws SQLException {
                Product p = new Product();
                p.setId(resultSet.getString("product_id"));
                p.setName(resultSet.getString("name"));
                //p.setCategory(resultSet.getString("category"));
                p.setCategoryId(resultSet.getString("cat_id"));

                if (categoriesPath.containsKey(p.getCategoryId()) ) {
                    p.setCategory(categoriesPath.get(p.getCategoryId()));
                } else {
                    List<Map<String, Object>> list = template.queryForList(categorySQL, p.getCategoryId());
                    final StringBuffer realCategory = new StringBuffer();
                    list.forEach(m -> {
                        realCategory.append(m.get("name")).append(" > ");
                    });
                    p.setCategory(realCategory.substring(0, realCategory.length()-3));
                    categoriesPath.put(p.getCategoryId(), p.getCategory());
                }
                p.setImage(resultSet.getString("image"));
                p.setInstock(resultSet.getString("instock"));
                p.setLink(resultSet.getString("link"));
                p.setManufacturer(resultSet.getString("manufacturer"));
                p.setMpn(resultSet.getString("mpn"));
                p.setPrice(resultSet.getString("price_with_vat"));
                p.setWeight(resultSet.getString("weight"));
                p.setAvailability(resultSet.getString("availability"));

                LOG.trace("Mapped product (name={}, id={})",p.getName(),p.getId());
                return p;
            }
        });
        List<Product> distinct = products.stream().distinct().collect(Collectors.toList());
        LOG.info("Mapped {} products, cached categories: {}",distinct.size(), categoriesPath.size());
        return distinct;
    }

    public void createFeedXml(List<Product> products, String templateName, File f) {

        Context ctx = new Context(new Locale(locale));

        ctx.setVariable("products", products);
        ctx.setVariable("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        try ( Writer writer = new BufferedWriter(new FileWriter(f,false)) ) {
            LOG.info("Writing XML in {} using template {} and {} products",f.getAbsolutePath(), templateName, products.size());
            engine.process(templateName, ctx,writer);
            LOG.info("Writing complete.");
        } catch (IOException e) {
            LOG.error("Exception writing the file: " + e.getMessage(),e);
        }
    }
}
