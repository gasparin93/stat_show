package com.honestefforts.stat_show.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honestefforts.stat_show.model.Payment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentProducer {
  private static final String TOPIC = "payments";

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  public void sendPayment(Payment payment) {
    try {
      System.out.println("writing message: "+payment);
      String json = objectMapper.writeValueAsString(payment);
      kafkaTemplate.send(TOPIC, json);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
