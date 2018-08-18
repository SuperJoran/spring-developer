package be.superjoran.springdeveloper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.handler.annotation.Headers;

import java.io.File;

@SpringBootApplication
public class SpringDeveloperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDeveloperApplication.class, args);
    }

    @Bean
    IntegrationFlow files(
            @Value("${input-directory:${sys:user.home}/Desktop/in}") File inputDirectory
    ) {
        return IntegrationFlows.from(Files.inboundAdapter(inputDirectory).autoCreateDirectory(true).patternFilter("*.txt"),
                poller -> poller.poller(pf -> pf.fixedRate(1000)))
                .channel(this.pubSubChannel())
                .log(e -> String.format("File with filename %s is inbound", e.getHeaders().get(FileHeaders.FILENAME)))
                .get();
    }

    @Bean
    SubscribableChannel pubSubChannel() {
        return MessageChannels.publishSubscribe("incomingFilesChannel")
                .maxSubscribers(2).get();
    }

    @Bean
    IntegrationFlow outputFlow(
            @Value("${output-directory:${sys:user.home}/Desktop/out}") File outputDirectory,
            CountryService countryService
    ) {
        return IntegrationFlows.from(this.pubSubChannel())
                .transform(Files.toStringTransformer())
                .transform(String.class, countryService::getPopulation)
                .transform(Integer.class, Object::toString)
                .handle(Files.outboundAdapter(outputDirectory)
                        .autoCreateDirectory(true)
                        .fileNameGenerator(m -> (String) m.getHeaders().get(FileHeaders.FILENAME)))
                .get();
    }

    @Bean
    IntegrationFlow archiveFlow(
            @Value("${archive-directory:${sys:user.home}/Desktop/archive}") File archiveDirectory) {
        return IntegrationFlows.from(this.pubSubChannel())
                .handle(Files.outboundAdapter(archiveDirectory)
                        .autoCreateDirectory(true)
//                        .fileNameGenerator(m -> ((String) m.getHeaders().get(FileHeaders.FILENAME)).split("\\.")[0] + "-archived" + ".txt")
                        .fileNameGenerator(m -> (String) m.getHeaders().get(FileHeaders.FILENAME))
                )
                .get();
    }
}
