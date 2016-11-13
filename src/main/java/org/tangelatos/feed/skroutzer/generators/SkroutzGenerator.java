package org.tangelatos.feed.skroutzer.generators;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Created by thanos on 11/12/16.
 */
@Component("Skroutz")
public class SkroutzGenerator implements Generator {
    
    private String productSql = "select prd.product_id, " +
            "  prdd.name, " +
            "  concat('$OC_SITE$image/cache/',prd.image) as image,  " + //needs host
            "  concat('$OC_SITE$',alias.keyword) as link,  " + //-- needs host
            "  cat.category_id as cat_id, " +
            "  cat.name category, " +
            "  prd.price, " +
            "  round(prd.price*((100+tra.rate)/100),2) as price_with_vat,  " +
            "  manf.name as manufacturer, " +
            "  prd.mpn, " +
            "  if (prd.quantity>prd.minimum, '$OC_Y$', '$OC_N$') as instock, " +
            "  stockstat.name availability, " +
            "  prd.weight " +
            "from  " +
            "  $OC_BASE$product prd, " +
            "  $OC_BASE$product_description prdd, " +
            "  $OC_BASE$language lang, " +
            "  $OC_BASE$stock_status stockstat, " +
            "  $OC_BASE$tax_class tc, " +
            "  $OC_BASE$tax_rate tra, " +
            "  $OC_BASE$tax_rule tru, " +
            "  $OC_BASE$manufacturer manf, " +
            "  $OC_BASE$url_alias alias, " +
            "  $OC_BASE$category_description cat, " +
            "  $OC_BASE$product_to_category ptc " +
            "where prd.product_id = prdd.product_id " +
            "  and prdd.language_id = lang.language_id " +
            "  and stockstat.language_id=lang.language_id " +
            "  and stockstat.stock_status_id = prd.stock_status_id " +
            "  and tc.tax_class_id=tru.tax_class_id " +
            "  and tru.tax_rate_id=tra.tax_rate_id " +
            "  and prd.tax_class_id=tc.tax_class_id " +
            "  and tru.priority=1 " +
            "  and cat.category_id=ptc.category_id " +
            "  and cat.language_id = lang.language_id " +
            "  and ptc.product_id=prd.product_id " +
            "  and lang.code = '$OC_LANGCODE$' " +
            "  and manf.manufacturer_id=prd.manufacturer_id " +
            "  and alias.query=concat('product_id=',prd.product_id) " +
            "  and prd.status=1 " +
            "order by prd.product_id;";

    String categorySql = "SELECT cp.category_id, cd.name, level " +
            "FROM $OC_BASE$category_path cp, $OC_BASE$category_description cd, $OC_BASE$language lang " +
            "WHERE cp.category_id = ? " +
            "and cp.path_id=cd.category_id " +
            "and cd.language_id=lang.language_id " +
            "and lang.code= '$OC_LANGCODE$' ; ";

    String discountSql = "select prd.product_id, " +
            //"       d.price, " +
            "       round(d.price*((100+tra.rate)/100),2) as price_with_vat, " +
            "       d.date_start, " +
            "       d.date_end " +
            "from $OC_BASE$product_special d, " +
            "  $OC_BASE$tax_class tc, " +
            "  $OC_BASE$tax_rate tra, " +
            "  $OC_BASE$tax_rule tru, " +
            "  $OC_BASE$product prd " +
            "where  tc.tax_class_id=tru.tax_class_id " +
            "  and tru.tax_rate_id=tra.tax_rate_id " +
            "  and prd.tax_class_id=tc.tax_class_id " +
            "  and tru.priority=1 " +
            "  and d.product_id = prd.product_id " +
            "  and (( current_timestamp between d.date_start and d.date_end) or " +
            "       ( current_timestamp > d.date_start and d.date_end is null) ) " +
            "order by 1 ";



    String excludedSql = "SELECT value from $OC_BASE$setting where `group` = 'skroutz' and `key` = 'exclude_feed_product' ; ";

    @Value("${feed.opencart.table.base}")
    String tableBase; //OC_BASE

    @Value("${feed.site}")
    String sitePrefix; //OC_SITE

    @Value("${feed.l10n.yes:Y}")
    String ocYes; // $OC_Y$


    @Value("${feed.l10n.no:N}")
    String ocNo; // $OC_N$

    @Value("${feed.l10n.langcode:gr}")
    String langCode; //$OC_LANGCODE$


    public SkroutzGenerator(){}

    @Override
    public String prepareSql() {

        String prepared = productSql.replaceAll( Pattern.quote("$OC_BASE$"), tableBase);
        prepared = prepared.replaceAll(Pattern.quote("$OC_SITE$"), sitePrefix);
        prepared = prepared.replaceAll(Pattern.quote("$OC_Y$"), ocYes);
        prepared = prepared.replaceAll(Pattern.quote("$OC_N$"), ocNo);
        prepared = prepared.replaceAll(Pattern.quote("$OC_LANGCODE$"), langCode);

        return prepared;
    }

    @Override
    public String prepareCategorySql() {

        return categorySql.replaceAll(Pattern.quote("$OC_LANGCODE$"), langCode)
                .replaceAll(Pattern.quote("$OC_BASE$"), tableBase);
    }

    @Override
    public String getExcludedProductIds() {
        return excludedSql.replaceAll(Pattern.quote("$OC_BASE$"), tableBase);
    }

    @Override
    public String getDiscounts() {
        return discountSql.replaceAll(Pattern.quote("$OC_BASE$"), tableBase);
    }


    @Override
    public String getTemplateName() {
        return "skroutzfeed";
    }



}
