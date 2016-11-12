package org.tangelatos.feed.skroutzer;


import de.ailis.pherialize.MixedArray;
import de.ailis.pherialize.Pherialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.tangelatos.feed.skroutzer.generators.Generator;
import org.tangelatos.feed.skroutzer.vo.Product;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The simplistic Generator component - queries the database and produces the Products that need to be entered in the
 * XML - it also performs a distinct() on the uid to make sure that no duplicates are present.
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

    /**
     * calls the relevant SQL to receive Product and Category; tries to cache as much as possible.
     * @param sqlGenerator the generator selected
     * @return the list of products
     */
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
                    LOG.debug("HIT: Category {}",p.getCategoryId());
                } else {
                    List<Map<String, Object>> list = template.queryForList(categorySQL, p.getCategoryId());
                    final StringBuffer realCategory = new StringBuffer();
                    list.forEach(m -> {
                        realCategory.append(m.get("name")).append(" > ");
                    });
                    p.setCategory(realCategory.substring(0, realCategory.length()-3).trim());
                    categoriesPath.put(p.getCategoryId(), p.getCategory());
                    LOG.debug("MISS: Category {} from DB",p.getCategoryId());
                }
                p.setImage(resultSet.getString("image"));
                p.setInstock(resultSet.getString("instock"));
                p.setLink(resultSet.getString("link"));
                p.setManufacturer(resultSet.getString("manufacturer"));
                p.setMpn(resultSet.getString("mpn"));
                p.setPrice(resultSet.getString("price_with_vat"));
                Double weightInGrams = resultSet.getDouble("weight") * 1000;
                p.setWeight( weightInGrams.intValue()+"" );
                p.setAvailability(resultSet.getString("availability"));

                LOG.trace("Mapped product (name={}, id={})",p.getName(),p.getId());
                return p;
            }
        });
        List<Product> distinct = products.stream().distinct().collect(Collectors.toList());
        LOG.info("Mapped {} unique products, cached categories: {}",distinct.size(), categoriesPath.size());
        return distinct;
    }

    /**
     * creates a product feed XML - using the template provided and the products list - file is never appended.
     * @param products
     * @param templateName
     * @param excluded
     * @param f
     */
    public void createFeedXml(List<Product> products, String templateName, File f) {

        Context ctx = new Context(new Locale(locale));

        ctx.setVariable("products", products);
        ctx.setVariable("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        try ( Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f,false), "UTF-8")) ) {
            LOG.info("Writing XML in {} using template {} and {} products",f.getAbsolutePath(), templateName, products.size());
            String textContent = engine.process(templateName, ctx);
            writer.write(textContent.replace("\"1.0\"?>", "\"1.0\" encoding=\"UTF-8\"?>" ));
            LOG.info("Writing complete.");
        } catch (IOException e) {
            LOG.error("Exception writing the file: " + e.getMessage(),e);
        }
    }

    public Set<String> getExcluded(Generator templateGenerator) {

        String phpExcluded = template.queryForObject(templateGenerator.getExcludedProductIds(), String.class);

        MixedArray list = Pherialize.unserialize(phpExcluded).toArray();

        if (list == null || list.isEmpty()) {
            return null;
        } else {
            Set<String> exc = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                exc.add(list.getString(i));
            }
            LOG.info("Products excluded: {}",exc);
            return exc;
        }
    }
}
