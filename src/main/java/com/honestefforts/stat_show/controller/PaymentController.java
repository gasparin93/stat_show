package com.honestefforts.stat_show.controller;

import com.honestefforts.stat_show.model.Payment;
import com.honestefforts.stat_show.model.PaymentStats;
import com.honestefforts.stat_show.persistence.PaymentStatsRepository;
import com.honestefforts.stat_show.service.PaymentProducer;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

  @Autowired
  private PaymentProducer paymentPusher;

  @Autowired
  private PaymentStatsRepository repository;

  @PostMapping("/payment")
  @ResponseStatus(HttpStatus.OK)
  public void receivePayment(@RequestBody List<Payment> payments) {
    payments.forEach(p -> paymentPusher.sendPayment(p));
  }

  @GetMapping("/stats")
  public List<PaymentStats> getStats() {
    return repository.findAll().stream()
        .sorted(Comparator.comparingDouble(PaymentStats::getTotalAmount).reversed())
        .toList();
  }
}