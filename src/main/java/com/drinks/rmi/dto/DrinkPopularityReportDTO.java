package com.drinks.rmi.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Drink Popularity Report information
 * Must implement Serializable for RMI transfer
 */
public class DrinkPopularityReportDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DrinkPopularityItem> drinkPopularity;
    private List<DrinkPopularityItem> topDrinks;
    private BigDecimal totalRevenue;
    private BigDecimal reportRevenue;
    
    // Default constructor required for serialization
    public DrinkPopularityReportDTO() {
        this.drinkPopularity = new ArrayList<>();
        this.topDrinks = new ArrayList<>();
    }
    
    public DrinkPopularityReportDTO(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.drinkPopularity = new ArrayList<>();
        this.topDrinks = new ArrayList<>();
    }
    
    // Getters and setters
    public LocalDate getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    
    public LocalDate getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public List<DrinkPopularityItem> getDrinkPopularity() {
        return drinkPopularity;
    }
    
    public void setDrinkPopularity(List<DrinkPopularityItem> drinkPopularity) {
        this.drinkPopularity = drinkPopularity;
    }
    
    public List<DrinkPopularityItem> getTopDrinks() {
        return topDrinks;
    }
    
    public void setTopDrinks(List<DrinkPopularityItem> topDrinks) {
        this.topDrinks = topDrinks;
    }
    
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public BigDecimal getReportRevenue() {
        return reportRevenue;
    }
    
    public void setReportRevenue(BigDecimal reportRevenue) {
        this.reportRevenue = reportRevenue;
    }
    
    public double getRevenuePercentage() {
        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) return 0.0;
        return reportRevenue.divide(totalRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100;
    }
    
    /**
     * Inner class to represent drink popularity data
     */
    public static class DrinkPopularityItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private Long drinkId;
        private String drinkName;
        private int totalQuantitySold;
        private BigDecimal totalRevenue;
        private int orderCount;
        private int totalOrders;
        
        public DrinkPopularityItem() {
        }
        
        public DrinkPopularityItem(Long drinkId, String drinkName, int totalQuantitySold, 
                                 BigDecimal totalRevenue, int orderCount, int totalOrders) {
            this.drinkId = drinkId;
            this.drinkName = drinkName;
            this.totalQuantitySold = totalQuantitySold;
            this.totalRevenue = totalRevenue;
            this.orderCount = orderCount;
            this.totalOrders = totalOrders;
        }
        
        // Getters and setters
        public Long getDrinkId() {
            return drinkId;
        }
        
        public void setDrinkId(Long drinkId) {
            this.drinkId = drinkId;
        }
        
        public String getDrinkName() {
            return drinkName;
        }
        
        public void setDrinkName(String drinkName) {
            this.drinkName = drinkName;
        }
        
        public int getTotalQuantitySold() {
            return totalQuantitySold;
        }
        
        public void setTotalQuantitySold(int totalQuantitySold) {
            this.totalQuantitySold = totalQuantitySold;
        }
        
        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }
        
        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }
        
        public int getOrderCount() {
            return orderCount;
        }
        
        public void setOrderCount(int orderCount) {
            this.orderCount = orderCount;
        }
        
        public int getTotalOrders() {
            return totalOrders;
        }
        
        public void setTotalOrders(int totalOrders) {
            this.totalOrders = totalOrders;
        }
        
        @Override
        public String toString() {
            return "DrinkPopularityItem{" +
                    "drinkId=" + drinkId +
                    ", drinkName='" + drinkName + '\'' +
                    ", totalQuantitySold=" + totalQuantitySold +
                    ", totalRevenue=" + totalRevenue +
                    ", orderCount=" + orderCount +
                    ", totalOrders=" + totalOrders +
                    '}';
        }
    }
    
    @Override
    public String toString() {
        return "DrinkPopularityReportDTO{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", drinkPopularity=" + drinkPopularity +
                ", topDrinks=" + topDrinks +
                ", totalRevenue=" + totalRevenue +
                ", reportRevenue=" + reportRevenue +
                '}';
    }
}
