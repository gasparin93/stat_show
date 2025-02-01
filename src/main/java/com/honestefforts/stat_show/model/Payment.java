package com.honestefforts.stat_show.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Data
@FieldDefaults(makeFinal = true)
public class Payment {
  String user;
  double paymentAmt;
}
