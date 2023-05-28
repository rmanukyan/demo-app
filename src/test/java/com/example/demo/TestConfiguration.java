//package com.example.demo;
//
//import org.apache.sshd.sftp.client.SftpClient;
//import org.springframework.context.annotation.Bean;
//import org.springframework.integration.annotation.InboundChannelAdapter;
//import org.springframework.integration.annotation.Poller;
//import org.springframework.integration.core.MessageSource;
//import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
//import org.springframework.integration.file.remote.session.CachingSessionFactory;
//import org.springframework.integration.file.remote.session.SessionFactory;
//import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
//import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
//import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
//import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
//
//import java.io.File;
//
//@org.springframework.boot.test.context.TestConfiguration
//public class TestConfiguration {
//
//    @Bean
//    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
//        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
//        factory.setHost("localhost");
//        factory.setPort(2222);
//        factory.setUser("foo");
//        factory.setPassword("pass");
//        factory.setAllowUnknownKeys(true);
//        return new CachingSessionFactory<>(factory);
//    }
//
//    @Bean
//    public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
//        SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
//        fileSynchronizer.setDeleteRemoteFiles(false);
//        fileSynchronizer.setRemoteDirectory("si.sftp.sample");
//        fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter("*.txt"));
//        return fileSynchronizer;
//    }
//
//    @Bean
//    @InboundChannelAdapter(channel = "receiveChannel", poller = @Poller(fixedDelay = "1000"))
//    public MessageSource<File> sftpMessageSource() {
//        SftpInboundFileSynchronizingMessageSource source =
//                new SftpInboundFileSynchronizingMessageSource(sftpInboundFileSynchronizer());
//        source.setLocalDirectory(new File("local-dir"));
//        source.setAutoCreateLocalDirectory(true);
//        source.setLocalFilter(new AcceptOnceFileListFilter<File>());
//        source.setMaxFetchSize(1);
//        return source;
//    }
//
//
//}
