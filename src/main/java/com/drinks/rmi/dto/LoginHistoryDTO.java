package com.drinks.rmi.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class LoginHistoryDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long userId;
    private LocalDateTime loginTime;
    private String ipAddress;
    
    public LoginHistoryDTO() {}
    
    public LoginHistoryDTO(Long id, Long userId, LocalDateTime loginTime, String ipAddress) {
        this.id = id;
        this.userId = userId;
        this.loginTime = loginTime;
        this.ipAddress = ipAddress;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    @Override
    public String toString() {
        return "LoginHistoryDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", loginTime=" + loginTime +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
