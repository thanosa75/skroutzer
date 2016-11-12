package org.tangelatos.feed.skroutzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.File;
import java.util.Collections;
import java.util.List;

@SpringBootApplication
@EnableAutoConfiguration
public class SkroutzfeederApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SkroutzfeederApplication.class, args);
	}

	private static final Logger LOG = LoggerFactory.getLogger("FeederApplication");
	@Autowired
	FeedGenerator gen;

	@Value("${feed.filename:product_feed.xml}")
	String fileName;


	@Value("${feed.generator:Skroutz}")
	private String generator;


	@Autowired
	ApplicationContext context;

	@Override
	public void run(String... strings) throws Exception {

		File f = new File(fileName);
		if (f.exists() && !f.delete()) {
			LOG.error("Cannot delete file {} - please check permissions!",f.getAbsolutePath());
		}
		Generator templateGenerator = context.getBean(generator, Generator.class);
		List<Product> products = gen.generateProducts(templateGenerator);
		gen.createFeedXml(products, templateGenerator.getTemplateName(), f);


	}


	@Bean
	public ITemplateResolver textTemplateResolver() {
		final ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
		templateResolver.setOrder(Integer.valueOf(1));
		templateResolver.setResolvablePatterns(Collections.singleton("text/xml"));
		templateResolver.setPrefix("/templates/");
		templateResolver.setSuffix(".xml");
		templateResolver.setTemplateMode("VALIDXML");
		templateResolver.setCharacterEncoding("UTF-8");
		templateResolver.setCacheable(false);
		return templateResolver;
	}

}
