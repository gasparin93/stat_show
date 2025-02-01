package com.honestefforts.stat_show.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.honestefforts.stat_show.model.PaymentStats;
import com.honestefforts.stat_show.persistence.PaymentStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentConsumer {
  @Autowired
  private PaymentStatsRepository repository;

  @Autowired
  private ObjectMapper objectMapper;

  @KafkaListener(topics = "payment-stats", groupId = "payment-stats-group")
  public void listen(String message) {
    try {
      PaymentStats stats = objectMapper.readValue(message, PaymentStats.class);
      repository.updateStats(stats);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
