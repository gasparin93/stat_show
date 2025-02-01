package com.honestefforts.stat_show.persistence;

import com.honestefforts.stat_show.model.PaymentStats;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * in-memory db
 */
@Repository
public class PaymentStatsRepository {
  private final Map<String, PaymentStats> stats = new ConcurrentHashMap<>();

  public void updateStats(PaymentStats newStats) {
    stats.put(newStats.getUser(), newStats);
  }

  public Collection<PaymentStats> findAll() {
    return stats.values();
  }

}
