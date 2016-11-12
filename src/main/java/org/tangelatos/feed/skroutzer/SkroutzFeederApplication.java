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
import org.tangelatos.feed.skroutzer.generators.Generator;
import org.tangelatos.feed.skroutzer.vo.Product;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableAutoConfiguration
public class SkroutzFeederApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(SkroutzFeederApplication.class, args);
	}

	private static final Logger LOG = LoggerFactory.getLogger("FeederApplication");

    @Autowired
	FeedGenerator gen;

	@Value("${feed.filename:./product_feed.xml}")
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
			return;
		}
		LOG.info("Generating feed using generator {} and target file {}",
				generator, fileName);
		Generator templateGenerator = context.getBean(generator, Generator.class);
		final List<Product> products = gen.generateProducts(templateGenerator);
		final Set<String> excluded = gen.getExcluded(templateGenerator);

		gen.createFeedXml(products.stream().filter( p -> !excluded.contains(p.getId())).collect(Collectors.toList()),
				templateGenerator.getTemplateName(), f);


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
