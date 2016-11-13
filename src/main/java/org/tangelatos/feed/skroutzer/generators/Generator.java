package org.tangelatos.feed.skroutzer.generators;

/**
 * Created by thanos on 11/12/16.
 */
public interface Generator {
    String prepareSql();

    String getTemplateName();

    String prepareCategorySql();

    String getExcludedProductIds();

    String getDiscounts();
}
