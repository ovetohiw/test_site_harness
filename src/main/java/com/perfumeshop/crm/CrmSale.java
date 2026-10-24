package com.perfumeshop.crm;

import java.math.BigDecimal;
import java.util.List;

public record CrmSale(
        Long orderId,
        String login,
        BigDecimal total,
        List<Line> lines
) {
    public record Line(String productName, int quantity, BigDecimal unitPrice) {
    }

    public String title() {
        return "Order #" + orderId + " — " + login;
    }

    public String description() {
        StringBuilder sb = new StringBuilder();
        for (Line line : lines) {
            sb.append(line.productName())
                    .append(" x")
                    .append(line.quantity())
                    .append(" @ ")
                    .append(line.unitPrice())
                    .append("; ");
        }
        return sb.toString().trim();
    }
}
