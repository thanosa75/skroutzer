# Skroutzer - A feed generator for e-shops

I have been using the [OpenCart](http://opencart.com) software for ages now - since 1.5.x. To generate revenue, we publish our products on [Skroutz.gr](http://skroutz.gr) and [BestBuy](http://bestbuy.gr). However, all the feed publishing modules (and I've tried more than one) have problems with updates on the feed xml (seems to be happening way too frequently) and have not yet published the _weight_ attribute which is very important so Skroutz _properly displays shipping costs_. 

The code here produces a proper _product feed xml_ that validates against the online Skroutz Feed validator. To make it work, you will need to compile it, take the executable Spring Boot app and configure a simplified _application.properties_ file; it should be placed together with the JAR itself.

## Installation
After you have cloned/extracted the code, you will need to compile it. The shell script in the root of the project (`mvnw`) will download the latest Maven tool automatically; see below for actions:
```sh
$ mvnw clean install
```
A few seconds later, in the `target` directory, a new _executable_ jar will be produced. It is a standalone (Java is required) executable and can be directly used to produce the feed. You may execute it with `./target/<nameofjar.jar>`. A sample application.properties file is provided below - you need to modify appropriately so the generator can find the database.
### Sample application.properties file
```properties
# specify the name for the DB used, username and pass
feed.dbname=db_name_here
feed.user=someuser
feed.pass=somepass
# name and path of the feed file to be created
feed.filename=product_feed.xml

# specify the prefix of tables (from opencart installation)
feed.opencart.table.base=oc_prefix
# specify the base of the www site being served (for links - images)
feed.site=http://angelatos.gr/
```

### Automatic feed updates
It is recommended that the feed is generated every 24 hours; to do so you may use `crontab` or a similar method to re-execute the feed generator. 

### Technical Support
Technical support is available, _create an issue in the github issues for this project_ and we'll contact you. Installation support and/or a support contract are also possible options, with a very small cost that includes upgrades to the generator (if and when they happen).
