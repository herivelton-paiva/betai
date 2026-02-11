package br.com.betai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class SqsService {

    private static final Logger log = LoggerFactory.getLogger(SqsService.class);
    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/544913640075/match-analyser-betai";

    private final SqsClient sqsClient;

    public SqsService() {
        this.sqsClient = SqsClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(DefaultCredentialsProvider.create()).build();
    }

    public void sendToAnalysisQueue(String payloadJson) {
        try {
            SendMessageRequest sendMsgRequest = SendMessageRequest.builder().queueUrl(QUEUE_URL)
                    .messageBody(payloadJson).build();

            sqsClient.sendMessage(sendMsgRequest);
            log.info("Payload enviado para SQS com sucesso!");
        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para SQS: {}", e.getMessage(), e);
        }
    }
}
