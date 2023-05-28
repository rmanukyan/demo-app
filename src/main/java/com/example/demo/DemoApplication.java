package com.example.demo;

import com.jcraft.jsch.ChannelSftp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.integration.annotation.*;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.filters.*;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.support.PeriodicTrigger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@SpringBootApplication
public class DemoApplication {

    public static final String LOCAL_DIR = "sftp-inbound";
    @Autowired
    InputFileRepo repo;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public SessionFactory<ChannelSftp.LsEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost("localhost");
        factory.setPort(2222);
        factory.setUser("foo");
        factory.setPassword("pass");
        factory.setAllowUnknownKeys(true);
        return new CachingSessionFactory<>(factory);
    }

    private String getDir() {
        String pattern = "yyMMdd";
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date());
    }

//    @Bean
//    public DataSource dataSource() {
//        DataSourceBuilder<?> dataSourceBuilder = DataSourceBuilder.create();
//        dataSourceBuilder.driverClassName("org.h2.Driver");
//        dataSourceBuilder.url("jdbc:h2:file:./mydb");
//        dataSourceBuilder.username("SA");
//        dataSourceBuilder.password("");
//        return dataSourceBuilder.build();
//    }
//
//    @Bean
//    public JdbcMetadataStore metadataStore(DataSource dataSource) {
//        JdbcMetadataStore jdbcMetadataStore = new JdbcMetadataStore(dataSource);
//        return jdbcMetadataStore;
//    }


    @Bean
    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
        SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
        fileSynchronizer.setDeleteRemoteFiles(false);
        fileSynchronizer.setRemoteDirectory("/upload/" + getDir());
        fileSynchronizer.setFilter(new ChainFileListFilter<>(
                        List.of(new SftpRegexPatternFileListFilter("^(NCOLC|NSFTRTXCC)[a-zA-Z0-9_-]+\\.csv$")
                                ,
                                (FileListFilter<ChannelSftp.LsEntry>) files -> {
                                    System.out.println("In the custom filter...");
                                    List<ChannelSftp.LsEntry> list = Arrays
                                            .stream(files).
                                            filter(f -> !repo.existsByNameAndProcessed(LOCAL_DIR + "/" + f.getFilename(), true))
                                            .toList();
                                    System.out.println("New files: " + list);
                                    return list;
                                })
                )
        );
        return fileSynchronizer;
    }

    @Bean
    @InboundChannelAdapter(channel = "sftpChannel", poller = @Poller(fixedDelay = "5000", maxMessagesPerPoll = "-1"))
    public MessageSource<File> sftpMessageSource() {
        SftpInboundFileSynchronizingMessageSource source =
                new SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer());
        source.setLocalDirectory(new File(LOCAL_DIR));
        source.setLoggingEnabled(true);
        source.setAutoCreateLocalDirectory(true);
//        source.setLocalFilter(new FileSystemPersistentAcceptOnceFileListFilter(metadataStore(dataSource()), "MYPREFIX_"));
        source.setLocalFilter(new AcceptAllFileListFilter<>());
        source.setMaxFetchSize(-1);
        return source;
    }

//    @MessageEndpoint
//    public class ErrorUnwrapper {
//
//        @Transformer
//        public Message<?> transform(ErrorMessage errorMessage) {
//            return ((MessagingException) errorMessage.getPayload()).getFailedMessage();
//        }
//    }

//    @Autowired
//    @Qualifier("errorChannel")
//    @Order(99)
//    private PublishSubscribeChannel errorChannel;
//
//    @Bean
//    @Order(9999)
//    public IntegrationFlow errorHandlingFlow() {
//        return IntegrationFlows.from(errorChannel)
//                .handle(message -> System.out.println("@@@@@@@@@@@@@@@@@@@@@" + message.getPayload()), e -> e.order(500))
//                .get();
//    }


    @Bean(PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata defaultPoller() {
        PollerMetadata pollerMetadata = new PollerMetadata();
        pollerMetadata.setMaxMessagesPerPoll(-1);
        pollerMetadata.setTrigger(new PeriodicTrigger(5000));
        return pollerMetadata;
    }

    @Bean
    public QueueChannel errorChannel() {
        return new QueueChannel(500);
    }
    // No 'poller' attribute because there is a default global poller
    @Transformer(inputChannel = "errorChannel")
    public Object transform(Object payload) {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! " + payload);
        return payload;
    }






    @Bean
    @ServiceActivator(inputChannel = "sftpChannel")
    public MessageHandler handler() {

        return message -> {
            try {
                System.out.println("in the handler");
                String name = message.getPayload().toString();

                Optional<InputFile> file = repo.findByName(name);
                if (file.isEmpty()) {
                    System.out.println("Starting download of " + name);
                    repo.save(new InputFile(name, true));
                } else if (!file.get().getProcessed()) {
                    System.out.println("Reprocessing file:  " + name);
                    InputFile inputFile = file.get();
                    inputFile.setProcessed(true);
                    repo.save(inputFile);
                } else {
                    System.out.println("Ignoring already processed file " + name);
                }
                File localFile = new File(name);
                localFile.delete();
            } catch (MessagingException e) {
                System.out.println("Exception happened..");
                System.out.println(e.getMessage());
            }
        };
    }

}
