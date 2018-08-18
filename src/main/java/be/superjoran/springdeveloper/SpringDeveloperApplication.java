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
                .get();

    }

    @Bean
    SubscribableChannel pubSubChannel() {
        return MessageChannels.publishSubscribe("incomingFilesChannel")
                .maxSubscribers(2).get();
    }

    @Bean
    IntegrationFlow outputFlow(
            @Value("${input-directory:${sys:user.home}/Desktop/out}") File outputDirectory) {
        return IntegrationFlows.from(this.pubSubChannel())
                .transform(Files.toStringTransformer())
                .transform(String.class, s -> "Hello, world\n" + s)
                .handle(Files.outboundAdapter(outputDirectory)
                        .autoCreateDirectory(true)
                        .fileNameGenerator(m -> (String) m.getHeaders().get(FileHeaders.FILENAME)))
                .get();
    }

    @Bean
    IntegrationFlow archiveFlow(
            @Value("${input-directory:${sys:user.home}/Desktop/archive}") File archiveDirectory) {
        return IntegrationFlows.from(this.pubSubChannel())
                .handle(Files.outboundAdapter(archiveDirectory)
                        .autoCreateDirectory(true)
                        .deleteSourceFiles(true)
                        .fileNameGenerator(m -> ((String) m.getHeaders().get(FileHeaders.FILENAME)).split("\\.")[0] + "-archived" + ".txt"))
                .get();
    }
}
